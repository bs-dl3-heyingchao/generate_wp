package com.neusoft.bsdl.wptool.api.controller;

import com.neusoft.bsdl.wptool.api.dto.ApiResponse;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.ParseExcelContent;
import com.neusoft.bsdl.wptool.core.service.ParseExcelUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class ExcelParseController {

    @PostMapping("/excel/parse")
    public ResponseEntity<ApiResponse<ParseExcelContent>> parseExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file is empty");
        }

        FileSource fileSource = file::getInputStream;
        try {
            ParseExcelContent parseExcelContent = ParseExcelUtils.parseExcel(fileSource);
            return ResponseEntity.ok(ApiResponse.success(parseExcelContent));
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to parse excel file", exception);
        }
    }
}