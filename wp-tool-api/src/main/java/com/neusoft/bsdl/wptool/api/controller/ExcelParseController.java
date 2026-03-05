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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@RestController
@RequestMapping("/api/v1")
public class ExcelParseController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @PostMapping("/excel/parse-screen")
    public ResponseEntity<ApiResponse<Object>> parseScreenExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file is empty");
        }

        FileSource fileSource = file::getInputStream;
        try {
            Object parseExcelContent = ParseExcelUtils.parseScreenExcel(fileSource);
            Object convertedContent = serializeSheetContentAsJsonString(parseExcelContent);
            return ResponseEntity.ok(ApiResponse.success(convertedContent));
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to parse excel file", exception);
        }
    }

    private Object serializeSheetContentAsJsonString(Object parseExcelContent) {
        JsonNode rootNode = OBJECT_MAPPER.valueToTree(parseExcelContent);
        if (!(rootNode instanceof ObjectNode rootObject)) {
            return parseExcelContent;
        }
        JsonNode sheetListNode = rootObject.get("sheetList");
        if (sheetListNode instanceof ArrayNode arrayNode) {
            for (JsonNode sheetNode : arrayNode) {
                if (sheetNode instanceof ObjectNode sheetObject && sheetObject.has("content")) {
                    JsonNode contentNode = sheetObject.get("content");
                    sheetObject.put("content", contentNode == null ? "null" : contentNode.toString());
                }
            }
        }
        return rootObject;
    }
}