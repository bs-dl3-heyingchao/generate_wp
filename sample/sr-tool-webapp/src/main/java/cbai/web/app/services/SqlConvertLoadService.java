package cbai.web.app.services;

import java.util.List;

import org.springframework.stereotype.Service;

import cbai.tool.sr.SRSqlConvert;
import cbai.util.db.define.TableBean;
import cbai.util.sqlconvert.SqlConverterAbstract;
import cbai.web.app.toolbox.services.ISqlConvertLoadService;

@Service
public class SqlConvertLoadService implements ISqlConvertLoadService {

	@Override
	public SqlConverterAbstract execute(String projectId, List<TableBean> tableList) {
		return new SRSqlConvert(tableList);
	}

}
