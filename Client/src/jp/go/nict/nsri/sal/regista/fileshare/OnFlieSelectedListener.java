package jp.go.nict.nsri.sal.regista.fileshare;

import java.io.Serializable;


public interface OnFlieSelectedListener extends Serializable {

	/**
	 * ファイルが選択された時に呼び出されます。
	 * @param path パス
	 */
	public abstract void onFileSelected(String path);
	/**
	 * ファイル選択がキャンセルされたときに呼び出されます。
	 */
	public abstract void onFileSelectCanceled();

	public abstract void onFileSelectOk(String path);

}
