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
import java.io.OutputStreamWriter;
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
import java.util.Calendar;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("HandlerLeak")
public class FileUploadAndShare extends FragmentActivity implements OnFileSelectedListener{

	String DestinationIDData = "DestinationIDData.dat";
	
	private static final int REQUEST_CODE = 0;	
	
	private ProgressDialog progressDialog;
	Handler	 mHandler;

	int port_no = 5111;
	String hostName = "202.16.211.113";
	String idpubpem = "key_idpub.pem";
	String sigkeypem = "key_sigkey.pem";
	String useriddat ="dat_userid.dat";

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fileupload);
		
		TextView tv = (TextView)findViewById(R.id.textView1);
		tv.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(FileUploadAndShare.this, DemoUpload.class);
				startActivity(intent);
			}
		});
		
		IDManagementButton();
		
		Button btFileSelect = (Button)findViewById(R.id.button4);
		btFileSelect.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String[] title={"Another App Select","Direct Path Select"};

				new AlertDialog.Builder(FileUploadAndShare.this)
				.setTitle("FileSelectMethod")
				.setItems(title, new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, final int which) {
						if(which==0){
							AnotherApp();
						}else if(which==1){
							DirectInput();
						}

					}
				})
				.setNeutralButton ("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}
				})
				.setCancelable(false)
				.show();


			}
		});
		
		
		FileUploadButton();
	}

	private void IDManagementButton() {
		// TODO Auto-generated method stub
		final TextView idt = (TextView)findViewById(R.id.editText1);
		final TextView memot = (TextView)findViewById(R.id.editText2);
		
		Button recordBt = (Button)findViewById(R.id.button1);
		recordBt.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (idt.getText().toString().matches("[a-zA-Z0-9@_.-]+")){

					String setmes="Do you want to save this ID?\n  ID: "+idt.getText().toString()+"\n  memo: "+memot.getText().toString();

					new AlertDialog.Builder(FileUploadAndShare.this)
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
							String linebuf;
							String iddatstr="";
							InputStream in;
							try {
								in = openFileInput(DestinationIDData);
								BufferedReader reader= new BufferedReader(new InputStreamReader(in,"UTF-8"));
								while( (linebuf = reader.readLine()) != null ){
									iddatstr+=linebuf+"/";
								}
								reader.close();
								in.close();
							} catch (FileNotFoundException e1) {
								// TODO 自動生成された catch ブロック
								e1.printStackTrace();
							} catch (UnsupportedEncodingException e) {
								// TODO 自動生成された catch ブロック
								e.printStackTrace();
							} catch (IOException e) {
								// TODO 自動生成された catch ブロック
								e.printStackTrace();
							}

							int idnum=1;
							if(iddatstr!=""){
								String[] strAry = iddatstr.split("/");
								idnum = Integer.parseInt(strAry[0]);
								idnum++;
								iddatstr="";
								for(int i=1;i<strAry.length;i++){
									iddatstr+=strAry[i]+"\n";
								}
							}


							try {
								OutputStream out = openFileOutput(DestinationIDData,MODE_PRIVATE);
								PrintWriter writer = new PrintWriter(new OutputStreamWriter(out,"UTF-8"));

								writer.append(idnum+"\n"+iddatstr);

								writer.append(idt.getText().toString()+":");
								writer.append(memot.getText().toString()+"\n");
								writer.close();
								out.close();

								Toast.makeText(FileUploadAndShare.this, "ID Saved", Toast.LENGTH_SHORT).show();
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

						}
					})
					.setCancelable(false)
					.show();

				}
				
			}
		});
		
		Button loadBt = (Button)findViewById(R.id.button2);
		loadBt.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				idlistdialog(1);
			}
		});
		
		Button deleteBt = (Button)findViewById(R.id.button3);
		deleteBt.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				idlistdialog(2);
				
			}
		});

	}

	protected void idlistdialog(final int rdf) {
		// TODO Auto-generated method stub
		final TextView idt = (TextView)findViewById(R.id.editText1);
		final TextView memot = (TextView)findViewById(R.id.editText2);

		String linebuf;
		String iddatstr="";
		InputStream in;
		try {
			in = openFileInput(DestinationIDData);
			BufferedReader reader= new BufferedReader(new InputStreamReader(in,"UTF-8"));
			while( (linebuf = reader.readLine()) != null ){
				iddatstr+=linebuf+"/";
			}
			reader.close();
			in.close();
			Log.d("FileShare", iddatstr);

		} catch (FileNotFoundException e1) {
			// TODO 自動生成された catch ブロック
			e1.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		if(iddatstr!=""){
			String[] strAry = iddatstr.split("/");
			int idnum = Integer.parseInt(strAry[0]);

			String idreadlist[] = null;
			String title="DestinationID List";
			if(rdf==1){
				idreadlist=new String[idnum];
				title +=" (load)";
			}else if(rdf==2){
				idreadlist=new String[idnum+1];
				idreadlist[idnum]="All Delete";
				title +=" (delete)";
			}
			final String idlist[]=new String[idnum];
			final String memolist[]=new String[idnum];

			for(int i=0;i<strAry.length-1;i++){
				String[] strAry2 = strAry[i+1].split(":");
				idlist[i]=strAry2[0];
				if(strAry2.length==2){
					memolist[i]=strAry2[1];
				}else{
					memolist[i]="";
				}
				idreadlist[i]="ID: "+idlist[i]+"\nmemo: "+memolist[i];
			}


			new AlertDialog.Builder(FileUploadAndShare.this)
			.setTitle(title)
			.setItems(idreadlist, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface dialog, final int which) {

					if(rdf==1){
						idt.setText(idlist[which]);
						memot.setText(memolist[which]);
					}else if(rdf==2){
						if(idlist.length==which){
							deleteid("","",-1);
						}else{
							deleteid(idlist[which],memolist[which],which);
						}

					}

				}
			}
			)
			.setNeutralButton ("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					;
				}
			})
			.setCancelable(false)
			.show();
		}
		
	}

	private void deleteid(String idstr,String memostr,final int which){
		String setmes="";
		if(which==-1){
			setmes="Do you want to delete All ID ?";
		}else{
			setmes="Do you want to delete this ID ?\n  ID: "+idstr+"\n	memo: "+memostr;
		}
		new AlertDialog.Builder(FileUploadAndShare.this)
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
				if(which==-1){
					deleteFile(DestinationIDData);
				}else{
					String linebuf;
					String iddatstr="";
					InputStream in;
					try {
						in = openFileInput(DestinationIDData);
						BufferedReader reader= new BufferedReader(new InputStreamReader(in,"UTF-8"));
						String idnumstr = reader.readLine();
						int idnum = Integer.parseInt(idnumstr);
						iddatstr+=(idnum-1)+"\n";
						int roopf=0;
						while( (linebuf = reader.readLine()) != null ){
							if(roopf!=which){
								iddatstr+=linebuf+"\n";
							}
							roopf++;
						}
						reader.close();
						in.close();



						OutputStream out = openFileOutput(DestinationIDData,MODE_PRIVATE);
						PrintWriter writer = new PrintWriter(new OutputStreamWriter(out,"UTF-8"));

						writer.append(iddatstr);
						writer.close();
						out.close();
					} catch (FileNotFoundException e1) {
						// TODO 自動生成された catch ブロック
						e1.printStackTrace();
					} catch (UnsupportedEncodingException e) {
						// TODO 自動生成された catch ブロック
						e.printStackTrace();
					} catch (IOException e) {
						// TODO 自動生成された catch ブロック
						e.printStackTrace();
					}

				}

			}
		})
		.setCancelable(false)
		.show();

	}
	
	protected void showToast() {
		// TODO Auto-generated method stub
		Toast.makeText(this, "Toast", Toast.LENGTH_LONG).show();
	}

	public void DirectInput() {
		TextView filedatatext = (TextView)findViewById(R.id.textView2);

		DialogFragment dialogFragment = new FileSelectDialogFragment();
		Bundle bundle = new Bundle();
		bundle.putString(FileSelectDialogFragment.ROOT_DIRECTORY, "/");

		if(filedatatext.getText().toString().matches("No Data")){
			bundle.putString(FileSelectDialogFragment.INITIAL_DIRECTORY, Environment.getExternalStorageDirectory().getPath());
		}else{
			String[] fdt0 = filedatatext.getText().toString().split("\n");
			String[] fdt = fdt0[4].split("/");
			String filenamepass = "/";
			for(int i=1;i<fdt.length-1;i++){
				filenamepass += fdt[i];
				if(i<fdt.length-2){
					filenamepass+="/";
				}
			}
			if(filenamepass=="/"){
				filenamepass = Environment.getExternalStorageDirectory().getPath();
			}
			bundle.putString(FileSelectDialogFragment.INITIAL_DIRECTORY, filenamepass);
			Log.d("testtag", filenamepass);
		}

		bundle.putString(FileSelectDialogFragment.PREVIOUS, "..");
		bundle.putString(FileSelectDialogFragment.CANCEL, "Cancel");
		bundle.putSerializable(FileSelectDialogFragment.LISTENER, this);
		dialogFragment.setArguments(bundle);
		dialogFragment.setCancelable(false);
		dialogFragment.show(this.getSupportFragmentManager(), "dialog");
	}

	
	public void AnotherApp(){
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		startActivityForResult(intent, REQUEST_CODE);
	}
	
	@SuppressLint("NewApi")
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_CODE) {
			switch (resultCode) {
				case Activity.RESULT_OK:
					String testpass = data.getData().getPath();
					Log.d("testtag", "testpass : "+testpass);

					String[] projection = {MediaStore.MediaColumns.DATA};
					Cursor cursor = getContentResolver().query(data.getData(), projection, null, null, null);
					if (cursor.getCount() == 1) {
						cursor.moveToNext();
//						cursor.moveToFirst();
						String filePath = cursor.getString(0);
						cursor.close();
						Log.d("testtag", "pass : "+filePath);
						if(filePath!=null){
							fileinfowrite(filePath);
						}else{
							TextView filedatatext = (TextView)findViewById(R.id.textView2);
							filedatatext.setText("Error : Null Data");


							//以下追加
							String id = DocumentsContract.getDocumentId(data.getData());
							String selection = "_id=?";
							String[] idsplit = id.split(":");
							if(idsplit.length>1){
								String[] selectionArgs = new String[]{idsplit[1]};

								Cursor cursor2 = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[]{MediaColumns.DATA}, selection, selectionArgs, null);
								if (cursor2.getCount() == 1) {
									cursor2.moveToNext();
									String filePath2 = cursor2.getString(0);
									cursor2.close();
									Log.d("testtag", "pass2 : "+filePath2);
									if(filePath2!=null){
										fileinfowrite(filePath2);
									}
								}
							}
						}
					}
			}
		}
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
	
	public void fileinfowrite(String path){
		TextView filedatatext = (TextView)findViewById(R.id.textView2);

		String[] strAry = path.split("/");


		String fileinfo="FileName:\n  "+strAry[strAry.length-1];

		fileinfo+="\n\nPath:\n	"+path;

		File file = new File(path);
		fileinfo+="\nsize:\n  "+file.length()+" byte";

		Date date = new Date(file.lastModified());
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);

		fileinfo+="\nLast Modified:\n  "+date+" "+cal.get(Calendar.HOUR_OF_DAY)+":"+cal.get(Calendar.MINUTE)+":"+cal.get(Calendar.SECOND);

		filedatatext.setText(fileinfo);
	}
	
	private void FileUploadButton() {
		
		Button btUpload = (Button)findViewById(R.id.button5);
		btUpload.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				progressDialog.show();
				progressDialog.setProgress(0);
				new Thread(new Runnable() {
					@Override
					public void run() {
						// バックグランドをここに記述
						setTextAsync("",2);
						clickrun();
						handler.sendEmptyMessage(0);
					}
				}).start();
				
			}
		});
		
	}
	
	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
				//終了処理を記述
			if(msg.what==0){
				progressDialog.dismiss();
			}
		}
	};
	
	private void setTextAsync(final String text,final int flag){
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				TextView status = (TextView)findViewById(R.id.textView4);

				switch (flag) {
					case 1:
						status.setText("Error:"+text);
						break;
					case 2:
						status.setText("Success:"+text);
						break;

				}

			}
		});
	}

	private void clickrun(){
		TextView idt = (TextView)findViewById(R.id.editText1);
		TextView filedatatext = (TextView)findViewById(R.id.textView2);

		if(idt.getText().toString().matches("[a-zA-Z0-9@_.-]+")==false&&filedatatext.getText().toString().matches("No Data")){
			setTextAsync("error: notID & notFile",1);
		}else if(idt.getText().toString().matches("[a-zA-Z0-9@_.-]+")==false){
			setTextAsync("error: notID",1);
		}else if(filedatatext.getText().toString().matches("No Data")){
			setTextAsync("error: notFile",1);
		}else{
			setTextAsync("",1);

			//ファイルが消されてないかどうか（ファイル選択後、削除された場合の処理）
			String[] fdt0 = filedatatext.getText().toString().split("\n");
			String[] fdt = fdt0[4].split("/");
			String filenamepass = "/";
			for(int i=1;i<fdt.length;i++){
				filenamepass += fdt[i]+"/";
			}
			File file = new File(filenamepass);
			if (file.exists()){
				Log.d("testtag", "idf00 : ");
				Log.d("testtag", "idf11 : ");
				//BDから公開鍵を取得
				int idf = keyacq();
				Log.d("testtag", "idf : "+idf);
				if(idf == 0){
					setTextAsync("not ID",1);
				}else if(idf == 2){
					setTextAsync("not connect",1);
				}else{
					//鍵が保存されているか
					if(prikeyread(sigkeypem)==null){
						setTextAsync("not Generate key",1);
					}else{
						encfile();
					}
				}

			}else{
				setTextAsync("break file data",1);
			}
		}
	}

	private int keyacq(){
		//ToDo　ここはWebに変えよう
		TextView idt = (TextView)findViewById(R.id.editText1);

		int flag=0;

		Socket socket;
		InputStream is;

		try {
			//どのアプリからか判別
			socket = new Socket(hostName, port_no);
			PrintWriter aprpw = new PrintWriter(socket.getOutputStream(),true);
			aprpw.println("2");
			socket.close();

			//IDがあるかどうか尋ねる
			socket = new Socket(hostName, port_no);
			PrintWriter pw = new PrintWriter(socket.getOutputStream(),true);
			pw.println(idt.getText().toString());
			socket.close();

			socket = new Socket(hostName, port_no);
			is = socket.getInputStream();
			BufferedReader sbr = new BufferedReader(new InputStreamReader(is));
			while(is.available() == 0);
			String sr = sbr.readLine();
			sbr.close();
			socket.close();
			if(sr.matches("0")==false){
				FileOutputStream fout = openFileOutput(idpubpem, MODE_PRIVATE);
				socket = new Socket(hostName, port_no);
				is = socket.getInputStream();

				byte[] byteBuffer = new byte[128];
				int recvMsgSize;
				while(is.available() == 0);
				while((recvMsgSize = is.read(byteBuffer)) != -1){
					fout.write(byteBuffer,0,recvMsgSize);
				}
				socket.close();

				flag=1;
			}
		} catch (UnknownHostException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
			flag=2;
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
			flag=2;
		}
		return flag;

	}

	private PublicKey pubkeyread(String filename){
		FileInputStream fin;
		String str64 = "";

		try {
			fin = openFileInput(filename);

			BufferedReader reader = new BufferedReader(new InputStreamReader(fin,"UTF-8"));

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

	
	private PrivateKey prikeyread(String filename){
		FileInputStream fin;
		String str64 = "";

		try {
			fin = openFileInput(filename);

			BufferedReader reader = new BufferedReader(new InputStreamReader(fin,"UTF-8"));

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
	
	private void encfile(){
		TextView filedatatext = (TextView)findViewById(R.id.textView2);
		progressDialog.setProgress(0);

		long times;
		long timee;
		long ltime1 = 0;
		long ltime2 = 0;


		PublicKey publicKey = pubkeyread(idpubpem);
//		PublicKey publicKey = pubkeyread("vrikey.pem");
		PrivateKey privateKey = prikeyread(sigkeypem);

		String[] fdt0 = filedatatext.getText().toString().split("\n");
		String[] fdt = fdt0[4].split("/");
		String filenamepass = "/";
		for(int i=1;i<fdt.length;i++){
			filenamepass += fdt[i]+"/";
		}

		File filep = new File(filenamepass);
		//ファイル名取得
		String filenames = fdt[fdt.length-1];

		Socket socket;
		InputStream is;
		OutputStream outStream;

		try {
			//自分のIDもここで送る
			FileInputStream fin = openFileInput(useriddat);
			BufferedReader reader = new BufferedReader( new InputStreamReader(fin , "UTF-8"));
			String myid = reader.readLine();
			reader.close();
			socket = new Socket(hostName, port_no);
			PrintWriter idpw = new PrintWriter(socket.getOutputStream(),true);
			idpw.println(myid);
			socket.close();

			//ファイル名を送る
//			long times = System.currentTimeMillis();/////時間計測スタート
			String aeskeyst = randst();
			String aesivst = randst();

			byte[] aeskey = aeskeyst.getBytes("UTF-8");
			byte[] aesiv  = aesivst.getBytes("UTF-8");

			SecretKey aescipherKey	  = new SecretKeySpec(aeskey, "AES");
			IvParameterSpec ivSpec = new IvParameterSpec(aesiv);

			// 条件を指定してCipherを暗号モードで初期化
			Cipher aescipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			aescipher.init(Cipher.ENCRYPT_MODE, aescipherKey, ivSpec);

			// 暗号化してみる。結果のバイト配列を16進文字列で表示
			times = System.currentTimeMillis();/////時間計測スタート
			byte[] cipherData = aescipher.doFinal(filenames.getBytes("UTF-8"));

			socket = new Socket(hostName, port_no);
			outStream = socket.getOutputStream();
			outStream.write(cipherData);
			socket.close();
			timee = System.currentTimeMillis();/////時間計測エンド
			ltime2 += timee - times;//時間計測


			//ファイル読み込み、送信
			FileInputStream fis = new FileInputStream(filep);
			CipherInputStream cis = new CipherInputStream(fis, aescipher);
			byte[] fileBuff = new byte[1024];
			int blen;

			//送信率計算
			double sendper = 1024.0/filep.length()*100;
			double sendperall=sendper;
//			Log.d("testtag", "long:"+sendper);
//			Log.d("testtag", "long:"+1024/filep.length()*100.0);

			socket = new Socket(hostName, port_no);
			outStream = socket.getOutputStream();
			times = System.currentTimeMillis();/////時間計測スタート
			while((blen = cis.read(fileBuff)) != -1){
				progressDialog.setProgress((int)sendperall);
				sendperall+=sendper;
				outStream.write(fileBuff, 0, blen);
			 }
			socket.close();
			timee = System.currentTimeMillis();/////時間計測エンド
			ltime2 += timee - times;//時間計測


			/////////////////
			//AES鍵の暗号署名、送信
			times = System.currentTimeMillis();/////時間計測スタート
			Cipher rsacipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			rsacipher.init(Cipher.ENCRYPT_MODE, publicKey);
			byte[] encaeskey = rsacipher.doFinal(aeskey);
			Signature signer = Signature.getInstance("SHA256withRSA");
			signer.initSign(privateKey);
			signer.update(encaeskey);
			byte[] sigaeskey = signer.sign();

			byte[] encaesiv = rsacipher.doFinal(aesiv);
			signer.update(encaesiv);
			byte[] sigaesiv = signer.sign();
			timee = System.currentTimeMillis();/////時間計測エンド
			ltime1 += timee - times;//時間計測

//			times = System.currentTimeMillis();/////時間計測スタート
			socket = new Socket(hostName, port_no);
			outStream = socket.getOutputStream();
			outStream.write(encaeskey);
			socket.close();

			socket = new Socket(hostName, port_no);
			outStream = socket.getOutputStream();
			outStream.write(sigaeskey);
			socket.close();

			socket = new Socket(hostName, port_no);
			outStream = socket.getOutputStream();
			outStream.write(encaesiv);
			socket.close();

			socket = new Socket(hostName, port_no);
			outStream = socket.getOutputStream();
			outStream.write(sigaesiv);
			socket.close();
//			timee = System.currentTimeMillis();/////時間計測エンド
//			totaltrat += timee - times;//時間計測

			//検証できたかどうか
			socket = new Socket(hostName, port_no);
			is = socket.getInputStream();
			BufferedReader sbr = new BufferedReader(new InputStreamReader(is));
			while(is.available() == 0);
			String vrif = sbr.readLine();
			sbr.close();
			socket.close();
			if(vrif.matches("1")){
				//検証不可
				setTextAsync("verify not",1);
			}else{
				//検証OK
				setTextAsync("Success",2);
				Log.d("CryptToolBox","FileEnc for App1_1ut: Encrypt AES with RSA "+ltime1+" ms\r\n");
				Log.d("CryptToolBox","FileEnc for App1_1ut: AES Including File Transmission time "+ltime2+" ms\r\n");
			}


		} catch (FileNotFoundException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
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
		} catch (SignatureException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

	}
	
	private String randst(){
		String ranstr = "0123456789abcdefghijklnmopqrstuvwxyz";
		String das = "";
		for(int i=0;i<16;i++){
			das += ranstr.charAt((int)(Math.random()*36));
		}

		return das;
	}

}
