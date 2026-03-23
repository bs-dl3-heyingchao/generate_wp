package com.neusoft.bsdl.wptool.generate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.neusoft.bsdl.wptool.core.context.WPContext;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.io.LocalFileSource;
import com.neusoft.bsdl.wptool.core.model.ScreenExcelContent;
import com.neusoft.bsdl.wptool.core.service.ConfigService;
import com.neusoft.bsdl.wptool.core.service.ParseExcelUtils;
import com.neusoft.bsdl.wptool.generate.context.WPGenerateContext;

import tools.jackson.databind.ObjectMapper;

public class WpCodeGeneratorTest {

    public static void main(String[] args) throws Exception {
//        File outputDir = new File("D:\\WPQuickStartPackage_2.7.0\\workspace\\wp-sample\\");

        // 请修改为本地实际的SVN根目录
        // System.setProperty("wp-tool.svn.base-dir", "D:/WORK/128SYIS25142_devora");
        File outputDir = new File("target/output/io", System.currentTimeMillis() + "");
        WPGenerateContext context = new WPGenerateContext(WPContext.create());
        String[] inputFiles = new String[] { "\\test\\模版\\設計書\\KHT003P01_総合結果一覧検索エリア部分入出力.xlsx", "\\test\\模版\\設計書\\KHT004P01_総合結果一覧検索結果部分入出力.xlsx",
                "\\test\\模版\\設計書\\KHT010S01B03_画面設計書_総合試験結果一覧［工事管理部門］.xlsx" };
        List<ScreenExcelContent> screenExcelContents = new ArrayList<>();
        for (String file : inputFiles) {
            System.out.println(file);
            FileSource source = new LocalFileSource(ConfigService.getSvnFullPath(file));
            ScreenExcelContent screenExcelContent = ParseExcelUtils.parseScreenExcel(source);
            screenExcelContents.add(screenExcelContent);
        }

        File outputFile = new File("./target/screenExcelContents.json");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, screenExcelContents);

        WPIOGenerator codeGenerator = new WPIOGenerator(context, screenExcelContents);
        codeGenerator.generate(outputDir);
    }

}
