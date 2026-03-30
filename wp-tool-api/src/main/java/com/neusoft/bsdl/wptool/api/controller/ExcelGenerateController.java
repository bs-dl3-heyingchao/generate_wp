package com.neusoft.bsdl.wptool.api.controller;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.neusoft.bsdl.wptool.core.exception.WPCheckException;
import com.neusoft.bsdl.wptool.core.exception.WPException;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.CsvLayout;
import com.neusoft.bsdl.wptool.core.model.DBConfigDefinition;
import com.neusoft.bsdl.wptool.core.model.DBQueryExcelContent;
import com.neusoft.bsdl.wptool.core.model.DBQuerySheetContent;
import com.neusoft.bsdl.wptool.core.model.ExcelSheetContent;
import com.neusoft.bsdl.wptool.core.model.ScreenExcelContent;
import com.neusoft.bsdl.wptool.core.service.ParseExcelUtils;
import com.neusoft.bsdl.wptool.generate.WPDBConfigGenerator;
import com.neusoft.bsdl.wptool.generate.WPDBQueryGenerator;
import com.neusoft.bsdl.wptool.generate.WPIOExportGenerator;
import com.neusoft.bsdl.wptool.generate.WPIOGenerator;
import com.neusoft.bsdl.wptool.generate.context.WPGenerateContext;
import com.neusoft.bsdl.wptool.generate.utils.GenerateUtils;
import com.neusoft.bsdl.wptool.validator.WPScreenValidator;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Excel Generate", description = "Excelコード生成API")
public class ExcelGenerateController {

