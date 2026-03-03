package cbai.tool.sr.bean;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class SRクエリー定義書Bean implements Serializable {

	private static final long serialVersionUID = 1L;
	public String データモデル;
	public String クエリー名;
	public List<SRクエリー定義書ItemBean> listクエリー定義書Item;

	@Data
	public static class SRクエリー定義書ItemBean implements Serializable {
		private static final long serialVersionUID = 1L;
		public String 項番;
		public String 名称;
		public String データ型;
		public String 長さ;
		public String テーブル名;
		public String カラム名;
		public String 備考;
	}

}
