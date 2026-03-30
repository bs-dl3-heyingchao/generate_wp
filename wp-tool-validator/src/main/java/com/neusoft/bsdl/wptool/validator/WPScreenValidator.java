package com.neusoft.bsdl.wptool.validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.neusoft.bsdl.wptool.core.context.WPContext;
import com.neusoft.bsdl.wptool.core.exception.WPCheckException;
import com.neusoft.bsdl.wptool.core.model.DBConfigDefinition;
import com.neusoft.bsdl.wptool.core.model.DBQuerySheetContent;
import com.neusoft.bsdl.wptool.core.model.ExcelSheetContent;
import com.neusoft.bsdl.wptool.core.model.MessageDefinition;
import com.neusoft.bsdl.wptool.core.model.ScreenDefinition;
import com.neusoft.bsdl.wptool.core.model.ScreenDefinitionTargetData;
import com.neusoft.bsdl.wptool.core.model.ScreenExcelContent;
import com.neusoft.bsdl.wptool.core.model.ScreenItemDescriptionResult;
import com.neusoft.bsdl.wptool.core.model.ScreenValidation;
import com.neusoft.bsdl.wptool.core.service.impl.WPMessageLoaderService;
import com.neusoft.bsdl.wptool.core.service.impl.WPTableSearchService;
import com.neusoft.bsdl.wptool.validator.CommonConstant.SCREEN_DEFINITION_SHEET;
import com.neusoft.bsdl.wptool.validator.CommonConstant.SCREEN_ITEM_DESCRIPTION_SHEET;
import com.neusoft.bsdl.wptool.validator.CommonConstant.SCREEN_VALIDATION_SHEET;
import com.neusoft.bsdl.wptool.validator.enums.ItemDescriptionIOEnum;
import com.neusoft.bsdl.wptool.validator.service.impl.MessageService;
import com.neusoft.bsdl.wptool.validator.utils.CommonUtils;

