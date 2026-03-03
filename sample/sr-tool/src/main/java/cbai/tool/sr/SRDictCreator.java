package cbai.tool.sr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cbai.util.FileUtil;
import cbai.util.db.define.FieldBean;
import cbai.util.db.define.TableBean;
import cbai.util.morphem.MorphemDicMaker;
import cbai.util.morphem.MorphemHelper;

public class SRDictCreator {

	public static List<String[]> getDictList() throws IOException {
		File cahceFile = new File("./dict.txt");
//		if (cahceFile.exists()) {
//			return MorphemHelper.readExtendDic(cahceFile.getAbsolutePath());
//		}
		List<TableBean> tableList = SRUtils.getTableList();
		List<String[]> inputDict = new ArrayList<String[]>();
		for (TableBean tb : tableList) {
			for (FieldBean fb : tb.getFieldList()) {
				inputDict.add(new String[] { fb.getFieldFullName(), fb.getFieldName().substring(2) });
				if (fb.getFieldFullName().contains("№")) {
					inputDict.add(
							new String[] { fb.getFieldFullName().replace("№", "No"), fb.getFieldName().substring(2) });
				}
			}
		}
		List<String> extDicts = FileUtil.readFromFile("classpath:/cbai/tool/sr/EXT_DICT.txt", "UTF-8");
		MorphemDicMaker maker = new MorphemDicMaker(inputDict);
		StringBuilder sb = new StringBuilder();
		List<String[]> dictList = maker.getDicList();
		dictList.add(new String[] { "(", "_" });
		dictList.add(new String[] { ")", "_" });
//		dictList.add(new String[] { "ワーニング", "WARNING" });
//		dictList.add(new String[] { "見積", "MRI" });
		for (String cnt : extDicts) {
			String[] tmp = cnt.split("\t");
			if (tmp.length == 2) {
				dictList.add(new String[] { tmp[0], tmp[1] });
			}
		}
		for (String[] item : dictList) {
			sb.append(item[0] + "\t" + item[1]).append("\n");
		}
		FileUtil.writeStringToFile(sb.toString(), cahceFile, "UTF-8");
		return dictList;
	}

	public static void main(String[] args) throws IOException {
		List<String[]> list = getDictList();

		MorphemHelper help = new MorphemHelper(list);
		System.out.println(help.getRomaFromKanji("見積No"));
	}

}
