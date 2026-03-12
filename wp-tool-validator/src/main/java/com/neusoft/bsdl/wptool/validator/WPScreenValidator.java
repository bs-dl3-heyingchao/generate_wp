package com.neusoft.bsdl.wptool.validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.neusoft.bsdl.wptool.core.exception.WPCheckException;
import com.neusoft.bsdl.wptool.core.model.DBConfigDefinition;
import com.neusoft.bsdl.wptool.core.model.ExcelSheetContent;
import com.neusoft.bsdl.wptool.core.model.ScreenDefinition;
import com.neusoft.bsdl.wptool.core.model.ScreenDefinitionTargetData;
import com.neusoft.bsdl.wptool.core.model.ScreenExcelContent;
import com.neusoft.bsdl.wptool.core.model.ScreenItemDescriptionResult;
import com.neusoft.bsdl.wptool.core.model.ScreenValidation;
import com.neusoft.bsdl.wptool.validator.CommonConstant.SCREEN_ITEM_DESCRIPTION_SHEET;
import com.neusoft.bsdl.wptool.validator.CommonConstant.SCREEN_VALIDATION_SHEET;
import com.neusoft.bsdl.wptool.validator.context.WPValidatorContext;
import com.neusoft.bsdl.wptool.validator.enums.ItemDescriptionIOEnum;
import com.neusoft.bsdl.wptool.validator.model.MessageDefinition;
import com.neusoft.bsdl.wptool.validator.service.impl.MessageService;
import com.neusoft.bsdl.wptool.validator.service.impl.WPMessageLoaderService;
import com.neusoft.bsdl.wptool.validator.utils.CommonUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WPScreenValidator {
	private WPValidatorContext context;

	public WPScreenValidator(WPValidatorContext context) {
		this.context = context;
	}

	/**
	 * 仕様書の内容のバリデーションチェックを実施する
	 * 
	 * @param screenExcelContent
	 * @throws WPCheckException
	 */
	public void validateParseContent(ScreenExcelContent screenExcelContent) throws WPCheckException {
		List<String> errors = new ArrayList<>();
		// 画面定義書
		ScreenDefinition ScreenDefinitionValidObj = null;
		for (ExcelSheetContent<?> sheet : screenExcelContent.getSheetList()) {
			String sheetName = sheet.getSheetName();
			if (CommonConstant.PARSE_SHEET_NAME.SCREEN_DEFINITION_SHEET.equals(sheetName)) {
				ScreenDefinitionValidObj = (ScreenDefinition) sheet.getContent();
				// 画面定義書のバリデーションチェックを実施する
				validateScreenDefinition(ScreenDefinitionValidObj, errors);
			} else if (CommonConstant.PARSE_SHEET_NAME.SCREEN_ITEM_DESCRIPTION_SHEET.equals(sheetName)) {
				// 画面項目説明書
				List<ScreenItemDescriptionResult> validList = (List<ScreenItemDescriptionResult>) sheet.getContent();
				// 画面項目説明書のバリデーションチェックを実施する
				validateScreenItemDescription(validList, ScreenDefinitionValidObj, errors);
			} else if (CommonConstant.PARSE_SHEET_NAME.SCREEN_VALIDATION_SHEET.equals(sheetName)) {
				// 画面バリデーション定義書
				List<ScreenValidation> validList = (List<ScreenValidation>) sheet.getContent();
				// 画面バリデーション定義書のバリデーションチェックを実施する
				validateScreenValidation(validList, errors);
			} else if (CommonConstant.PARSE_SHEET_NAME.BP_SHEET.equals(sheetName)) {
				// BP定義書

			} else if (CommonConstant.PARSE_SHEET_NAME.DB_CONFIG_SHEET.equals(sheetName)) {
				// DB設定定義書
				DBConfigDefinition validObj = (DBConfigDefinition) sheet.getContent();

			}
		}
		// エラーが存在する場合、例外をスローする
		if (!CollectionUtils.isEmpty(errors)) {
			throw new WPCheckException(errors);
		}
	}

	private void validateScreenDefinition(ScreenDefinition screenDefinitionValidObj, List<String> errors) {
		// TODO Auto-generated method stub

	}

	/**
	 * 画面バリデーション定義書のバリデーションチェックを実施する
	 * 
	 * @param validList バリデーションチェック対象
	 * @param errors    エラーリスト
	 */
	private void validateScreenValidation(List<ScreenValidation> validList, List<String> errors) {
		for (ScreenValidation screenValidation : validList) {
			// [メッセージID／メッセージ内容／パラメータ]
			// 種別「エラー」の場合、
			// メッセージIDとメッセージ内容を記載していること。
			// メッセージ一覧（共通）または、メッセージ（サブ）に記載のID、メッセージと一致していること。
			// メッセージ内に{n}がある場合、パラメータを記載していること。
			if (SCREEN_VALIDATION_SHEET.STR_ERROR.equals(screenValidation.getType())) {
				if (StringUtils.isEmpty(screenValidation.getMessageId())
						|| StringUtils.isEmpty(screenValidation.getMessageContent())) {
					errors.add(MessageService.getMessage("error.screen.validation.msg.required").replace("{0}",
							screenValidation.getItemNo()));
					continue;
				}

				// メッセージIDとメッセージ内容の整合性チェック
				WPMessageLoaderService service = (WPMessageLoaderService) context.getMessageLoaderService();
				MessageDefinition messageObj = service.findMessageById(screenValidation.getMessageId());
				// メッセージIDとメッセージ内容を記載してない場合、エラーリストにエラーメッセージを追加する
				if (Objects.isNull(messageObj)) {
					errors.add(MessageService.getMessage("error.screen.validation.error.message").replace("{0}",
							screenValidation.getItemNo()));
				}
				// メッセージID:「{0}」のメッセージ内容が、メッセージ一覧の内容と一致していない場合、エラーリストにエラーメッセージを追加する
				if (!Objects.isNull(messageObj)
						&& !StringUtils.equals(messageObj.getMessageText(), screenValidation.getMessageContent())) {
					errors.add(MessageService.getMessage("error.screen.validation.error.message.mismatch")
							.replace("{0}", screenValidation.getItemNo()).replace("{1}", messageObj.getMessageText()));
				}
			}
			// TODO: メッセージ内に{n}がある場合、パラメータを記載していること。

			// [メッセージID／メッセージ内容／パラメータ]
			// 種別「ワーニング」の場合、
			// メッセージIDはなし、メッセージ内容は、別シートを参照の旨を記載していること。
			// ※記載方法は、設計書サンプルを参照。
			if (SCREEN_VALIDATION_SHEET.STR_WARNING.equals(screenValidation.getType())) {
				if (!StringUtils.isEmpty(screenValidation.getMessageId())) {
					errors.add(MessageService.getMessage("warning.screen.validation.msgId.not.required").replace("{0}",
							screenValidation.getItemNo()));
					continue;
				}
				// TODO:メッセージ内容は、別シートを参照の旨を記載していること。
			}
		}
	}

	/**
	 * 画面項目説明書のバリデーションチェックを実施する
	 * 
	 * @param validList                バリデーションチェック対象
	 * @param screenDefinitionValidObj 画面定義書の解析内容（対象データモデル情報のチェックに使用）
	 * @param errors                   エラーリスト
	 */
	private void validateScreenItemDescription(List<ScreenItemDescriptionResult> validList,
			ScreenDefinition screenDefinitionValidObj, List<String> errors) {
		for (ScreenItemDescriptionResult screenItemDescription : validList) {
			screenItemDescription.getItems().forEach(item -> {
				// 画面項目説明書の「IO」列には「I（入力）」「O（出力）」「A（アクション）」「G（グループ）」「IO（入出力）」以外の値を記載することはできません
				if (!ItemDescriptionIOEnum.getAllDisplayNames().contains(item.getIo())) {
					errors.add(MessageService.getMessage("error.screen.item.description.io").replace("{0}",
							item.getItemNo()));
				}
				// [属性(WP)／桁数(WP)]:
				// I/O列の値はAアクション、Gグループの項目の場合、「-」を記載していること。
				if ((ItemDescriptionIOEnum.ACTION.getDisplayName().equals(item.getIo())
						|| ItemDescriptionIOEnum.GROUP.getDisplayName().equals(item.getIo()))
						&& (!Arrays.asList(SCREEN_ITEM_DESCRIPTION_SHEET.ARR_HAIHUN).contains(item.getAttributeWP())
								|| !Arrays.asList(SCREEN_ITEM_DESCRIPTION_SHEET.ARR_HAIHUN)
										.contains(item.getLengthWP()))) {
					errors.add(MessageService.getMessage("error.screen.item.description.wp").replace("{0}",
							item.getItemNo()));
				}

				// [属性(WP)／桁数(WP)]:
				// TODO:I/O列の値はIO入出力、O出力項目の場合
				if ((ItemDescriptionIOEnum.IO.getDisplayName().equals(item.getIo())
						|| ItemDescriptionIOEnum.OUTPUT.getDisplayName().equals(item.getIo()))
						// 対象データモデル情報が記載されている場合、「DM」を記載していること。
						&& (((!Arrays.asList(SCREEN_ITEM_DESCRIPTION_SHEET.ARR_OUTSIDE_SCOPE)
								.contains(item.getModelName()))
								&& (!SCREEN_ITEM_DESCRIPTION_SHEET.STR_DM.equals(item.getAttributeWP())
										|| !SCREEN_ITEM_DESCRIPTION_SHEET.STR_DM.equals(item.getLengthWP())))
								// 対象データモデル情報が記載されていない場合は、属性、桁数と同じ値を記載していること。
								|| (Arrays.asList(SCREEN_ITEM_DESCRIPTION_SHEET.ARR_OUTSIDE_SCOPE)
										.contains(item.getModelName())
										&& (!StringUtils.equals(item.getAttribute(), item.getAttributeWP())
												|| StringUtils.equals(item.getLength(), item.getLengthWP()))))) {
					errors.add(MessageService.getMessage("error.screen.item.description.wp").replace("{0}",
							item.getItemNo()));
				}

				// [対象データモデル情報]
				// データモデル名は、画面定義書の対象データモデル欄に記載している論理名称を記載していること。
				// 他のデータモデル名を記載していないこと。
				// 項目名は、データモデルに存在する項目を記載していること。
				Set<String> logicalNames = screenDefinitionValidObj.getTargetModels().stream()
						.map(ScreenDefinitionTargetData::getLogicalName).filter(Objects::nonNull)
						.collect(Collectors.toSet());
				if (!Arrays.asList(SCREEN_ITEM_DESCRIPTION_SHEET.ARR_OUTSIDE_SCOPE).contains(item.getModelName())
						&& !logicalNames.contains(item.getModelName())) {
					errors.add(MessageService.getMessage("error.screen.item.description.modelName.exits")
							.replace("{0}", item.getModelName()).replace("{1}", item.getItemNo()));
				}

				// [フォーマット]
				// TODO:数値項目、日付項目の場合（非表示項目は除く）、フォーマットを記載していること。
				if (!SCREEN_ITEM_DESCRIPTION_SHEET.STR_NO_DISPLAY.equals(item.getDisplay())
						&& Arrays.asList(SCREEN_ITEM_DESCRIPTION_SHEET.ARR_DATA_TYPE).contains(item.getAttribute())
						&& (Objects.isNull(item.getFormat()) || item.getFormat().isEmpty())) {
					errors.add(MessageService.getMessage("error.screen.item.description.format").replace("{0}",
							item.getItemNo()));
				}

				// [選択リスト]
				// ドロップダウン形式/ラジオボタン形式/複数選択チェックボックス形式の項目の場合、リストの作成内容を記載していること
				if (item.getItemName().indexOf(SCREEN_ITEM_DESCRIPTION_SHEET.STR_DROPDOWN) > 0
						|| item.getItemName().indexOf(SCREEN_ITEM_DESCRIPTION_SHEET.STR_RADIO_BUTTON) > 0
						|| item.getItemName().indexOf(SCREEN_ITEM_DESCRIPTION_SHEET.STR_DUPLICATE_CHECKBOX) > 0) {
					// 選択リストの内容は空白の場合、エラーメッセージを出力する
					if (StringUtils.isEmpty(item.getSelectList())) {
						errors.add(MessageService.getMessage("error.screen.item.description.selectlist").replace("{0}",
								item.getItemNo()));
					}
					// フォーマットチェック
					if (!StringUtils.isEmpty(item.getSelectList())
							&& !CommonUtils.containsAllRequiredTags(item.getSelectList(),
									CommonConstant.SCREEN_ITEM_DESCRIPTION_SHEET.ARR_SELECT_LIST)) {
						errors.add(MessageService.getMessage("error.screen.item.description.selectlist.format")
								.replace("{0}", item.getItemNo()));
					}
				}
			});
		}
		// [ソート順]
		// 一覧の場合、必ずソート順を設定していること。順番と昇順／降順を記載していること。
		validList.stream()
				.filter(item -> item.getGroupName().indexOf(SCREEN_ITEM_DESCRIPTION_SHEET.STR_SORT_SCOPE_TITLE) > 0)
				.findFirst().ifPresent(item -> {
					if (item.getItems().stream().allMatch(i -> i.getSorted() == null
							|| (i.getSorted().indexOf(SCREEN_ITEM_DESCRIPTION_SHEET.STR_SORT_ASC)) < 0
									&& i.getSorted().indexOf(SCREEN_ITEM_DESCRIPTION_SHEET.STR_SORT_DESC) < 0)) {
						// 一覧の場合、必ずソート順を設定していること。順番と昇順／降順を記載していること。
						errors.add(MessageService.getMessage("error.screen.item.description.sort"));
					}
				});
	}

}
