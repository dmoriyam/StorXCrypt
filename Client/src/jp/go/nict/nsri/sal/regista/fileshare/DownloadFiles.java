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
		
		//通知関係
		NotificationManager notificationManager = (NotificationManager)getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancelAll();

		mHandler   = new Handler();

		progressDialog = new ProgressDialog(this);
		progressDialog.setTitle("File transfer");
		progressDialog.setMessage("Please wait...");
		progressDialog.setIndeterminate(true);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setCancelable(false);

		//パーセントあり
		progressDialogper = new ProgressDialog(this);
		progressDialogper.setTitle("File transfer");
		progressDialogper.setMessage("Please wait...");
		progressDialogper.setIndeterminate(false);
		progressDialogper.setProgressStyle(progressDialogper.STYLE_HORIZONTAL);
		progressDialogper.setMax(100);	// 最大値
		progressDialogper.incrementProgressBy(0);	// 初期値
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
						// バックグランドをここに記述
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
			//フォルダがない場合
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
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
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
				//終了処理を記述
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

		//ここでフォルダがあるかどうか調べる。なかったら作成する
		File file = new File(filepasst.getText().toString());
		if (file.exists()==false){
			file.mkdir();
		}

		if(prikeyread(prikeypem)!=null){
			//暗号化するかしないかの場合わけ
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
								// ここにNOの処理
							}
						})
						.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								// ここにYESの処理
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
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		byte[] pemdec = Base64.decode(str64,Base64.DEFAULT);
		KeyFactory keyFactory;
		try {
			keyFactory = KeyFactory.getInstance("RSA");
			EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(pemdec);
			return keyFactory.generatePrivate(privateKeySpec);


		} catch (NoSuchAlgorithmException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			// TODO 自動生成された catch ブロック
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
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		byte[] pemdec = Base64.decode(str64,Base64.DEFAULT);
		KeyFactory keyFactory;
		try {
			keyFactory = KeyFactory.getInstance("RSA");
			EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(pemdec);
			return keyFactory.generatePublic(publicKeySpec);


		} catch (NoSuchAlgorithmException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		return null;
	}

	private String connectd(){
		//サーバに要求、来たやつを署名送る、OKかどうかまで

		PrivateKey privateKey = prikeyread(sigkeypem);

		String verf="-1";

		Socket socket;
		InputStream is;
		OutputStream outStream;

		try {
			//どのアプリからか判別
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

			//署名して送る
			Signature signer = Signature.getInstance("SHA256withRSA");
			signer.initSign(privateKey);
			signer.update(randst.getBytes("UTF-8"));
			byte[] sign = signer.sign();

			socket = new Socket(hostName, port_no);
			outStream = socket.getOutputStream();
			outStream.write(sign);
			socket.close();

			//自分のID送る
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
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (SignatureException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		return verf;
	}

	private void downloadfile(){
		//ファイルダウンロード、検証復元

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
			int safenum=0;//最後、検証できなかったファイル名を排除するときに使用
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

				//ファイル保存時間の取得
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
							//復号失敗の処理、この端末側が鍵を更新した時
							dlfilename[i]="error not file";
						}

					}else{
						//検証失敗の処理、暗合した側が鍵を更新した時
						dlfilename[i]="error not file";
					}
				}else{
					//検証失敗の処理、暗合した側が鍵を更新した時、↑と同じ
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
			// TODO 自動生成された catch ブロック
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO 自動生成された catch ブロック
			e1.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (SignatureException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO 自動生成された catch ブロック
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
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		return aescipher;

	}
	
	private void verkeydl(){
		//検証鍵ダウンロードのみ

		Socket socket;
		InputStream is;

		byte[] byteBuffer = new byte[32];
		int recvMsgSize;

		try {
			//送信元の検証鍵保存
			FileOutputStream fout = openFileOutput(idverpem, MODE_PRIVATE);

			socket = new Socket(hostName, port_no);
			is = socket.getInputStream();
			while(is.available() == 0);

			while((recvMsgSize = is.read(byteBuffer)) != -1){
				fout.write(byteBuffer,0,recvMsgSize);
			}
			socket.close();

		} catch (UnknownHostException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
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
		//ファイル名を取得してそれをダイアログに表示、選択したものをDL
		new AlertDialog.Builder(DownloadFiles.this)
	    .setTitle("FileSelect")
	    .setItems(dlfilenamesafe, new DialogInterface.OnClickListener(){
	        public void onClick(DialogInterface dialog, final int which) {
//	        	Log.d("testtag", which+"");
	        	//ここでまたダイアログを表示

	        	progressDialogper.show();
	        	progressDialogper.setProgress(0);

				new Thread(new Runnable() {
					@Override
					public void run() {
						// バックグランドをここに記述
//						long timesall = System.currentTimeMillis();/////時間計測スタート
						for(int i=0;i<dlfilename.length;i++){
							if(dlfilenamesafe[which]==dlfilename[i]){
								downloadfilemain(i,dlfilenamesafe[which]);
								break;
							}
						}

//						downloadfilemain(which,filename[which]);
//						long timeeall = System.currentTimeMillis();/////時間計測エンド
//						long totaltimeall = timeeall - timesall;//時間計測
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
		//ファイルダウンロード、検証復元

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
			//アプリフラグ
			socket = new Socket(hostName, port_no);
			PrintWriter aprpw = new PrintWriter(socket.getOutputStream(),true);
	        aprpw.println("4");
			socket.close();

			socket = new Socket(hostName, port_no);
			aprpw = new PrintWriter(socket.getOutputStream(),true);
	        aprpw.println(filewhich);
			socket.close();

			//以下ダウンロード
//			times = System.currentTimeMillis();/////時間計測スタート
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
//			timee = System.currentTimeMillis();/////時間計測エンド
//			totaltrat += timee - times;//時間計測

			times = System.currentTimeMillis();/////時間計測スタート
			verKey = pubkeyread(idverpem);

			Signature signer = Signature.getInstance("SHA256withRSA");
			signer.initVerify(verKey);
			signer.update(enckey);
			if(signer.verify(sigkey)){
				signer.update(enciv);
				if(signer.verify(sigiv)){
					timee = System.currentTimeMillis();/////時間計測エンド
					ltime1 += timee - times;//時間計測
					socket = new Socket(hostName, port_no);
					aprpw = new PrintWriter(socket.getOutputStream(),true);
					aprpw.println("0");
					socket.close();

					Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
					cipher.init(Cipher.DECRYPT_MODE, priKey);
					times = System.currentTimeMillis();/////時間計測スタート
					byte[] aeskey = cipher.doFinal(enckey);
					byte[] aesiv = cipher.doFinal(enciv);
					timee = System.currentTimeMillis();/////時間計測エンド
					ltime1 += timee - times;//時間計測

					SecretKey cipherKey	   = new SecretKeySpec(aeskey, "AES");
					IvParameterSpec ivSpec = new IvParameterSpec(aesiv);

					Cipher aescipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
					aescipher.init(Cipher.DECRYPT_MODE, cipherKey, ivSpec);
//					timee = System.currentTimeMillis();/////時間計測エンド
//					totaldect += timee - times;//時間計測

					String[] strAry = filename.split("\n");
					FileOutputStream fout = new FileOutputStream(filepasst.getText().toString()+strAry[0]);

					byte[] fileBuff = new byte[1024];
					int blen;

					//ファイルサイズを取得、ダイアログにパーセント反映
					socket = new Socket(hostName, port_no);
					is = socket.getInputStream();
					BufferedReader sbr = new BufferedReader(new InputStreamReader(is));
					while(is.available() == 0);
					long filebytelen = Long.parseLong(sbr.readLine());
					sbr.close();
					socket.close();

					//送信率計算
					double sendper = 1024.0/filebytelen*100;
					double sendperall=sendper;

//					times = System.currentTimeMillis();/////時間計測スタート
					socket = new Socket(hostName, port_no);
					is = socket.getInputStream();
					while(is.available() == 0);
					CipherInputStream cos = new CipherInputStream(is, aescipher);
					times = System.currentTimeMillis();/////時間計測スタート
					while((blen = cos.read(fileBuff)) != -1){
						progressDialogper.setProgress((int)sendperall);
						sendperall+=sendper;
						fout.write(fileBuff, 0, blen);
					}
					socket.close();
					timee = System.currentTimeMillis();/////時間計測エンド
					ltime2 += timee - times;//時間計測


					Log.d("CryptToolBox","FileDec for App1_1ut: Decrypt AES with RSA "+ltime1+" ms\r\n");
			    	Log.d("CryptToolBox","FileDec for App1_1ut: AES Including File Transmission time "+ltime2+" ms\r\n");

			    	setTextAsync("Success",3);

					//ファイル情報表示
					fileinfo(strAry[0],strAry[1].substring(39),strAry[1].substring(9,28));

				}else{
					//検証失敗の処理
					socket = new Socket(hostName, port_no);
					aprpw = new PrintWriter(socket.getOutputStream(),true);
					aprpw.println("1");
					socket.close();
					setTextAsync("verify not",2);
				}
			}else{
				//検証失敗の処理、↑と同じ
				socket = new Socket(hostName, port_no);
				aprpw = new PrintWriter(socket.getOutputStream(),true);
				aprpw.println("1");
				socket.close();
				setTextAsync("verify not",2);
			}


		} catch (UnknownHostException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (SignatureException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO 自動生成された catch ブロック
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

		//ファイル開く
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
