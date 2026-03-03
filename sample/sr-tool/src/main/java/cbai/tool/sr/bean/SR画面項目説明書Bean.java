package cbai.tool.sr.bean;

import java.io.Serializable;

import lombok.Data;

@Data
public class SR画面項目説明書Bean implements Serializable {

	private static final long serialVersionUID = 1L;
	public String 項番;
	public String 項目名;
	public String IO;
	public String 表示;
	public String 属性;
	public String 桁数;
	public String 必須;
	public String ソート順;
	public String テーブル名;
	public String テーブル項目名;
	public String 初期値;
	public String 加工式;
	public String 選択リスト;
	public String 表示条件;
	public String 備考;

}
