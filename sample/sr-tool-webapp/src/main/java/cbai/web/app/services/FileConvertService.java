package cbai.web.app.services;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cbai.tool.sr.SRSqlConvert;
import cbai.tool.sr.SRWprxCreator;
import cbai.tool.sr.SR詳細設計Reader;
import cbai.util.StringUtils;
import cbai.util.db.define.TableBean;
import cbai.util.log.UtilLogger;
import cbai.util.morphem.MorphemHelper;
import cbai.web.app.common.dto.OptionDto;
import cbai.web.app.common.services.NameConvertService;
import cbai.web.app.common.services.TableBeanService;
import cbai.web.app.fileconvert.form.FileConvertForm;
import cbai.web.app.fileconvert.services.IFileConvertService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FileConvertService implements IFileConvertService {

	@Autowired
	private TableBeanService tableBeanService;
	@Autowired
	private NameConvertService nameConvertService;

	@Override
	public File execute(FileConvertForm form, File outDir, File file) {
		if (!outDir.exists()) {
			outDir.mkdirs();
		}
		if ("1".equals(form.getOperationId())) {
			return createWPSources(form, outDir, file);

		}
		return null;
	}

	private File createWPSources(FileConvertForm form, File outDir, File file) {
		try {
			Map<String, String> queryIdMap = new HashMap<String, String>();
			List<TableBean> tableList = tableBeanService.loadTableInfos(form.getProjectId()).getTableList();
			SRSqlConvert sqlConvert = new SRSqlConvert(tableList);
			UtilLogger logger = new UtilLogger();
			SR詳細設計Reader reader = new SR詳細設計Reader(file.getAbsolutePath(), sqlConvert);
			reader.setLogger(logger);
			reader.read();
			List<TableBean> queryTableBeans = reader.getQueryTableBeans();
			if (queryTableBeans != null) {
				sqlConvert.addTableBean(queryTableBeans.toArray(new TableBean[queryTableBeans.size()]));
			}
			MorphemHelper morphemHelper = nameConvertService.loadMorphemHelper(form.getProjectId());
			SRWprxCreator creator = new SRWprxCreator(reader.getSr詳細設計Bean(), sqlConvert, morphemHelper);
			creator.setLogger(logger);
			creator.createALL(outDir, "", queryIdMap);
			logger.writeToFile(new File(outDir, "log.txt"));
			form.setOutLog(StringUtils.join(logger.getLogList().toArray(), "\n"));
			reader.close();
			return outDir;
		} catch (Exception e) {
			log.error("createWPSources", e);
			form.setOutLog(e.getMessage());
		}
		return null;
	}

	@Override
	public List<OptionDto> getOperations(FileConvertForm form) {
		List<OptionDto> options = new ArrayList<OptionDto>();
		options.add(new OptionDto("1", "ソース生成"));
		return options;
	}

}
