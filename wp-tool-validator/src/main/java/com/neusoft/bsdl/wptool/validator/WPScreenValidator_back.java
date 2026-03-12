package com.neusoft.bsdl.wptool.validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.collections.CollectionUtils;

import com.neusoft.bsdl.wptool.core.context.WPValidatorContext;
import com.neusoft.bsdl.wptool.core.exception.WPCheckException;
import com.neusoft.bsdl.wptool.core.model.DBConfigDefinition;
import com.neusoft.bsdl.wptool.core.model.ExcelSheetContent;
import com.neusoft.bsdl.wptool.core.model.ScreenExcelContent;
import com.neusoft.bsdl.wptool.core.model.ScreenItemDescriptionResult;
import com.neusoft.bsdl.wptool.core.model.ScreenValidation;
import com.neusoft.bsdl.wptool.core.service.impl.WPTableSearchService;
import com.neusoft.bsdl.wptool.validator.CommonConstant.SCREEN_ITEM_DESCRIPTION_SHEET;
import com.neusoft.bsdl.wptool.validator.enums.ItemDescriptionIOEnum;
import com.neusoft.bsdl.wptool.validator.service.impl.MessageService;

import cbai.util.db.define.FieldBean;
import cbai.util.db.define.TableBean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WPScreenValidator_back {
	private WPValidatorContext context;

	public WPScreenValidator_back(WPValidatorContext context) {
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
		for (ExcelSheetContent<?> sheet : screenExcelContent.getSheetList()) {
			String sheetName = sheet.getSheetName();
			if (CommonConstant.PARSE_SHEET_NAME.SCREEN_DEFINITION_SHEET.equals(sheetName)) {
				// 画面定義書
			} else if (CommonConstant.PARSE_SHEET_NAME.SCREEN_ITEM_DESCRIPTION_SHEET.equals(sheetName)) {
				// 画面項目説明書
				List<ScreenItemDescriptionResult> validList = (List<ScreenItemDescriptionResult>) sheet.getContent();
				validateScreenItemDescription(validList, errors);
			} else if (CommonConstant.PARSE_SHEET_NAME.SCREEN_VALIDATION_SHEET.equals(sheetName)) {
				// 画面バリデーション定義書
				List<ScreenValidation> validList = (List<ScreenValidation>) sheet.getContent();

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

	/**
	 * 画面項目説明書のバリデーションチェックを実施する
	 * 
	 * @param validList
	 * @param errors
	 */
	private void validateScreenItemDescription(List<ScreenItemDescriptionResult> validList, List<String> errors) {
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
				// I/O列の値はIO入出力、O出力項目の場合
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
										&& (!item.getAttribute().equals(item.getAttributeWP())
												|| !item.getLength().equals(item.getLengthWP()))))) {
					errors.add(MessageService.getMessage("error.screen.item.description.wp").replace("{0}",
							item.getItemNo()));
				}

				// [属性／桁数]
				// I/O列の値はAアクション、Gグループの項目の場合
				if ((ItemDescriptionIOEnum.ACTION.getDisplayName().equals(item.getIo())
						|| ItemDescriptionIOEnum.GROUP.getDisplayName().equals(item.getIo()))
						// 「-」を記載していること。
						&& (!SCREEN_ITEM_DESCRIPTION_SHEET.STR_HAIHUN.equals(item.getAttribute())
								|| !SCREEN_ITEM_DESCRIPTION_SHEET.STR_HAIHUN.equals(item.getLength()))) {
					errors.add(MessageService.getMessage("error.screen.item.description.attr.haihun").replace("{0}",
							item.getItemNo()));
				}

				// TODO:I/O列の値はIO入出力、O出力項目の場合、
				if ((ItemDescriptionIOEnum.IO.getDisplayName().equals(item.getIo())
						|| ItemDescriptionIOEnum.OUTPUT.getDisplayName().equals(item.getIo()))) {
					// テーブル定義書の該当項目と同じ属性・桁数を記載していること。
					WPTableSearchService service = (WPTableSearchService) context.getTableSearchService();
					// データモデル名でテーブル定義書のコンテンツを取得する
					TableBean ｔableContent = service.findTableByFullName(item.getModelName());
					// テーブル定義書のコンテンツを取得する
					if (!Objects.isNull(ｔableContent)) {
						// テーブル定義書の該当項目と同じ属性・桁数を記載していること。
						Optional<FieldBean> optional = ｔableContent.getFieldList().stream()
								.filter(field -> field.getFieldFullName().equals(item.getModelItemName())).findFirst();
						if (optional.isPresent()) {
							FieldBean field = optional.get();
							// TODO:
							if ((item.getAttribute() != null && !item.getAttribute()
									.equals(field.getOthers().get(SCREEN_ITEM_DESCRIPTION_SHEET.STR_WP_TYPE)))
									|| (item.getLength() != null
											&& !item.getLength().equals(String.valueOf(field.getLen())))) {
								errors.add(MessageService.getMessage("error.screen.item.description.attr.table")
										.replace("{0}", item.getItemNo())
										.replace("{1}",
												field.getOthers().get(SCREEN_ITEM_DESCRIPTION_SHEET.STR_WP_TYPE))
										.replace("{2}", field.getLen()));
							}
						} else {
							// テーブル定義書の該当項目が存在しない場合、エラーとする
							errors.add(MessageService.getMessage("error.screen.item.description.field.notExists")
									.replace("{0}", item.getModelItemName()).replace("{1}", item.getModelName())
									.replace("{2}", item.getItemNo()));
						}
					} else {
						// TODO: データモデル名でテーブル定義書のコンテンツが取得できない場合、エラーとする
					}
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
					if (Objects.isNull(item.getSelectList()) || item.getSelectList().trim().isEmpty()) {
						errors.add(MessageService.getMessage("error.screen.item.description.selectlist").replace("{0}",
								item.getItemNo()));
					}
					// TODO:フォーマットチェック

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
