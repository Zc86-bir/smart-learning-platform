package com.smartlearn.platform.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlearn.platform.dto.ApiResponse;
import com.smartlearn.platform.entity.Banner;
import com.smartlearn.platform.service.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/banners")
@Tag(name = "轮播图管理(管理员)", description = "轮播图CRUD")
@com.smartlearn.platform.interceptor.RequireRole("ADMIN")
public class BannerController {

    private static final Path UPLOAD_DIR = Paths.get("uploads/banners");

    private final BannerService bannerService;

    public BannerController(BannerService bannerService) {
        this.bannerService = bannerService;
        try { Files.createDirectories(UPLOAD_DIR); } catch (IOException ignored) {}
    }

    @PostMapping
    @Operation(summary = "创建轮播图")
    public ApiResponse<Banner> create(@RequestParam("title") String title,
                                      @RequestParam("file") MultipartFile file,
                                      @RequestParam(required = false) String linkUrl,
                                      @RequestParam(defaultValue = "0") int sortOrder) throws IOException {
        var ext = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.'));
        var filename = UUID.randomUUID() + ext;
        file.transferTo(UPLOAD_DIR.resolve(filename));

        var banner = new Banner();
        banner.setTitle(title);
        banner.setImageUrl("/uploads/banners/" + filename);
        banner.setLinkUrl(linkUrl);
        banner.setSortOrder(sortOrder);

        return ApiResponse.ok(bannerService.createBanner(banner));
    }

    @PutMapping
    @Operation(summary = "更新轮播图")
    public ApiResponse<Banner> update(@RequestBody Banner banner) {
        return ApiResponse.ok(bannerService.updateBanner(banner));
    }

    @GetMapping
    @Operation(summary = "轮播图列表")
    public ApiResponse<Page<Banner>> list(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(bannerService.listBanners(page, size));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除轮播图")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        bannerService.deleteBanner(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/toggle")
    @Operation(summary = "切换启用状态")
    public ApiResponse<Void> toggle(@PathVariable Long id) {
        bannerService.toggleActive(id);
        return ApiResponse.ok();
    }
}
