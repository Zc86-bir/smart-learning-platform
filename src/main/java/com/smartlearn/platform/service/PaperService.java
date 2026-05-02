package com.smartlearn.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlearn.platform.dto.PaperDTO;
import com.smartlearn.platform.dto.QuestionWithScoreDTO;
import com.smartlearn.platform.entity.Paper;
import com.smartlearn.platform.request.SmartPaperRequest;

import java.util.List;

public interface PaperService {
    Page<Paper> listPapers(int page, int size);
    Paper getPaperById(Long id);
    PaperDTO getPaperDTO(Long id);
    PaperDTO createPaper(Paper paper);
    List<QuestionWithScoreDTO> smartGenerate(SmartPaperRequest request);
}
