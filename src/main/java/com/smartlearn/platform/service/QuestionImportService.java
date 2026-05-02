package com.smartlearn.platform.service;

import com.smartlearn.platform.dto.QuestionDTO;

import java.io.InputStream;
import java.util.List;

public interface QuestionImportService {

    /**
     * Parse questions from an uploaded file.
     * Supported formats: .xlsx, .docx, .pdf, .txt
     *
     * @param stream    file content
     * @param extension file extension (without dot)
     * @return parsed question DTOs for preview before saving
     */
    List<QuestionDTO> parseImport(InputStream stream, String extension);

    /**
     * Save a list of parsed questions to the database.
     *
     * @param questions validated question DTOs
     * @return count of successfully imported questions
     */
    int saveImported(List<QuestionDTO> questions);
}
