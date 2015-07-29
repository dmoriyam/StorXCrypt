package jp.go.nict.nsri.sal.regista.fileshare;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity implements Runnable {

	private static ProgressDialog waitDialog;
	private Thread thread;
	public String packName;
	private int flgKeyGet = 0;
	private byte[] keydat;

	private String strAlgPKE = "RSA";
	private String strAlgSIGBase = "RSA";
	private String strAlgSIGDetail = "SHA256withRSA";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		packName = getPackageName();

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Intent rcvIntent = getIntent();
    	Bundle bdl = rcvIntent.getExtras();
        if(bdl!=null){
    		String strKey = bdl.getString("KEYDATA");
    		keydat = bdl.getByteArray("KeyDat");

    		flgKeyGet = 1;
        }

		if(flgKeyGet==0){
			setWait();
		}
		else{
			keyGen();
		}
		
		Button btn01 = (Button)findViewById(R.id.app1_1ut_bt_fupshare);
		Button btn02 = (Button)findViewById(R.id.app1_1ut_bt_downloadfile);
		
		btn01.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(MainActivity.this,FileUploadAndShare.class);
				startActivity(intent);
			}
		});
		
		btn02.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(MainActivity.this,DownloadFiles.class);
				startActivity(intent);
				
			}
		});
		
	}

	private void keyGen() {
		// TODO Auto-generated method stub
		//two RSA 2048 bit key pairs

		try{
			SecureRandom secrand = new SecureRandom(keydat);
			KeyPairGenerator kpg = KeyPairGenerator.getInstance(strAlgPKE, "BC");
			kpg.initialize(2048, secrand);
			KeyPair kp_rsa2048 = kpg.generateKeyPair();
			byte[] pubkey = kp_rsa2048.getPublic().getEncoded(); // vk1
			byte[] privkey = kp_rsa2048.getPrivate().getEncoded(); // sk1

			KeyPairGenerator kpg2 = KeyPairGenerator.getInstance(strAlgSIGBase, "BC");
			kpg2.initialize(2048, secrand);
			KeyPair kp_rsa2048_2 = kpg2.generateKeyPair();
			byte[] verkey = kp_rsa2048_2.getPublic().getEncoded(); // vk12
			byte[] sigkey = kp_rsa2048_2.getPrivate().getEncoded(); // sk12

			Toast.makeText(this, "Pub Key:"+pubkey.length+":"+pubkey[0]+","+pubkey[1]+","+pubkey[2]+","+"/Verify Key:"+verkey.length+":"+verkey[0]+","+verkey[1], Toast.LENGTH_LONG).show();

		}
		catch(NumberFormatException e){
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void setWait() {
		// TODO Auto-generated method stub
		waitDialog = new ProgressDialog(this);
		waitDialog.setMessage("Access to Key Manager...");
		waitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		waitDialog.show();
		
		thread = new Thread(this);
		thread.start();
		
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		handler.sendEmptyMessage(0);
	}
	
	private Handler handler = new Handler(){
		public void handleMessage(Message msg){
			
			getKeyFromKeyManager();
			
			waitDialog.dismiss();
			waitDialog = null;
		}
	};

	protected void getKeyFromKeyManager() {
		// TODO Auto-generated method stub
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("sal/keyreq");
		intent.putExtra("KEYSTRING", "Please give me my key!!");
		intent.putExtra("PACKAGE_NAME", packName);
		startActivity(intent);
		finish();

	}

}
