package jp.go.nict.nsri.sal.regista.fileshare;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class DownloadFiles extends FragmentActivity implements OnFileSelectedListener, View.OnClickListener{

	private ProgressDialog progressDialog;
	private ProgressDialog progressDialogper;
	Handler	 mHandler;
	
	String useriddat = "dat_userid.dat";
	String prikeypem = "prikey.pem";
	String sigkeypem = "sigkey.pem";
	String idverpem = "idver.pem";

	int port_no = 5111;
	String hostName = "202.16.211.113";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.filedownload);
		
		TextView tv = (TextView)findViewById(R.id.dl_title);
		tv.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(DownloadFiles.this, DemoDownload.class);
				startActivity(intent);
			}
		});
		
		//�ʒm�֌W
		NotificationManager notificationManager = (NotificationManager)getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancelAll();

		mHandler   = new Handler();

		progressDialog = new ProgressDialog(this);
		progressDialog.setTitle("File transfer");
		progressDialog.setMessage("Please wait...");
		progressDialog.setIndeterminate(true);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setCancelable(false);

		//�p�[�Z���g����
		progressDialogper = new ProgressDialog(this);
		progressDialogper.setTitle("File transfer");
		progressDialogper.setMessage("Please wait...");
		progressDialogper.setIndeterminate(false);
		progressDialogper.setProgressStyle(progressDialogper.STYLE_HORIZONTAL);
		progressDialogper.setMax(100);	// �ő�l
		progressDialogper.incrementProgressBy(0);	// �����l
		progressDialogper.setCancelable(false);

		loadInfo();
		
		Button btPass = (Button)findViewById(R.id.button1);
		btPass.setOnClickListener(this);
		
		Button btDL = (Button)findViewById(R.id.button2);
		btDL.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				progressDialog.show();

				new Thread(new Runnable() {
					@Override
					public void run() {
						// �o�b�N�O�����h�������ɋL�q
						clickrun();
						handler.sendEmptyMessage(0);
					}
				}).start();
				
			}
		});
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		TextView directorypass = (TextView)findViewById(R.id.textView2);

		DialogFragment dialogFragment = new FileSelectDialogFragment2();
		Bundle bundle = new Bundle();
		bundle.putString(FileSelectDialogFragment2.ROOT_DIRECTORY, "/");

		File file = new File(directorypass.getText().toString());
		if (file.exists()){

			String[] strAry = directorypass.getText().toString().split("/");
			String directory = "";
			for(int i=0;i<strAry.length;i++){
				directory += strAry[i];
				if(i<strAry.length-1){
					directory+="/";
				}
			}
			bundle.putString(FileSelectDialogFragment2.INITIAL_DIRECTORY, directory);
		}else{
			//�t�H���_���Ȃ��ꍇ
			bundle.putString(FileSelectDialogFragment2.INITIAL_DIRECTORY, Environment.getExternalStorageDirectory().getPath());
		}

		bundle.putString(FileSelectDialogFragment2.PREVIOUS, "..");
		bundle.putString(FileSelectDialogFragment2.CANCEL, "Cancel");
		bundle.putString(FileSelectDialogFragment2.OK, "Select");
		bundle.putSerializable(FileSelectDialogFragment2.LISTENER, this);
		dialogFragment.setArguments(bundle);
		dialogFragment.setCancelable(false);
		dialogFragment.show(this.getSupportFragmentManager(), "dialog");
		
	}

	
	protected void showToast() {
		// TODO Auto-generated method stub
		Toast.makeText(this, "Tapped", Toast.LENGTH_LONG).show();
	}

	private void loadInfo() {
		TextView idtext = (TextView)findViewById(R.id.editText_userID);
		TextView dpasstext = (TextView)findViewById(R.id.textView2);

		String idreadstr=Idread();
		if(idreadstr==""){
			idtext.setText("no data");
		}else{
			idtext.setText(idreadstr);
		}

		dpasstext.setText(Environment.getExternalStorageDirectory().getPath()+"/Download/");

	}
	
	private String Idread(){
		String strid="";
		try {
			InputStream in = openFileInput(useriddat);
			BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			strid = br.readLine();
			in.close();
			br.close();
		} catch (FileNotFoundException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (IOException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		}
		return strid;
	}

	@Override
	public void onFileSelected(String path) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onFileSelectCanceled() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onFileSelectOk(String path) {
		// TODO Auto-generated method stub
		
	}
	
	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
				//�I���������L�q
			if(msg.what==0){
				progressDialog.dismiss();
			}else if(msg.what==1){
				progressDialogper.dismiss();
			}
		}
	};
	
	private void clickrun(){
		TextView filepasst = (TextView)findViewById(R.id.textView2);
		setTextAsync("",3);

		//�����Ńt�H���_�����邩�ǂ������ׂ�B�Ȃ�������쐬����
		File file = new File(filepasst.getText().toString());
		if (file.exists()==false){
			file.mkdir();
		}

		if(prikeyread(prikeypem)!=null){
			//�Í������邩���Ȃ����̏ꍇ�킯
			String verf = connectd();
			if(verf.matches("-1")){
				setTextAsync("not connect",2);
			}else if(verf.matches("1")){
				setTextAsync("not data",2);
			}else if(verf.matches("2")){
				setTextAsync("verify not",2);
			}else{
				setTextAsync("",2);
				downloadfile();
			}
			
		}else{
			setTextAsync("not key",2);
		}
	}

	private void setTextAsync(final String text,final int flag){
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				TextView fileinfotext = (TextView)findViewById(R.id.textView3);
				TextView status = (TextView)findViewById(R.id.textView4);
				TextView filepasst = (TextView)findViewById(R.id.textView2);

				switch (flag) {
					case 1:
						fileinfotext.setText(text);
						break;
					case 2:
						status.setText("Error: "+text);
						break;
					case 3:
						status.setText("Success: "+text);
						break;
					case 4:
						final File file = new File(filepasst.getText().toString()+text);
						String setmes ="Do you want to open this file?\n  "+text;
						new AlertDialog.Builder(DownloadFiles.this)
						.setTitle("Confirmation")
						.setMessage(setmes)
						.setNegativeButton("No", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								// ������NO�̏���
							}
						})
						.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								// ������YES�̏���
								Intent intent = new Intent(Intent.ACTION_VIEW);
								String extention = MimeTypeMap.getFileExtensionFromUrl(text);
								String mimetype =MimeTypeMap.getSingleton().getMimeTypeFromExtension(extention);
								Log.d("testtag", "mimetype : "+mimetype);
								if(mimetype!=null){
									intent.setDataAndType(Uri.parse("file://"+file.getPath()), mimetype);
									startActivity(intent);
								}else{
									Toast.makeText(DownloadFiles.this, "not file open", Toast.LENGTH_SHORT).show();
								}
							}
						})
						.setCancelable(false)
						.show();

						break;
				}

			}
		});

	}

	private PrivateKey prikeyread(String filename){
		FileInputStream fin;
		String str64 = "";

		try {
			fin = openFileInput(filename);

			BufferedReader reader = new BufferedReader( new InputStreamReader(fin , "UTF-8"));

			String tmp2 = "";
			String tmp = reader.readLine();

			while((tmp = reader.readLine()) != null){
				str64 += tmp2;
				tmp2 = tmp;
			}
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (IOException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		}

		byte[] pemdec = Base64.decode(str64,Base64.DEFAULT);
		KeyFactory keyFactory;
		try {
			keyFactory = KeyFactory.getInstance("RSA");
			EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(pemdec);
			return keyFactory.generatePrivate(privateKeySpec);


		} catch (NoSuchAlgorithmException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		}
		return null;
	}

	private PublicKey pubkeyread(String filename){
		FileInputStream fin;
		String str64 = "";

		try {
			fin = openFileInput(filename);

			BufferedReader reader = new BufferedReader( new InputStreamReader(fin , "UTF-8"));

			String tmp2 = "";
			String tmp = reader.readLine();

			while((tmp = reader.readLine()) != null){
				str64 += tmp2;
				tmp2 = tmp;
			}
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (IOException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		}

		byte[] pemdec = Base64.decode(str64,Base64.DEFAULT);
		KeyFactory keyFactory;
		try {
			keyFactory = KeyFactory.getInstance("RSA");
			EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(pemdec);
			return keyFactory.generatePublic(publicKeySpec);


		} catch (NoSuchAlgorithmException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		}

		return null;
	}

	private String connectd(){
		//�T�[�o�ɗv���A���������������AOK���ǂ����܂�

		PrivateKey privateKey = prikeyread(sigkeypem);

		String verf="-1";

		Socket socket;
		InputStream is;
		OutputStream outStream;

		try {
			//�ǂ̃A�v�����炩����
			socket = new Socket(hostName, port_no);
			PrintWriter aprpw = new PrintWriter(socket.getOutputStream(),true);
			aprpw.println("3");
			socket.close();

			socket = new Socket(hostName, port_no);
			is = socket.getInputStream();
			BufferedReader sbr = new BufferedReader(new InputStreamReader(is));
			while(is.available() == 0);

			String randst = sbr.readLine();
			sbr.close();
			socket.close();

			//�������đ���
			Signature signer = Signature.getInstance("SHA256withRSA");
			signer.initSign(privateKey);
			signer.update(randst.getBytes("UTF-8"));
			byte[] sign = signer.sign();

			socket = new Socket(hostName, port_no);
			outStream = socket.getOutputStream();
			outStream.write(sign);
			socket.close();

			//������ID����
		    String myid = Idread();
		    socket = new Socket(hostName, port_no);
		    PrintWriter idpw = new PrintWriter(socket.getOutputStream(),true);
            idpw.println(myid);
		    socket.close();


			socket = new Socket(hostName, port_no);
			is = socket.getInputStream();
			sbr = new BufferedReader(new InputStreamReader(is));
			while(is.available() == 0);
			verf = sbr.readLine();
			sbr.close();
			socket.close();

		} catch (UnknownHostException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (IOException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (SignatureException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		}

		return verf;
	}

	private void downloadfile(){
		//�t�@�C���_�E�����[�h�A���ؕ���

		byte[] enckey = new byte[256];
		byte[] sigkey = new byte[256];
		byte[] enciv = new byte[256];
		byte[] sigiv = new byte[256];

		String sortime;

		Socket socket;
		InputStream is;

		PublicKey verKey;
		PrivateKey priKey = prikeyread(prikeypem);

		try {
			socket = new Socket(hostName, port_no);
			is = socket.getInputStream();
			BufferedReader sbr = new BufferedReader(new InputStreamReader(is));
			while(is.available() == 0);

			int roopf = Integer.parseInt(sbr.readLine());
			sbr.close();
			socket.close();

			byte[] filenc0 = new byte[1024];
			int blen0;
			String[] dlfilename = new String[roopf];
//			int dialogf=0;
			String sourceid;
			int safenum=0;//�Ō�A���؂ł��Ȃ������t�@�C������r������Ƃ��Ɏg�p
			for(int i=0;i<roopf;i++){
				verkeydl();

				socket = new Socket(hostName, port_no);
				is = socket.getInputStream();
				sbr = new BufferedReader(new InputStreamReader(is));
				while(is.available() == 0);
				sourceid = sbr.readLine();
				sbr.close();
				socket.close();

				socket = new Socket(hostName, port_no);
				is = socket.getInputStream();
				while(is.available() == 0);
				blen0 =is.read(filenc0);
				socket.close();

				byte[] filenc = new byte[blen0];
				for(int j=0;j<blen0;j++){
					filenc[j]=filenc0[j];
				}

				socket = new Socket(hostName, port_no);
				is = socket.getInputStream();
				while(is.available() == 0);
				is.read(enckey);
				socket.close();

				socket = new Socket(hostName, port_no);
				is = socket.getInputStream();
				while(is.available() == 0);
				is.read(sigkey);
				socket.close();

				socket = new Socket(hostName, port_no);
				is = socket.getInputStream();
				while(is.available() == 0);
				is.read(enciv);
				socket.close();

				socket = new Socket(hostName, port_no);
				is = socket.getInputStream();
				while(is.available() == 0);
				is.read(sigiv);
				socket.close();

				//�t�@�C���ۑ����Ԃ̎擾
				socket = new Socket(hostName, port_no);
				is = socket.getInputStream();
				sbr = new BufferedReader(new InputStreamReader(is));
				while(is.available() == 0);
				sortime = sbr.readLine();
				sbr.close();
				socket.close();


				verKey = pubkeyread(idverpem);

				Signature signer = Signature.getInstance("SHA256withRSA");
				signer.initVerify(verKey);
				signer.update(enckey);
				if(signer.verify(sigkey)){
					signer.update(enciv);
					if(signer.verify(sigiv)){

						Cipher aescipher = AesCipherCheck(enckey,enciv);

						if(aescipher!=null){

							byte[] filenamebyte = aescipher.doFinal(filenc);
							dlfilename[i] = new String(filenamebyte, "UTF-8");
//							dlfilename[i]="<font size=\"+2\">"+dlfilename[i]+"</font>";
							dlfilename[i] += "\n    time:"+sortime+"        ID:"+sourceid;
							Log.d("testtag", dlfilename[i]);

							safenum++;
						}else{
							//�������s�̏����A���̒[�����������X�V������
							dlfilename[i]="error not file";
						}

					}else{
						//���؎��s�̏����A�Í��������������X�V������
						dlfilename[i]="error not file";
					}
				}else{
					//���؎��s�̏����A�Í��������������X�V�������A���Ɠ���
					dlfilename[i]="error not file";
				}
//				dialogf=1;
			}
			if(safenum!=0){
				handler.sendEmptyMessage(0);
//				setDialog(dlfilename);

				String[] dlfilenamesafe = new String[safenum];
				int num=0;
				for(int i=0;i<dlfilename.length;i++){
					if(dlfilename[i]!="error not file"){
						dlfilenamesafe[num] = dlfilename[i];
						num++;
					}
				}
				setDialog(dlfilenamesafe,dlfilename);

			}else{
				setTextAsync("not data",2);
			}



		} catch (UnknownHostException e1) {
			// TODO �����������ꂽ catch �u���b�N
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO �����������ꂽ catch �u���b�N
			e1.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (SignatureException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		}

	}

	public Cipher AesCipherCheck(byte[] enckey,byte[] enciv){
		PrivateKey priKey = prikeyread(prikeypem);

		Cipher cipher;
		Cipher aescipher = null;
		try {
			cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.DECRYPT_MODE, priKey);
			byte[] aeskey = cipher.doFinal(enckey);
			byte[] aesiv = cipher.doFinal(enciv);
			Log.d("testtag", "aes : "+aeskey[0]);
			Log.d("testtag", "aes : "+aesiv[0]);

			SecretKey cipherKey	   = new SecretKeySpec(aeskey, "AES");
			IvParameterSpec ivSpec = new IvParameterSpec(aesiv);

			aescipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			aescipher.init(Cipher.DECRYPT_MODE, cipherKey, ivSpec);
		} catch (NoSuchAlgorithmException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		}

		return aescipher;

	}
	
	private void verkeydl(){
		//���،��_�E�����[�h�̂�

		Socket socket;
		InputStream is;

		byte[] byteBuffer = new byte[32];
		int recvMsgSize;

		try {
			//���M���̌��،��ۑ�
			FileOutputStream fout = openFileOutput(idverpem, MODE_PRIVATE);

			socket = new Socket(hostName, port_no);
			is = socket.getInputStream();
			while(is.available() == 0);

			while((recvMsgSize = is.read(byteBuffer)) != -1){
				fout.write(byteBuffer,0,recvMsgSize);
			}
			socket.close();

		} catch (UnknownHostException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (IOException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		}

	}
	
	private void setDialog(final String[] dlfilenamesafe,final String[] dlfilename){
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				filedialog(dlfilenamesafe,dlfilename);
			}
		});
	}
	
	private void filedialog(final String[] dlfilenamesafe,final String[] dlfilename){
		//�t�@�C�������擾���Ă�����_�C�A���O�ɕ\���A�I���������̂�DL
		new AlertDialog.Builder(DownloadFiles.this)
	    .setTitle("FileSelect")
	    .setItems(dlfilenamesafe, new DialogInterface.OnClickListener(){
	        public void onClick(DialogInterface dialog, final int which) {
//	        	Log.d("testtag", which+"");
	        	//�����ł܂��_�C�A���O��\��

	        	progressDialogper.show();
	        	progressDialogper.setProgress(0);

				new Thread(new Runnable() {
					@Override
					public void run() {
						// �o�b�N�O�����h�������ɋL�q
//						long timesall = System.currentTimeMillis();/////���Ԍv���X�^�[�g
						for(int i=0;i<dlfilename.length;i++){
							if(dlfilenamesafe[which]==dlfilename[i]){
								downloadfilemain(i,dlfilenamesafe[which]);
								break;
							}
						}

//						downloadfilemain(which,filename[which]);
//						long timeeall = System.currentTimeMillis();/////���Ԍv���G���h
//						long totaltimeall = timeeall - timesall;//���Ԍv��
//						setTextAsync("Total : "+totaltimeall + "ms",3);
						handler.sendEmptyMessage(1);
					}
				}).start();

	        }
	    })
	    .setNeutralButton ("Cancel", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) {
//	        	Log.d("testtag", "cancel");
	        }
	    })
	    .setCancelable(false)
	    .show();

	}
	
	private void downloadfilemain(int filewhich,String filename){
		TextView filepasst = (TextView)findViewById(R.id.textView2);
		progressDialogper.setProgress(0);
		//�t�@�C���_�E�����[�h�A���ؕ���

		byte[] enckey = new byte[256];
		byte[] sigkey = new byte[256];
		byte[] enciv = new byte[256];
		byte[] sigiv = new byte[256];

		Socket socket;
		InputStream is;

		long times;
		long timee;
		long ltime1=0;
		long ltime2=0;

		PublicKey verKey;
		PrivateKey priKey = prikeyread(prikeypem);


		try {
			//�A�v���t���O
			socket = new Socket(hostName, port_no);
			PrintWriter aprpw = new PrintWriter(socket.getOutputStream(),true);
	        aprpw.println("4");
			socket.close();

			socket = new Socket(hostName, port_no);
			aprpw = new PrintWriter(socket.getOutputStream(),true);
	        aprpw.println(filewhich);
			socket.close();

			//�ȉ��_�E�����[�h
//			times = System.currentTimeMillis();/////���Ԍv���X�^�[�g
			verkeydl();

			socket = new Socket(hostName, port_no);
			is = socket.getInputStream();
			while(is.available() == 0);
			is.read(enckey);
			socket.close();

			socket = new Socket(hostName, port_no);
			is = socket.getInputStream();
			while(is.available() == 0);
			is.read(sigkey);
			socket.close();

			socket = new Socket(hostName, port_no);
			is = socket.getInputStream();
			while(is.available() == 0);
			is.read(enciv);
			socket.close();

			socket = new Socket(hostName, port_no);
			is = socket.getInputStream();
			while(is.available() == 0);
			is.read(sigiv);
			socket.close();
//			timee = System.currentTimeMillis();/////���Ԍv���G���h
//			totaltrat += timee - times;//���Ԍv��

			times = System.currentTimeMillis();/////���Ԍv���X�^�[�g
			verKey = pubkeyread(idverpem);

			Signature signer = Signature.getInstance("SHA256withRSA");
			signer.initVerify(verKey);
			signer.update(enckey);
			if(signer.verify(sigkey)){
				signer.update(enciv);
				if(signer.verify(sigiv)){
					timee = System.currentTimeMillis();/////���Ԍv���G���h
					ltime1 += timee - times;//���Ԍv��
					socket = new Socket(hostName, port_no);
					aprpw = new PrintWriter(socket.getOutputStream(),true);
					aprpw.println("0");
					socket.close();

					Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
					cipher.init(Cipher.DECRYPT_MODE, priKey);
					times = System.currentTimeMillis();/////���Ԍv���X�^�[�g
					byte[] aeskey = cipher.doFinal(enckey);
					byte[] aesiv = cipher.doFinal(enciv);
					timee = System.currentTimeMillis();/////���Ԍv���G���h
					ltime1 += timee - times;//���Ԍv��

					SecretKey cipherKey	   = new SecretKeySpec(aeskey, "AES");
					IvParameterSpec ivSpec = new IvParameterSpec(aesiv);

					Cipher aescipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
					aescipher.init(Cipher.DECRYPT_MODE, cipherKey, ivSpec);
//					timee = System.currentTimeMillis();/////���Ԍv���G���h
//					totaldect += timee - times;//���Ԍv��

					String[] strAry = filename.split("\n");
					FileOutputStream fout = new FileOutputStream(filepasst.getText().toString()+strAry[0]);

					byte[] fileBuff = new byte[1024];
					int blen;

					//�t�@�C���T�C�Y���擾�A�_�C�A���O�Ƀp�[�Z���g���f
					socket = new Socket(hostName, port_no);
					is = socket.getInputStream();
					BufferedReader sbr = new BufferedReader(new InputStreamReader(is));
					while(is.available() == 0);
					long filebytelen = Long.parseLong(sbr.readLine());
					sbr.close();
					socket.close();

					//���M���v�Z
					double sendper = 1024.0/filebytelen*100;
					double sendperall=sendper;

//					times = System.currentTimeMillis();/////���Ԍv���X�^�[�g
					socket = new Socket(hostName, port_no);
					is = socket.getInputStream();
					while(is.available() == 0);
					CipherInputStream cos = new CipherInputStream(is, aescipher);
					times = System.currentTimeMillis();/////���Ԍv���X�^�[�g
					while((blen = cos.read(fileBuff)) != -1){
						progressDialogper.setProgress((int)sendperall);
						sendperall+=sendper;
						fout.write(fileBuff, 0, blen);
					}
					socket.close();
					timee = System.currentTimeMillis();/////���Ԍv���G���h
					ltime2 += timee - times;//���Ԍv��


					Log.d("CryptToolBox","FileDec for App1_1ut: Decrypt AES with RSA "+ltime1+" ms\r\n");
			    	Log.d("CryptToolBox","FileDec for App1_1ut: AES Including File Transmission time "+ltime2+" ms\r\n");

			    	setTextAsync("Success",3);

					//�t�@�C�����\��
					fileinfo(strAry[0],strAry[1].substring(39),strAry[1].substring(9,28));

				}else{
					//���؎��s�̏���
					socket = new Socket(hostName, port_no);
					aprpw = new PrintWriter(socket.getOutputStream(),true);
					aprpw.println("1");
					socket.close();
					setTextAsync("verify not",2);
				}
			}else{
				//���؎��s�̏����A���Ɠ���
				socket = new Socket(hostName, port_no);
				aprpw = new PrintWriter(socket.getOutputStream(),true);
				aprpw.println("1");
				socket.close();
				setTextAsync("verify not",2);
			}


		} catch (UnknownHostException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (IOException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (SignatureException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		}

	}

	private void fileinfo(String filename, String senderid, String uptime){
		TextView filepasst = (TextView)findViewById(R.id.textView2);

		File file = new File(filepasst.getText().toString()+filename);

		String fileinfostr="FileName:\n  "+ filename;
		fileinfostr+="\n\nSourceID:\n  "+ senderid;
		fileinfostr+="\nUpload time:\n  "+ uptime;
		fileinfostr+="\nPath:\n  "+ filepasst.getText().toString()+filename;
		fileinfostr+="\nSize:\n  "+ +file.length()+" byte";

		setTextAsync(fileinfostr,1);
//		Log.d("testtag", "last:"+senderid);

		//�t�@�C���J��
		setTextAsync(filename,4);

	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode==KeyEvent.KEYCODE_BACK){
			Intent intent = new Intent(DownloadFiles.this, MainActivity.class);
			startActivity(intent);
			return true;
		}
		return false;
	}
	
}
