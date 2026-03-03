package cbai.tool.sr;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Sheet;

import cbai.util.FileUtil;
import cbai.util.StringUtils;
import cbai.util.db.define.FieldBean;
import cbai.util.db.define.TableBean;
import cbai.util.db.define.reader.AbstractExcelTableBeanReader;
import cbai.util.db.define.type.ColumnType;
import cbai.util.db.define.type.ITypeConvert;
import cbai.util.db.define.type.JavaColumnType;
import cbai.util.db.define.type.JdbcColumnType;
import cbai.util.db.tool.mybatis.DBMybatisCreator;
import cbai.util.excel.ExcelUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SRTableBeanReader extends AbstractExcelTableBeanReader {

    public SRTableBeanReader(String excelPath) {
        super(excelPath);
    }

    @Override
    protected boolean isTableBeanSheet(ExcelUtil excelUtil, Sheet sheet, int sheetIndex) {
        if (!StringUtils.equals("テーブル名", excelUtil.getCellStringValue(sheet, 3, 0).trim()) && !StringUtils.equals("テ－ブル名", excelUtil.getCellStringValue(sheet, 3, 0).trim())) {
            return false;
        }
        String tableFullName = excelUtil.getCellStringValue(sheet, 4, 1).trim();
        if (tableFullName == null || "".equals(tableFullName)) {
            return false;
        }
        String tableName = excelUtil.getCellStringValue(sheet, 5, 1).trim();
        if (StringUtils.isEmpty(tableName)) {
            return false;
        }
        return true;
    }

    @Override
    protected List<TableBean> readTableBeans(ExcelUtil excelUtil, Sheet sheet) {
        TableBean tableBean = new TableBean();
        String tableName = excelUtil.getCellStringValue(sheet, 5, 1).trim();
        String tableFullName = excelUtil.getCellStringValue(sheet, 4, 1).trim();
        tableBean.setTableFullName(tableFullName);
        tableBean.setTableName(tableName);
        tableBean.setSourceName(excelUtil.getExcelFilePath());
        List<FieldBean> fieldList = new ArrayList<>();
        for (int row = 4; row <= sheet.getLastRowNum(); row++) {
            String no = excelUtil.getCellStringValue(sheet, 3, row).trim();
            if (no.matches("\\d+")) {

                FieldBean fieldBean = new FieldBean();
                String fieldName = excelUtil.getCellStringValue(sheet, 5, row).trim();
                if (StringUtils.isEmpty(fieldName)) {
                    log.warn(excelUtil.getExcelFilePath() + " no=" + no + " fieldName is empty");
                } else {
                    String fieldFullName = excelUtil.getCellStringValue(sheet, 4, row).trim();
                    String type = excelUtil.getCellStringValue(sheet, 8, row).trim();
                    String len = excelUtil.getCellStringValue(sheet, 6, row).trim();
                    String dotLen = excelUtil.getCellStringValue(sheet, 7, row).trim();
                    String key = excelUtil.getCellStringValue(sheet, 9, row).trim();
                    String comment = excelUtil.getCellStringValue(sheet, 13, row).trim();
                    fieldBean.setFieldFullName(fieldFullName);

                    fieldBean.setFieldName(fieldName);

                    fieldBean.setType(type);

                    fieldBean.setLen(len);

                    fieldBean.setDotLen(dotLen);
                    fieldBean.setComment(comment);
                    if (key.matches("\\d+")) {
                        fieldBean.setKey(true);
                    }
                    Map<String, String> others = new HashMap<>();
                    others.put("WP_TYPE", excelUtil.getCellStringValue(sheet, 12, row).trim());
                    others.put("WP_LEN", excelUtil.getCellStringValue(sheet, 10, row).trim());
                    others.put("WP_DOTLEN", excelUtil.getCellStringValue(sheet, 11, row).trim());
                    fieldBean.setOthers(others);
                    fieldList.add(fieldBean);
                }
            }
        }
        tableBean.setFieldList(fieldList);
        return Arrays.asList(tableBean);
    }

    public static void main(String[] args) throws IOException {
        List<TableBean> list = SRUtils.getTableList();
        System.out.println(list.size());
        Set<String> typeList = new HashSet<>();
        list.forEach((b) -> {
            System.out.println(b);
            for (FieldBean fb : b.getFieldList()) {
                typeList.add(fb.getType());
            }
        });
        System.out.println(typeList);
        DBMybatisCreator creator = new DBMybatisCreator(list, new ITypeConvert() {
            @Override
            public ColumnType convert(String type) {
                if ("varchar".equalsIgnoreCase(type)) {
                    return new ColumnType(JavaColumnType.STRING, JdbcColumnType.VARCHAR);
                } else if ("date".equalsIgnoreCase(type)) {
                    return new ColumnType(JavaColumnType.LOCAL_DATE, JdbcColumnType.DATE);
                } else if ("datetime".equalsIgnoreCase(type)) {
                    return new ColumnType(JavaColumnType.LOCAL_DATE_TIME, JdbcColumnType.TIMESTAMP);
                } else if ("decimal".equalsIgnoreCase(type)) {
                    return new ColumnType(JavaColumnType.BIG_DECIMAL, JdbcColumnType.DECIMAL);
                } else if ("image".equalsIgnoreCase(type)) {
                    return new ColumnType(JavaColumnType.BYTE_ARRAY, JdbcColumnType.LONGVARBINARY);
                } else if ("int IDENTITY(1,1)".equalsIgnoreCase(type)) {
                    return new ColumnType(JavaColumnType.BIG_DECIMAL, JdbcColumnType.DECIMAL);
                }
                throw new UnsupportedOperationException(type);
            }
        }, "cbai.tool.sr.DB_TEMPLATE", true);
        creator.setEntitySuffix("Dto");
        creator.setEntityPkg("jp.co.yrl.sirius_rt.batch.cm.dto");
        creator.setMapperPkg("jp.co.yrl.sirius_rt.batch.cm.mapper");
        File outDir = new File("./target/out");
        FileUtil.deleteDirectory(outDir);

        creator.generateDBEntity(outDir);
        File ddlDir = new File(outDir, "ddl");
        File[] ddlFileList = ddlDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".sql");
            }
        });
        StringBuilder sb = new StringBuilder();
        sb.append("USE SIRIUSRT_DB").append("\r\n");
        for (File file : ddlFileList) {
            String ddl = FileUtil.readFileAsString(file, "UTF-8");
            ddl = ddl.replaceAll("\r\n\\W*,", ",");
            sb.append(ddl).append("\r\n");
        }
        FileUtil.writeStringToFile("sqlcmd -S localhost,1433 -E -f i:65001 -i .\\ddl.sql\r\npause", new File(outDir, "ddl.bat"), "UTF-8");
        FileUtil.writeStringToFile(sb.toString(), new File(outDir, "ddl.sql"), "UTF-8");
    }

}
