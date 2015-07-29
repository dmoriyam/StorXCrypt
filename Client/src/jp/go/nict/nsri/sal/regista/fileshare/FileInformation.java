package jp.go.nict.nsri.sal.regista.fileshare;

import java.io.File;
import java.util.Locale;


public class FileInformation implements Comparable<FileInformation> {

	private File file;

	/**
	 * コンストラクタ。
	 * @param file ファイル
	 */
	public FileInformation(File file) {
		this.file = file;
	}
	
	@Override
	public int compareTo(FileInformation another) {
		if(this.file.isDirectory() && ! another.file.isDirectory()) {
			return -1;
		} else if(! this.file.isDirectory() && another.file.isDirectory()) {
			return 1;
		} else {
			return this.file.getName().toLowerCase(Locale.US).compareTo(another.file.getName().toLowerCase(Locale.US));
		}
	}
	
	
	public File getFile() {
		return this.file;
	}
	
	
}
