package com.smartlearn.platform.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartlearn.platform.annotation.LogOperation;
import com.smartlearn.platform.entity.OperationLog;
import com.smartlearn.platform.mapper.OperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * AOP aspect that captures @OperationLog-annotated method calls
 * and persists them to the sys_operation_log table.
 */
@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    private final OperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    public OperationLogAspect(OperationLogMapper operationLogMapper, ObjectMapper objectMapper) {
        this.operationLogMapper = operationLogMapper;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint point, LogOperation opLog) throws Throwable {
        long start = System.currentTimeMillis();

        OperationLog entity = new OperationLog();
        entity.setModule(opLog.module());
        entity.setOperation(opLog.operation());
        entity.setMethod(point.getSignature().getDeclaringTypeName() + "." + point.getSignature().getName());
        entity.setTraceId(MDC.get("traceId"));

        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                entity.setRequestMethod(request.getMethod());
                entity.setRequestUrl(request.getRequestURI());
                entity.setIpAddress(getClientIp(request));
                entity.setUserAgent(request.getHeader("User-Agent"));

                try {
                    Object[] args = point.getArgs();
                    if (args.length > 0) {
                        entity.setRequestParams(objectMapper.writeValueAsString(args));
                    }
                } catch (Exception e) {
                    entity.setRequestParams("[serialization failed]");
                }
            }

            Object result = point.proceed();
            entity.setResponseStatus(200);
            return result;
        } catch (Exception e) {
            entity.setResponseStatus(500);
            entity.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            entity.setDurationMs((int) (System.currentTimeMillis() - start));
            try {
                operationLogMapper.insert(entity);
            } catch (Exception e) {
                log.warn("[AuditLog] Failed to persist operation log: {}", e.getMessage());
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
