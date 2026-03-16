package com.neusoft.bsdl.wptool.validator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.neusoft.bsdl.wptool.core.context.WPContext;
import com.neusoft.bsdl.wptool.core.exception.WPCheckException;
import com.neusoft.bsdl.wptool.core.model.DBQueryEntity;
import com.neusoft.bsdl.wptool.core.model.DBQueryExcelContent;
import com.neusoft.bsdl.wptool.core.model.DBQuerySheetContent;
import com.neusoft.bsdl.wptool.core.service.impl.WPTableSearchService;
import com.neusoft.bsdl.wptool.validator.CommonConstant.DBQUERY_SHEET;
import com.neusoft.bsdl.wptool.validator.service.impl.MessageService;

import cbai.util.db.define.FieldBean;
import cbai.util.db.define.TableBean;

public class WPDBQueryValidator {
	private WPContext context;

	public WPDBQueryValidator(WPContext context) {
		this.context = context;
	}

	public void validateParseContent(DBQueryExcelContent dbQueryExcelContent) throws WPCheckException {
		List<String> errors = new ArrayList<>();
		List<DBQuerySheetContent> querySheetContents = dbQueryExcelContent.getQuerySheetContents();
		querySheetContents.forEach(querySheetContent -> {
			String sheetName = querySheetContent.getSheetName();
			// TODO:[テーブル名称] WPネーミング規約にそった名称を記載していること。
			var tableNameMatcher = DBQUERY_SHEET.PATTERN_TABLE_NAME.matcher(querySheetContent.getTableName());
			if (!tableNameMatcher.matches()) {
				errors.add(sheetName + CommonConstant.MESSAGE_KUGIRI + MessageService
						.getMessage("error.db.query.tableName").replace("{0}", querySheetContent.getTableName()));
			}

			// TODO:[テーブルID] WPネーミング規約にそった名称を記載していること。
			var tableIDMatcher = DBQUERY_SHEET.PATTERN_TABLE_ID.matcher(querySheetContent.getTableId());
			if (!tableIDMatcher.matches()) {
				errors.add(sheetName + CommonConstant.MESSAGE_KUGIRI + MessageService
						.getMessage("error.db.query.tableId").replace("{0}", querySheetContent.getTableId()));
			}

			List<DBQueryEntity> queryEntities = querySheetContent.getQueryEntities();
			if (!CollectionUtils.isEmpty(queryEntities)) {
				Set<String> physicalNames = new HashSet<>();
				Set<String> logicalNames = new HashSet<>();
				querySheetContent.getQueryEntities().forEach(entity -> {
					// [カラム名] dbQuery内で一意の名称であること。
					// カラム名の物理名のユニックチェック
					String physicalName = entity.getPhysicalName();
					if (physicalName != null && !physicalNames.add(physicalName)) {
						errors.add(sheetName + CommonConstant.MESSAGE_KUGIRI + MessageService
								.getMessage("error.db.query.column.physicalName.unique").replace("{0}", physicalName));
					}

					// カラム名の論理名のユニックチェック
					String logicalName = entity.getLogicalName();
					if (logicalName != null && !logicalNames.add(logicalName)) {
						errors.add(sheetName + CommonConstant.MESSAGE_KUGIRI + MessageService
								.getMessage("error.db.query.column.logicalName.unique").replace("{0}", logicalName));
					}

					// テーブル名称取得
					String targetTable = entity.getResourceTableName();
					WPTableSearchService service = (WPTableSearchService) context.getTableSearchService();
					// データモデル名でテーブル定義書のコンテンツを取得する
					TableBean ｔableContent = service.findTableByFullName(targetTable);
					if (Objects.nonNull(ｔableContent)) {
						List<FieldBean> fieldList = ｔableContent.getFieldList();
						// カラム名は存在するかどうか
						if (!fieldList.stream()
								.anyMatch(column -> StringUtils.equals(column.getFieldFullName(), logicalName))) {
							// テーブル定義書の該当項目が存在しない場合、エラーとする
							errors.add(sheetName + CommonConstant.MESSAGE_KUGIRI
									+ MessageService.getMessage("error.db.query.column.exits")
											.replace("{0}", logicalName).replace("{1}", entity.getResourceTableName()));
							return;
						}
						// カラム名は存在する場合
						FieldBean fieldBean = fieldList.stream()
								.filter(field -> field.getFieldFullName().equals(logicalName)).findFirst().orElse(null);
						// TODO:[長さ／データ型]SELECTするテーブルの情報(テーブル定義書)と一致していること。
						if (!fieldBean.getOthers().get(CommonConstant.STR_WP_LEN_PRE).equals(entity.getLengthPre())) {
							errors.add(sheetName + CommonConstant.MESSAGE_KUGIRI
									+ MessageService.getMessage("error.db.query.column.length")
											.replace("{0}", logicalName).replace("{1}", entity.getLengthPre())
											.replace("{2}", fieldBean.getLen()));
						}
						if (!fieldBean.getOthers().get(CommonConstant.STR_WP_TYPE).equals(entity.getDataTypeWP())) {
							errors.add(sheetName + CommonConstant.MESSAGE_KUGIRI
									+ MessageService.getMessage("error.db.query.column.type")
											.replace("{0}", logicalName).replace("{1}", entity.getDataTypeWP())
											.replace("{2}", fieldBean.getType()));
						}
						// [キーグループ]主キー(一意)となるようにキーグループ１を設定していること。
						if (fieldBean.isKey() && !CommonConstant.GROUP_KEY.equals(entity.getKeyGroup())) {
							errors.add(sheetName + CommonConstant.MESSAGE_KUGIRI + MessageService
									.getMessage("error.db.query.column.groupKey").replace("{0}", logicalName));
						}
						// [NULL可]キーグループ１の項目と、BOOL型の項目に「FALSE」を設定していること。
						if (entity.getIsNullable() && (CommonConstant.GROUP_KEY.equals(entity.getKeyGroup())
								|| DBQUERY_SHEET.STR_BOOL.equals(entity.getDataTypeWP()))) {
							errors.add(sheetName + CommonConstant.MESSAGE_KUGIRI + MessageService
									.getMessage("error.db.query.column.isnull").replace("{0}", logicalName));
						}
					}
				});
			}
		});
		// エラーが存在する場合、例外をスローする
		if (!CollectionUtils.isEmpty(errors)) {
			throw new WPCheckException(errors);
		}
	}
}
