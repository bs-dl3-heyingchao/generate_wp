package com.neusoft.bsdl.wptool.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.neusoft.bsdl.wptool.api.dto.ApiResponse;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.service.ParseExcelUtils;

@RestController
@RequestMapping("/api/v1")
public class ExcelParseController {

    @PostMapping("/excel/parse-screen")
    public ResponseEntity<ApiResponse<Object>> parseScreenExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file is empty");
        }

        FileSource fileSource = file::getInputStream;
        try {
            Object parseExcelContent = ParseExcelUtils.parseScreenExcel(fileSource);
            return ResponseEntity.ok(ApiResponse.success(parseExcelContent));
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to parse excel file", exception);
        }
    }
}