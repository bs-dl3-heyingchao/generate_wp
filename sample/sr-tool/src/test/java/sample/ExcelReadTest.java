package sample;

import java.io.IOException;

import org.apache.poi.ss.usermodel.Sheet;

import cbai.util.excel.ExcelUtil;

public class ExcelReadTest {

    public static void main(String[] args) throws IOException {
        ExcelUtil excelUtil = new ExcelUtil("./テーブル定義_KB_個別見積.xlsx");
        Sheet sheet = excelUtil.getSheet("改版履歴");
        String str = excelUtil.getCellStringValue(sheet, 0, 4);
        System.out.println(str);
        excelUtil.close();
    }
}