    private static final Logger LOG = LoggerFactory.getLogger(ExcelGenerateController.class);

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
            @Parameter(description = "関連するDBQuery設計書(複数可)", required = false, content = @Content(schema = @Schema(type = "array"))) @RequestParam(value = "dbQueryFiles", required = false) MultipartFile[] dbQueryFiles) {
        if (ioFiles == null || ioFiles.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded files are empty");
        }

        try {
            GeneratedCodeZipResponse responseData = generateIoZipBase64Response(ioFiles, dbQueryFiles);
            return ResponseEntity.ok(ApiResponse.success(responseData));
        } catch (WPException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to generate code from excel files", exception);
        }
    }

    @PostMapping("/excel/generate-dbquery-code")
    @Operation(summary = "画面設計書ExcelからIOコード生成", description = "アップロードされた画面設計書Excelを解析し、生成したコードをZIP化してBase64文字列で返します。")
    public ResponseEntity<ApiResponse<GeneratedCodeZipResponse>> generateDbQueryCode(
            @Parameter(description = "DBQuery設計書(複数可)", required = true, content = @Content(schema = @Schema(type = "array"))) @RequestParam("dbQueryFiles") MultipartFile[] dbQueryFiles) {
        if (dbQueryFiles == null || dbQueryFiles.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded files are empty");
        }

        try {
            GeneratedCodeZipResponse responseData = generateDbQueryZipBase64Response(dbQueryFiles);
            return ResponseEntity.ok(ApiResponse.success(responseData));
        } catch (WPException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to generate code from excel files", exception);
        }
    }

    private GeneratedCodeZipResponse generateDbQueryZipBase64Response(MultipartFile[] dbQueryFiles) throws Exception {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        Path taskDir = Paths.get(generateOutputRootDir, "generate-dbquery-code", taskId);
        Path taskInputDir = taskDir.resolve(Paths.get("input"));
        Path taskOutputDir = taskDir.resolve("output");
        Files.createDirectories(taskInputDir);
        Files.createDirectories(taskOutputDir);
        LOG.info("Start generateDbQueryZipBase64Response taskId={}, inputDir={}, outputDir={}", taskId, taskInputDir, taskOutputDir);

        List<String> errorLog = new ArrayList<>();
        List<String> warnLog = new ArrayList<>();
        int savedCount = 0;
        for (MultipartFile file : dbQueryFiles) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            savedCount++;
            Path savedFile = saveMultipartFile(taskInputDir, file, savedCount);

            FileSource fileSource = () -> Files.newInputStream(savedFile);
            DBQueryExcelContent queryExcelContent = ParseExcelUtils.parseDBQueryExcel(fileSource);
            for (DBQuerySheetContent sheetContent : queryExcelContent.getQuerySheetContents()) {
                WPDBQueryGenerator ioGenerator = new WPDBQueryGenerator(generateContext, sheetContent);
                ioGenerator.generate(taskOutputDir.toFile());
                errorLog.addAll(ioGenerator.getLogSnapshotError());
                warnLog.addAll(ioGenerator.getLogSnapshotWarn());
            }
        }
        writeTaskLogs(taskDir, errorLog, warnLog);
        byte[] zipBytes = zipDirectory(taskOutputDir);
        String zipBase64 = Base64.getEncoder().encodeToString(zipBytes);
        LOG.info("Finish generateDbQueryZipBase64Response taskId={}, savedFiles={}, errors={}, warns={}", taskId, savedCount, errorLog.size(), warnLog.size());
        return new GeneratedCodeZipResponse(zipBase64, taskId, errorLog, warnLog);
    }

    private GeneratedCodeZipResponse generateIoZipBase64Response(MultipartFile[] ioFiles, MultipartFile[] dbQueryFiles) throws Exception {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        Path taskDir = Paths.get(generateOutputRootDir, "generate-io-code", taskId);
        Path taskIoInputDir = taskDir.resolve(Paths.get("input"));
        Path taskDbQueryInputDir = taskIoInputDir.resolve(Paths.get("dbQuery"));
        Path taskOutputDir = taskDir.resolve(Paths.get("output"));
        Files.createDirectories(taskIoInputDir);
        Files.createDirectories(taskDbQueryInputDir);
        Files.createDirectories(taskOutputDir);
        LOG.info("Start generateIoZipBase64Response taskId={}, inputDir={}, outputDir={}", taskId, taskIoInputDir.getParent(), taskOutputDir);

        List<String> errorLog = new ArrayList<>();
        List<String> warnLog = new ArrayList<>();
        List<ScreenExcelContent> parsedContents = new ArrayList<>();
        List<ExcelSheetContent<CsvLayout>> csvLayoutList = new ArrayList<>();

        int savedIoCount = 0;
        for (MultipartFile file : ioFiles) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            savedIoCount++;
            Path savedFile = saveMultipartFile(taskIoInputDir, file, savedIoCount);
            FileSource fileSource = () -> Files.newInputStream(savedFile);
            ScreenExcelContent screenExcelContent = ParseExcelUtils.parseScreenExcel(fileSource);
            parsedContents.add(screenExcelContent);

            List<ExcelSheetContent<CsvLayout>> list = GenerateUtils.filterCsvLayoutSheetContents(screenExcelContent.getSheetList());
            csvLayoutList.addAll(list);
        }
        WPScreenValidator screenValidator = new WPScreenValidator(generateContext);
        for (ScreenExcelContent content : parsedContents) {
            try {
                screenValidator.validateParseContent(content);
            } catch (WPCheckException e) {
                errorLog.addAll(e.getErrors());
            }
        }
        List<DBQuerySheetContent> parsedDBQueryContents = new ArrayList<>();
        int savedDbQueryCount = 0;
        if (dbQueryFiles != null) {
            for (MultipartFile file : dbQueryFiles) {
                if (file == null || file.isEmpty()) {
                    continue;
                }

                savedDbQueryCount++;
                Path savedFile = saveMultipartFile(taskDbQueryInputDir, file, savedDbQueryCount);
                FileSource fileSource = () -> Files.newInputStream(savedFile);
                DBQueryExcelContent queryExcelContent = ParseExcelUtils.parseDBQueryExcel(fileSource);
                parsedDBQueryContents.addAll(queryExcelContent.getQuerySheetContents());
            }
        }
        // 生成画面的IO
        WPIOGenerator ioGenerator = new WPIOGenerator(generateContext, parsedContents, parsedDBQueryContents);
        ioGenerator.generate(taskOutputDir.toFile());
        errorLog.addAll(ioGenerator.getLogSnapshotError());
        warnLog.addAll(ioGenerator.getLogSnapshotWarn());

        // 生成画面中的CSVLayout
        WPIOExportGenerator exportCodeGenerator = new WPIOExportGenerator(generateContext, csvLayoutList, parsedDBQueryContents);
        exportCodeGenerator.generate(taskOutputDir.toFile());
        errorLog.addAll(exportCodeGenerator.getLogSnapshotError());
        warnLog.addAll(exportCodeGenerator.getLogSnapshotWarn());

        // 生成画画中的DB設定項目定義DM
        List<ExcelSheetContent<DBConfigDefinition>> dbConfigSheetList = GenerateUtils.filterDBConfigSheetContents(parsedContents);
        for (ExcelSheetContent<DBConfigDefinition> dbConfigSeet : dbConfigSheetList) {
            WPDBConfigGenerator dbConfigGenerator = new WPDBConfigGenerator(generateContext, dbConfigSeet.getContent());
            dbConfigGenerator.generate(taskOutputDir.toFile());
            errorLog.addAll(dbConfigGenerator.getLogSnapshotError());
            warnLog.addAll(dbConfigGenerator.getLogSnapshotWarn());
        }

        writeTaskLogs(taskDir, errorLog, warnLog);
        byte[] zipBytes = zipDirectory(taskOutputDir);
        String zipBase64 = Base64.getEncoder().encodeToString(zipBytes);
        LOG.info("Finish generateIoZipBase64Response taskId={}, savedIoFiles={}, savedDbQueryFiles={}, errors={}, warns={}", taskId, savedIoCount, savedDbQueryCount, errorLog.size(), warnLog.size());
        return new GeneratedCodeZipResponse(zipBase64, taskId, errorLog, warnLog);
    }

    private Path saveMultipartFile(Path targetDir, MultipartFile multipartFile, int fileIndex) throws Exception {
        String originalName = multipartFile.getOriginalFilename();
        String safeName = (originalName == null || originalName.isBlank()) ? "uploaded.bin" : Paths.get(originalName).getFileName().toString();
        String savedName = fileIndex + "_" + safeName;
        Path targetFile = targetDir.resolve(savedName);
        try (InputStream inputStream = multipartFile.getInputStream()) {
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
        return targetFile;
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

    private void writeTaskLogs(Path taskDir, List<String> errorLog, List<String> warnLog) throws Exception {
        Path logDir = taskDir.resolve("log");
        Files.createDirectories(logDir);
        Files.write(logDir.resolve("errorLog.txt"), errorLog, StandardCharsets.UTF_8);
        Files.write(logDir.resolve("warnLog.txt"), warnLog, StandardCharsets.UTF_8);
    }
}