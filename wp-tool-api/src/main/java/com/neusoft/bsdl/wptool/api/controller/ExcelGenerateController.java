package com.neusoft.bsdl.wptool.api.controller;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.neusoft.bsdl.wptool.api.dto.ApiResponse;
import com.neusoft.bsdl.wptool.api.dto.GeneratedCodeZipResponse;
import com.neusoft.bsdl.wptool.core.exception.WPParseExcelException;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.DBQueryExcelContent;
import com.neusoft.bsdl.wptool.core.model.DBQuerySheetContent;
import com.neusoft.bsdl.wptool.core.model.ScreenExcelContent;
import com.neusoft.bsdl.wptool.core.service.ParseExcelUtils;
import com.neusoft.bsdl.wptool.generate.WPIOGenerator;
import com.neusoft.bsdl.wptool.generate.context.WPGenerateContext;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Excel Generate", description = "Excelコード生成API")
public class ExcelGenerateController {

    private final WPGenerateContext generateContext;

    public ExcelGenerateController(WPGenerateContext generateContext) {
        this.generateContext = generateContext;
    }

    @Value("${wp-tool.generate.output-root-dir:${user.dir}/temp}")
    private String generateOutputRootDir;

    @PostMapping("/excel/generate-io-code")
    @Operation(summary = "画面設計書ExcelからIOコード生成", description = "アップロードされた画面設計書Excelを解析し、生成したコードをZIP化してBase64文字列で返します。")
    public ResponseEntity<ApiResponse<GeneratedCodeZipResponse>> generateIoCode(
            @Parameter(description = "解析対象のExcelファイル(複数可)", required = true, content = @Content(schema = @Schema(type = "array"))) @RequestParam("ioFiles") MultipartFile[] ioFiles,
            @Parameter(description = "関連するDBQuery設計書(複数可)", required = false, content = @Content(schema = @Schema(type = "array"))) @RequestParam("dbQueryFiles") MultipartFile[] dbQueryFiles) {
        if (ioFiles == null || ioFiles.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded files are empty");
        }

        try {
            GeneratedCodeZipResponse responseData = generateZipBase64Response(ioFiles, dbQueryFiles);
            return ResponseEntity.ok(ApiResponse.success(responseData));
        } catch (WPParseExcelException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to generate code from excel files", exception);
        }
    }

    private GeneratedCodeZipResponse generateZipBase64Response(MultipartFile[] ioFiles, MultipartFile[] dbQueryFiles) throws Exception {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        Path taskOutputDir = Paths.get(generateOutputRootDir, taskId);
        Files.createDirectories(taskOutputDir);

        List<String> errorLog = new ArrayList<>();
        List<String> warnLog = new ArrayList<>();
        List<ScreenExcelContent> parsedContents = new ArrayList<>();
        for (MultipartFile file : ioFiles) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            FileSource fileSource = file::getInputStream;
            ScreenExcelContent screenExcelContent = ParseExcelUtils.parseScreenExcel(fileSource);
            parsedContents.add(screenExcelContent);
        }

        List<DBQuerySheetContent> parsedDBQueryContents = new ArrayList<>();
        if (dbQueryFiles != null) {
            for (MultipartFile file : dbQueryFiles) {
                if (file == null || file.isEmpty()) {
                    continue;
                }

                FileSource fileSource = file::getInputStream;
                DBQueryExcelContent queryExcelContent = ParseExcelUtils.parseDBQueryExcel(fileSource);
                parsedDBQueryContents.addAll(queryExcelContent.getQuerySheetContents());
            }
        }
        WPIOGenerator ioGenerator = new WPIOGenerator(generateContext, parsedContents, parsedDBQueryContents);
        ioGenerator.generate(taskOutputDir.toFile());
        errorLog.addAll(ioGenerator.getLogSnapshotError());
        warnLog.addAll(ioGenerator.getLogSnapshotWarn());

        byte[] zipBytes = zipDirectory(taskOutputDir);
        String zipBase64 = Base64.getEncoder().encodeToString(zipBytes);
        return new GeneratedCodeZipResponse(zipBase64, taskId, errorLog, warnLog);
    }

    private byte[] zipDirectory(Path sourceDir) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream); var pathStream = Files.walk(sourceDir)) {
            List<Path> filePaths = pathStream.filter(Files::isRegularFile).toList();
            for (Path filePath : filePaths) {
                String entryName = sourceDir.relativize(filePath).toString().replace("\\", "/");
                zipOutputStream.putNextEntry(new ZipEntry(entryName));
                zipOutputStream.write(Files.readAllBytes(filePath));
                zipOutputStream.closeEntry();
            }
        }
        return byteArrayOutputStream.toByteArray();
    }
}