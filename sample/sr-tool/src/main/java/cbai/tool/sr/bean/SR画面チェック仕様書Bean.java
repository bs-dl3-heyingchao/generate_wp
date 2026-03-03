package cbai.tool.sr.bean;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class SR画面チェック仕様書Bean implements Serializable {

	private static final long serialVersionUID = 1L;
	public Map<String,List<SRチェックItemBean>> listチェックItemMap;

	@Data
	public static class SRチェックItemBean implements Serializable {
		private static final long serialVersionUID = 1L;
		public String 項番;
		public String チェック名;
		public String 仕様説明;
		public String チェックアクション;
		public String メッセージID;
		public String メッセージ内容;
	}

}
