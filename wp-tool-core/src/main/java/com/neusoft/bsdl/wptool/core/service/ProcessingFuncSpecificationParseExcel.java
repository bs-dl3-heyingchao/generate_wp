package com.neusoft.bsdl.wptool.core.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.util.StringUtils;
import com.neusoft.bsdl.wptool.core.CommonConstant.SESSION_MANAGEMENT_SHEET;
import com.neusoft.bsdl.wptool.core.io.FileSource;
import com.neusoft.bsdl.wptool.core.model.ProcessingFuncSpecification;
import com.neusoft.bsdl.wptool.core.model.ProcessingFuncSpecificationParam;

import lombok.extern.slf4j.Slf4j;

/**
 * 処理機能仕様書シートから「1.パラメータ」セクションを解析するクラス。
 */
@Slf4j
public class ProcessingFuncSpecificationParseExcel {

	/**
	 * 指定されたシートから「1.パラメータ」セクションを解析し、 順序（C列）と項目（E列）のマッピングを取得します。
	 *
	 * @param source    Excelファイルソース
	 * @param sheetName 解析対象シート名
	 * @return 解析結果（順序 → 項目 のマップ）
	 * @throws Exception 入出力エラーなど
	 */
	public ProcessingFuncSpecification parseSpecSheet(FileSource source, String sheetName)
			throws Exception {
		// 「順（C列）→ 項目（E列）」のマッピングを格納
		List<ProcessingFuncSpecificationParam> params = new ArrayList<>();

		// EasyExcelでシートを読み込み、各行をMap<Integer, String>形式で処理
		EasyExcel.read(source.getInputStream(), null, new AnalysisEventListener<Map<Integer, String>>() {
			// 状態管理フラグ:
			// 0: 「1.パラメータ」セクション未発見
			// 1: セクション発見済み → 次の行はヘッダ行（スキップ対象）
			// 2: パラメータデータ読み取り中
			// 3: 終了（空行または「2.」以降のセクションに到達）
			int step = 0;

			@Override
			public void invoke(Map<Integer, String> row, AnalysisContext context) {
				// 状態が「終了」なら以降の行は無視
				if (step == 3) {
					return;
				}

				// C列（インデックス2）: 順
				String colC = row.get(SESSION_MANAGEMENT_SHEET.COL_C);
				// E列（インデックス4）: 項目
				String colE = row.get(SESSION_MANAGEMENT_SHEET.COL_E);

				switch (step) {
				case 0:
					// 「1.」で始まるセルを探して、セクション開始を検出
					for (String cell : row.values()) {
						if (!StringUtils.isEmpty(cell) && cell.trim().startsWith("1.")) {
							step = 1; // 次の行はヘッダ行（スキップ）
							return;
						}
					}
					break;

				case 1:
					// ヘッダ行（例: 「順」「項目」）をスキップ
					step = 2; // 次の行からパラメータデータとして読み取り開始
					break;

				case 2:
					// 終了条件:
					// - C列とE列が両方空（空行）
					String colValue = row.get(SESSION_MANAGEMENT_SHEET.COL_B);
					if (!StringUtils.isEmpty(colValue) && colValue.startsWith("2.")) {
						step = 3; // 読み取り終了
						return;
					}

					// C列が空でなければ、パラメータ行としてマップに追加
					// ※ 数値チェックは行わず、文字列としてそのまま格納
					if (!StringUtils.isEmpty(colC)) {
						ProcessingFuncSpecificationParam param =new ProcessingFuncSpecificationParam();
						param.setSort(colC);
						param.setLogicName(colE);
						params.add(param);
					}
					break;
				}
			}

			@Override
			public void doAfterAllAnalysed(AnalysisContext context) {
				log.info("【処理機能仕様】パラメータ解析完了。{} 件取得。", params.size());
			}
		}).sheet(sheetName).headRowNumber(5).doRead();

		// 解析結果をBeanに設定して返却
		ProcessingFuncSpecification spec = new ProcessingFuncSpecification();
		spec.setParams(params);
		return spec;
	}
}