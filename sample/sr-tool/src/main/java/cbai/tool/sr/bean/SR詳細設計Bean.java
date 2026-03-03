package cbai.tool.sr.bean;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class SR詳細設計Bean implements Serializable {

	private static final long serialVersionUID = 1L;
	public String 画面ID;
	public String 画面名;
	public List<SR画面項目説明書Bean> list画面項目説明書 = null;
	public List<SR画面項目説明書Bean> list画面項目説明書CSV = null;
	public List<SRクエリー定義書Bean> listクエリー定義書 = null;
	public SR対象条件Bean 対象条件;
	public SR画面チェック仕様書Bean 画面チェック仕様書;

}
