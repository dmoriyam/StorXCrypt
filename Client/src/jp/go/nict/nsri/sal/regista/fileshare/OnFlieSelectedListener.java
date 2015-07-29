package jp.go.nict.nsri.sal.regista.fileshare;

import java.io.Serializable;


public interface OnFlieSelectedListener extends Serializable {

	/**
	 * �t�@�C�����I�����ꂽ���ɌĂяo����܂��B
	 * @param path �p�X
	 */
	public abstract void onFileSelected(String path);
	/**
	 * �t�@�C���I�����L�����Z�����ꂽ�Ƃ��ɌĂяo����܂��B
	 */
	public abstract void onFileSelectCanceled();

	public abstract void onFileSelectOk(String path);

}
