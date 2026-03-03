package sample;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cbai.tool.sr.SRUtils;
import cbai.util.db.define.TableBean;

public class SqlConvertTest {

    public static void main(String[] args) {
//		SRSqlConvert convert = SRUtils.getSqlConvert();
//		String input = FileUtil.readFileAsString("classpath:/sample/input.sql", "UTF-8");
//		System.out.println(input);
//		String output = convert.convert(input);
//		System.out.println(output);
//		
        List<TableBean> list = SRUtils.getTableList();
        System.out.println(list.size());
        Map<String, Integer> map = new HashMap<>();
        for (TableBean tb : list) {
            if (!map.containsKey(tb.getSourceName())) {
                map.put(tb.getSourceName(), 1);
            } else {
                map.put(tb.getSourceName(), map.get(tb.getSourceName()) + 1);
            }
        }
        map.forEach((key, value) -> {
            if (value > 1) {
                System.out.println(key + ":" + value);
            }
        });
    }

}
