package jp.go.nict.nsri.sal.regista.fileshare;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

@SuppressLint("NewApi")
public class FileSelectDialogFragment extends DialogFragment implements OnItemClickListener {
	/**
	 * ���[�g�f�B���N�g����\���܂��B
	 */
	public static final String ROOT_DIRECTORY = "rootDirectory";
	/**
	 * �_�C�A���O�\������̃f�B���N�g����\���܂��B
	 */
	public static final String INITIAL_DIRECTORY = "initialDirectory";
	/**
	 * �e�f�B���N�g���̃e�L�X�g��\���܂��B
	 */
	public static final String PREVIOUS = "previous";
	/**
	 * �L�����Z���{�^���̕������\���܂��B
	 */
	public static final String CANCEL = "cancel";
	/**
	 * ���X�i��\���܂��B
	 */
	public static final String LISTENER = "listener";
	/**
	 * ���ݑI�𒆂̃f�B���N�g����\���܂��B
	 */
	private static final String DIRECTORY = "directory";
	/**
	 * �A�_�v�^�[�B
	 */
	private ArrayAdapter<String> adapter;
	/**
	 * �t�@�C�����B
	 */
	private List<FileInformation> fileInformations = new ArrayList<FileInformation>();

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Activity activity = this.getActivity();
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);

		ListView listView = new ListView(activity);
		this.adapter = new ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1);
		listView.setAdapter(this.adapter);

		this.initializeArguments();

		// �r���[���X�V����
		this.updateView();

		listView.setOnItemClickListener(this);
		builder.setView(listView);

		Bundle bundle = this.getArguments();
		builder.setTitle(bundle.getString(DIRECTORY) + File.separator);
		builder.setNegativeButton(bundle.getString(CANCEL), new CancelListener());

		return builder.create();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Dialog dialog = this.getDialog();
		Bundle bundle = this.getArguments();
		String directory = bundle.getString(DIRECTORY);

		if(position == 0) {
			// ��ԏ��I�������ꍇ
			if(directory.length() > bundle.getString(ROOT_DIRECTORY).length()) {
				// �ʏ�͖߂鏈��������
				directory = directory.substring(0, directory.lastIndexOf(File.separator));
				bundle.putString(DIRECTORY, directory);

				// �_�C�A���O�ƃr���[���X�V
				dialog.setTitle(directory + File.separator);
				this.updateView();
			} else {
				// �g�b�v�f�B���N�g���̏ꍇ�͉������Ȃ�
			}
		} else {
			directory = directory + File.separator + this.fileInformations.get(position - 1).getFile().getName();
			File file = new File(directory);
			if(file.isDirectory()) {
				// �f�B���N�g���̏ꍇ�͂��̒��ֈړ�
				bundle.putString(DIRECTORY, directory);

				// �_�C�A���O�ƃr���[���X�V
				dialog.setTitle(directory + File.separator);
				this.updateView();
			} else {
				// �t�@�C�����m��
				OnFileSelectedListener listener = (OnFileSelectedListener)this.getArguments().getSerializable(LISTENER);
				listener.onFileSelected(directory);

				// ���̃_�C�A���O���I��
				this.dismiss();
			}
		}
	}

	/**
	 * �v�f�������������܂��B
	 */
	private void initializeArguments() {
		Bundle bundle = this.getArguments();
		if(bundle.getString(ROOT_DIRECTORY) == null) {
			bundle.putString(ROOT_DIRECTORY, File.separator);
		}
		if(bundle.getString(INITIAL_DIRECTORY) == null) {
			bundle.putString(INITIAL_DIRECTORY, bundle.getString(ROOT_DIRECTORY));
		}
		if(bundle.getString(DIRECTORY) == null) {
			bundle.putString(DIRECTORY, bundle.getString(INITIAL_DIRECTORY));
		}
		if(bundle.getString(PREVIOUS) == null) {
			bundle.putString(PREVIOUS, "..");
		}
	}
	/**
	 * �r���[���X�V���܂��B
	 */
	private void updateView() {
		this.adapter.clear();

		Bundle bundle = this.getArguments();
		this.adapter.add(bundle.getString(PREVIOUS));

		String directory = bundle.getString(DIRECTORY);
		if(directory.equals("")) {
			directory = File.separator;
		}

		this.fileInformations.clear();
		File[] files = new File(directory).listFiles();

		if(files != null) {
			for(File file : files) {
				this.fileInformations.add(new FileInformation(file));
			}

			// �\�[�g
			Collections.sort(this.fileInformations);

			for(FileInformation fileInformation : this.fileInformations) {
				File file = fileInformation.getFile();
				if(file.isDirectory()) {
					this.adapter.add(file.getName() + File.separator);
				} else {
					this.adapter.add(file.getName());
				}
			}
		}
	}

	/**
	 * �L�����Z�����������郊�X�i�ł��B
	 */
	private class CancelListener implements DialogInterface.OnClickListener {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			OnFileSelectedListener listener = (OnFileSelectedListener)FileSelectDialogFragment.this.getArguments().getSerializable(LISTENER);
			listener.onFileSelectCanceled();
		}
	}
}

