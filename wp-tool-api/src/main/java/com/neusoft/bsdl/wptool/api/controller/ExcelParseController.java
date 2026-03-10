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
import com.neusoft.bsdl.wptool.core.exception.WPParseExcelException;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.ScreenExcelContent;
import com.neusoft.bsdl.wptool.core.service.ParseExcelUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Excel Parse", description = "Excelファイル解析API")
public class ExcelParseController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @PostMapping("/excel/parse-screen")
    @Operation(
            summary = "画面設計書Excel解析",
            description = "アップロードされた画面設計書Excelを解析し、sheetList内のcontentをJSON文字列として返します。"
        )
    public ResponseEntity<ApiResponse<ScreenExcelContent>> parseScreenExcel(
            @Parameter(
                description = "解析対象のExcelファイル",
                required = true,
                content = @Content(schema = @Schema(type = "string", format = "binary", example = "screen-design.xlsx"))
            )
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file is empty");
        }

        FileSource fileSource = file::getInputStream;
        try {
            ScreenExcelContent parseExcelContent = ParseExcelUtils.parseScreenExcel(fileSource);
            ScreenExcelContent convertedContent = serializeSheetContentAsJsonString(parseExcelContent);
            return ResponseEntity.ok(ApiResponse.success(convertedContent));
        } catch (WPParseExcelException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to parse excel file", exception);
        }
    }

    private ScreenExcelContent serializeSheetContentAsJsonString(ScreenExcelContent parseExcelContent) {
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
        return OBJECT_MAPPER.convertValue(rootObject, ScreenExcelContent.class);
    }
}