import cbai.util.db.define.TableBean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WPScreenValidator {
	public static final String STR_KORON = ":";
	private WPContext context;

	public WPScreenValidator(WPContext context) {
		this.context = context;
	}

	/**
	 * 仕様書の解析内容に対してバリデーションチェックを実施する。
	 * 
	 * @param screenExcelContent 解析済みの画面Excelコンテンツ
	 * @throws WPCheckException チェックエラーが発生した場合
	 */
	public void validateParseContent(ScreenExcelContent screenExcelContent,List<DBQuerySheetContent> dbQuerySheetContents) throws WPCheckException {
		List<String> errors = new ArrayList<>();
		// 画面定義情報
		ScreenDefinition screenDefinitionValidObj = null;
		String errorPrex = "[" + screenExcelContent.getScreenId() + STR_KORON + screenExcelContent.getScreenName()
				+ " ";

		for (ExcelSheetContent<?> sheet : screenExcelContent.getSheetList()) {
			String sheetName = sheet.getSheetName();
			// [画面ID:画面名称 シート名称] 例：[KHT004P01:総合結果一覧検索結果部分入出力 画面項目説明書]
			errorPrex = errorPrex + sheetName + "] ";
			if (CommonConstant.PARSE_SHEET_NAME.SCREEN_DEFINITION_SHEET.equals(sheetName)) {
				screenDefinitionValidObj = (ScreenDefinition) sheet.getContent();
				// 画面定義書のバリデーションチェックを実施する
				validateScreenDefinition(errorPrex, screenDefinitionValidObj, errors);
			} else if (sheetName.indexOf(CommonConstant.PARSE_SHEET_NAME.SCREEN_ITEM_DESCRIPTION_SHEET) != -1) {
				// 画面項目説明書
				List<ScreenItemDescriptionResult> validList = (List<ScreenItemDescriptionResult>) sheet.getContent();
				// 画面項目説明書のバリデーションチェックを実施する
				validateScreenItemDescription(errorPrex, validList, screenDefinitionValidObj, errors);
			} else if (CommonConstant.PARSE_SHEET_NAME.SCREEN_VALIDATION_SHEET.equals(sheetName)) {
				// 画面バリデーション定義書
				List<ScreenValidation> validList = (List<ScreenValidation>) sheet.getContent();
				// 画面バリデーション定義書のバリデーションチェックを実施する
				validateScreenValidation(errorPrex, validList, errors);
			} else if (CommonConstant.PARSE_SHEET_NAME.BP_SHEET.equals(sheetName)) {
				// BP定義書（現状未対応）
			} else if (sheetName.indexOf(CommonConstant.PARSE_SHEET_NAME.DB_CONFIG_SHEET) != -1) {
				// DB設定定義書
				DBConfigDefinition validObj = (DBConfigDefinition) sheet.getContent();
				validateDBConfigDefinition(errorPrex, validObj, errors);
			}
		}
		// エラーが存在する場合、例外をスローする
		if (!CollectionUtils.isEmpty(errors)) {
			throw new WPCheckException(errors);
		}
	}

	/**
	 * DB設定定義書のバリデーションチェックを実施する。
	 * 
	 * @param errorPrex エラーメッセージのプレフィックス（問題箇所の特定用）
	 * @param validObj  DB設定定義オブジェクト
	 * @param errors    エラーリスト
	 */
	private void validateDBConfigDefinition(String errorPrex, DBConfigDefinition validObj, List<String> errors) {
		// 登録・更新操作ごとに別シートを作成すること。
		// [操作] 列には「登録」または「更新」のいずれかを記載すること。
		validObj.getProcessList().forEach(item -> {
			if (!CommonConstant.DB_CONFIG_SHEET.STR_DB_OPERATION_INSERT_NAME.equals(item.getOperation())
					&& !CommonConstant.DB_CONFIG_SHEET.STR_DB_OPERATION_UPD_NAME.equals(item.getOperation())) {
				errors.add(errorPrex + CommonConstant.MESSAGE_KUGIRI
						+ MessageService.getMessage("error.db.config.operation").replace("{0}", item.getOperation()));
			}

			// TODO: [操作コード] はWPネーミング規約に準拠していること。
			if (!isValidCode(item.getOperationCode(), item.getOperation())) {
				errors.add(errorPrex + CommonConstant.MESSAGE_KUGIRI + MessageService
						.getMessage("error.db.config.operationCode").replace("{0}", item.getOperation()));
			}

			// TODO: [名称] はWPネーミング規約に準拠していること。
			if (!isValidName(item.getTableName())) {
				errors.add(errorPrex + CommonConstant.MESSAGE_KUGIRI + MessageService
						.getMessage("error.db.config.operationTableName").replace("{0}", item.getTableName()));
			}
			// [項目] はテーブル定義に存在する項目であること。
			WPTableSearchService service = (WPTableSearchService) context.getTableSearchService();
			// データモデル名でテーブル定義書のコンテンツを取得する
			TableBean tableContent = service.findTableByFullName(item.getDataModel());
			// 項目名は、テーブル定義書の論理名称と一致していること。
			if (!Objects.isNull(tableContent)) {
				item.getDetails().forEach(detail -> {
					if (!tableContent.getFieldList().stream().anyMatch(
							column -> StringUtils.equals(column.getFieldFullName(), detail.getLogicalName()))) {
						// テーブル定義に該当項目が存在しない場合、エラーとする
						errors.add(errorPrex + CommonConstant.MESSAGE_KUGIRI
								+ MessageService.getMessage("error.db.config.logicalName.exits")
										.replace("{0}", detail.getLogicalName()).replace("{1}", item.getDataModel()));
					}
				});
			} else {
				// テーブル定義が存在しない場合、エラーとする
				errors.add(errorPrex + CommonConstant.MESSAGE_KUGIRI
						+ MessageService.getMessage("error.db.config.table.exits").replace("{0}", item.getDataModel()));
			}

		});
	}

	/**
	 * 操作コードのバリデーションチェックを行う。
	 * 
	 * @param code      操作コード（例: "MTI350S01_I010"）
	 * @param operation 操作タイプ（"登録" | "更新"）
	 * @return チェック結果
	 */
	public static boolean isValidCode(String code, String operation) {
		if (code == null || operation == null) {
			return false;
		}

		var matcher = CommonConstant.DB_CONFIG_SHEET.PATTERN_OPERATION_CODE.matcher(code);
		if (!matcher.matches()) {
			return false;
		}
		// プレフィックス種別を取得
		String prefixType = matcher.group(1);

		if (CommonConstant.DB_CONFIG_SHEET.STR_DB_OPERATION_INSERT_NAME.equals(operation)) {
			return CommonConstant.DB_CONFIG_SHEET.STR_DB_OPERATION_INSERT_PREFIX.equals(prefixType);
		} else if (CommonConstant.DB_CONFIG_SHEET.STR_DB_OPERATION_UPD_NAME.equals(operation)) {
			return CommonConstant.DB_CONFIG_SHEET.STR_DB_OPERATION_UPD_PREFIX.equals(prefixType);
		} else {
			return false;
		}
	}

	/**
	 * TODO: 名称のバリデーションチェックを行う。
	 * 
	 * @param name 名称
	 * @return チェック結果
	 */
	public static boolean isValidName(String name) {
		return true;
		// return name != null && !name.isEmpty() &&
		// CommonConstant.DB_CONFIG_SHEET.PATTERN_NAME.matcher(name).matches();
	}

	/**
	 * 画面定義書のバリデーションチェックを実施する。
	 * 
	 * @param errorPrex                エラーメッセージのプレフィックス
	 * @param screenDefinitionValidObj 画面定義書の解析内容
	 * @param errors                   エラーリスト
	 */
	private void validateScreenDefinition(String errorPrex, ScreenDefinition screenDefinitionValidObj,
			List<String> errors) {
		// 画面定義書の対象データモデルの「論理名称」は、テーブル定義書の「論理名称」と一致していること。
		screenDefinitionValidObj.getTargetModels().forEach(item -> {
			WPTableSearchService service = (WPTableSearchService) context.getTableSearchService();
			// データモデル名でテーブル定義書のコンテンツを取得
			TableBean tableContent = service.findTableByFullName(item.getLogicalName());
			// テーブル定義が存在しない場合、エラーとする
			if (Objects.isNull(tableContent)) {
				errors.add(errorPrex + CommonConstant.MESSAGE_KUGIRI
						+ MessageService.getMessage("error.screen.definition.model.logicname.exits").replace("{0}",
								item.getLogicalName()));
			}
		});
		// [対象条件] のフォーマットチェック
		// @NAMEDPARAMが存在する場合、複数条件はコンマ区切りであること。
		// @NAMEDPARAMが存在しない場合、AND/OR形式で記述し、コンマを使用しないこと。
		String targetCondition = screenDefinitionValidObj.getTargetCondition();
		if (!isValidCondition(targetCondition)) {
			errors.add(errorPrex + CommonConstant.MESSAGE_KUGIRI + MessageService
					.getMessage("error.screen.definition.condition.format").replace("{0}", targetCondition));
		}
	}

	/**
	 * [対象条件] のフォーマット妥当性をチェックする。
	 * 
	 * @param condition 条件文字列
	 * @return 妥当性
	 */
	private boolean isValidCondition(String condition) {
		String trimmed = condition.trim();

		// 共通ルール：末尾にコンマ、AND、ORが来ない（末尾空白は無視）
		if (SCREEN_DEFINITION_SHEET.INVALID_ENDING.matcher(trimmed).matches()) {
			return false;
		}

		boolean hasNamedParam = trimmed.startsWith(SCREEN_DEFINITION_SHEET.NAMED_PARAM);

		if (hasNamedParam) {
			// @NAMEDPARAMプレフィックスを除去
			String content = trimmed.substring(SCREEN_DEFINITION_SHEET.NAMED_PARAM.length()).trim();

			// ANDまたはORが任意位置に出現してはならない
			if (SCREEN_DEFINITION_SHEET.AND_OR_ANYWHERE.matcher(content).find()) {
				return false;
			}

			return true;
		} else {
			// コンマの使用は禁止
			if (condition.contains(",")) {
				return false;
			}

			return true;
		}
	}

	/**
	 * 画面バリデーション定義書のバリデーションチェックを実施する。
	 * 
	 * <<<<<<< HEAD
	 * 
	 * @param errorPrex エラーメッセージのプレフィックス
	 * @param validList バリデーション対象リスト
	 * @param errors    エラーリスト =======
	 * @param errorPrex エラーメッセージのプレフィックス
	 * @param validList バリデーション対象リスト
	 * @param errors    エラーリスト >>>>>>> b07c7bcda8bc7400f991c69c74b5c994cde3d2e1
	 */
	private void validateScreenValidation(String errorPrex, List<ScreenValidation> validList, List<String> errors) {
		for (ScreenValidation screenValidation : validList) {
			// [メッセージID／メッセージ内容／パラメータ]
			// 種別が「エラー」の場合：
			// - メッセージIDおよびメッセージ内容を記載すること。
			// - メッセージIDと内容は、共通メッセージ一覧またはサブメッセージと一致すること。
			// - メッセージ内に{n}が含まれる場合、パラメータを記載すること。
			if (SCREEN_VALIDATION_SHEET.STR_ERROR.equals(screenValidation.getType())) {
				if (StringUtils.isEmpty(screenValidation.getMessageId())
						|| StringUtils.isEmpty(screenValidation.getMessageContent())) {
					errors.add(errorPrex + CommonConstant.MESSAGE_KUGIRI
							+ MessageService.getMessage("error.screen.validation.msg.required").replace("{0}",
									screenValidation.getItemNo()));
					continue;
				}

				// メッセージIDと内容の整合性チェック
				WPMessageLoaderService service = (WPMessageLoaderService) context.getMessageLoaderService();
				MessageDefinition messageObj = service.findMessageById(screenValidation.getMessageId());
				// メッセージIDが存在しない場合、エラー
				if (Objects.isNull(messageObj)) {
					errors.add(errorPrex + CommonConstant.MESSAGE_KUGIRI
							+ MessageService.getMessage("error.screen.validation.error.message").replace("{0}",
									screenValidation.getItemNo()));
				}
				// メッセージ内容が一致しない場合、エラー
				if (!Objects.isNull(messageObj)
						&& !StringUtils.equals(messageObj.getMessageText(), screenValidation.getMessageContent())) {
					errors.add(errorPrex + CommonConstant.MESSAGE_KUGIRI
							+ MessageService.getMessage("error.screen.validation.error.message.mismatch")
									.replace("{0}", screenValidation.getItemNo())
									.replace("{1}", messageObj.getMessageText()));
				}
			}
			// TODO: メッセージ内に{n}が含まれる場合、パラメータを記載すること。

			// [メッセージID／メッセージ内容／パラメータ]
			// 種別が「ワーニング」の場合：
			// - メッセージIDは記載しないこと。
			// - メッセージ内容には「別シート参照」と明記すること（設計書サンプルに準拠）。
			if (SCREEN_VALIDATION_SHEET.STR_WARNING.equals(screenValidation.getType())) {
				if (!StringUtils.isEmpty(screenValidation.getMessageId())) {
					errors.add(errorPrex + CommonConstant.MESSAGE_KUGIRI
							+ MessageService.getMessage("warning.screen.validation.msgId.not.required").replace("{0}",
									screenValidation.getItemNo()));
					continue;
				}
				// TODO: メッセージ内容に「別シート参照」と記載されていること。
			}
		}
	}

	/**
	 * 画面項目説明書のバリデーションチェックを実施する。
	 * 
	 * @param errorPrex                エラーメッセージのプレフィックス
	 * @param validList                バリデーション対象リスト
	 * @param screenDefinitionValidObj 画面定義書の解析内容（対象データモデル情報のチェックに使用）
	 * @param errors                   エラーリスト
	 */
	private void validateScreenItemDescription(String errorPrex, List<ScreenItemDescriptionResult> validList,
			ScreenDefinition screenDefinitionValidObj, List<String> errors) {
		for (ScreenItemDescriptionResult screenItemDescription : validList) {
			screenItemDescription.getItems().forEach(item -> {
				// 項目名の内容には[部分入出力が含む場合、バリデーションチェック対象外とする
				if (!StringUtils.isEmpty(item.getItemName()) && item.getItemName().indexOf("[部分入出力") >= -1) {
					return;
				}
				// 画面項目説明書の「IO」列には、「I（入力）」「O（出力）」「A（アクション）」「G（グループ）」「IO（入出力）」以外の値を記載不可
				if (!ItemDescriptionIOEnum.getAllDisplayNames().contains(item.getIo())) {
					errors.add(errorPrex + CommonConstant.MESSAGE_KUGIRI + MessageService
							.getMessage("error.screen.item.description.io").replace("{0}", item.getItemNo()));
				}
				// [属性(WP)／桁数(WP)]:
				// IOが「A（アクション）」または「G（グループ）」の場合、「-」を記載すること。
				if ((ItemDescriptionIOEnum.ACTION.getDisplayName().equals(item.getIo())
						|| ItemDescriptionIOEnum.GROUP.getDisplayName().equals(item.getIo()))
						&& (!Arrays.asList(SCREEN_ITEM_DESCRIPTION_SHEET.ARR_HAIHUN).contains(item.getAttributeWP())
								|| !Arrays.asList(SCREEN_ITEM_DESCRIPTION_SHEET.ARR_HAIHUN)
										.contains(item.getLengthWP()))) {
					errors.add(errorPrex + CommonConstant.MESSAGE_KUGIRI + MessageService
							.getMessage("error.screen.item.description.wp").replace("{0}", item.getItemNo()));
				}

				// [属性(WP)／桁数(WP)]:
				// IOが「IO（入出力）」または「O（出力）」の場合：
				// - 対象データモデル情報が記載されている場合、「DM」を記載すること。
				if ((ItemDescriptionIOEnum.IO.getDisplayName().equals(item.getIo())
						|| ItemDescriptionIOEnum.OUTPUT.getDisplayName().equals(item.getIo()))
						&& (((!Arrays.asList(SCREEN_ITEM_DESCRIPTION_SHEET.ARR_OUTSIDE_SCOPE)
								.contains(item.getModelName()))
								&& (!SCREEN_ITEM_DESCRIPTION_SHEET.STR_DM.equals(item.getAttributeWP())
										|| !SCREEN_ITEM_DESCRIPTION_SHEET.STR_DM.equals(item.getLengthWP()))))) {
					errors.add(errorPrex + CommonConstant.MESSAGE_KUGIRI + MessageService
							.getMessage("error.screen.item.description.wp").replace("{0}", item.getItemNo()));
				}

				// [対象データモデル情報]
				// - データモデル名は、画面定義書の「対象データモデル」欄に記載された論理名称と一致すること。
				// - 項目名は、該当データモデルに存在する項目であること。
				Set<String> logicalNames = screenDefinitionValidObj.getTargetModels().stream()
						.map(ScreenDefinitionTargetData::getLogicalName).filter(Objects::nonNull)
						.collect(Collectors.toSet());
				if (!Arrays.asList(SCREEN_ITEM_DESCRIPTION_SHEET.ARR_OUTSIDE_SCOPE).contains(item.getModelName())
						&& !logicalNames.contains(item.getModelName())) {
					errors.add(errorPrex + CommonConstant.MESSAGE_KUGIRI
							+ MessageService.getMessage("error.screen.item.description.modelName.exits")
									.replace("{0}", item.getModelName()).replace("{1}", item.getItemNo()));
				}

				// [フォーマット]
				// TODO: 数値項目・日付項目（非表示項目を除く）の場合、フォーマットを記載すること。
				if (!SCREEN_ITEM_DESCRIPTION_SHEET.STR_NO_DISPLAY.equals(item.getDisplay())
						&& Arrays.asList(SCREEN_ITEM_DESCRIPTION_SHEET.ARR_DATA_TYPE).contains(item.getAttribute())
						&& (Objects.isNull(item.getFormat()) || item.getFormat().isEmpty())) {
					errors.add(errorPrex + CommonConstant.MESSAGE_KUGIRI + MessageService
							.getMessage("error.screen.item.description.format").replace("{0}", item.getItemNo()));
				}

				// [選択リスト]
				// ドロップダウン／ラジオボタン／複数選択チェックボックス形式の項目の場合、選択リスト内容を記載すること。
				if (item.getItemName().indexOf(SCREEN_ITEM_DESCRIPTION_SHEET.STR_DROPDOWN) > 0
						|| item.getItemName().indexOf(SCREEN_ITEM_DESCRIPTION_SHEET.STR_RADIO_BUTTON) > 0
						|| item.getItemName().indexOf(SCREEN_ITEM_DESCRIPTION_SHEET.STR_DUPLICATE_CHECKBOX) > 0) {
					// 選択リストが空の場合、エラー
					if (StringUtils.isEmpty(item.getSelectList())) {
						errors.add(errorPrex + CommonConstant.MESSAGE_KUGIRI
								+ MessageService.getMessage("error.screen.item.description.selectlist").replace("{0}",
										item.getItemNo()));
					}
					// フォーマットチェック（必須タグの存在確認）
					if (!StringUtils.isEmpty(item.getSelectList())
							&& !CommonUtils.containsAllRequiredTags(item.getSelectList(),
									CommonConstant.SCREEN_ITEM_DESCRIPTION_SHEET.ARR_SELECT_LIST)) {
						errors.add(errorPrex + CommonConstant.MESSAGE_KUGIRI
								+ MessageService.getMessage("error.screen.item.description.selectlist.format")
										.replace("{0}", item.getItemNo()));
					}
				}
			});
		}
		// [ソート順]
		// 一覧表の場合、ソート順を必ず設定し、昇順／降順を明記すること。
		validList.stream()
				.filter(item -> item.getGroupName().indexOf(SCREEN_ITEM_DESCRIPTION_SHEET.STR_SORT_SCOPE_TITLE) > 0)
				.findFirst().ifPresent(item -> {
					if (item.getItems().stream().allMatch(i -> i.getSorted() == null
							|| (i.getSorted().indexOf(SCREEN_ITEM_DESCRIPTION_SHEET.STR_SORT_ASC)) < 0
									&& i.getSorted().indexOf(SCREEN_ITEM_DESCRIPTION_SHEET.STR_SORT_DESC) < 0)) {
						// 一覧表の場合、ソート順の設定が必須
						errors.add(errorPrex + CommonConstant.MESSAGE_KUGIRI
								+ MessageService.getMessage("error.screen.item.description.sort"));
					}
				});
	}

}