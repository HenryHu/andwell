package net.henryhu.andwell;

import java.io.IOException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class AndWell extends Activity implements AuthHandler {
//	private EditText tUsername = null;
//	private EditText tPassword = null;
	private EditText tServer = null;
	private TextView tStatus = null;
//	private CheckBox cSavePwd = null;
//	private CheckBox cAutoLogin = null;
//	private Button bLogin = null;
	private Activity myAct = null;
	private SharedPreferences pref = null;
	private final Handler handler = new Handler();
	private ProgressDialog loginDialog = null;
	private Button bLoginOAuth = null;
	private Button bGetOAuthCode = null;
	private EditText tOAuthCode = null;
	String basePath = "";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        myAct = this;
    	pref = getSharedPreferences(Utils.PREFS_FILE, MODE_PRIVATE);
    	
//        bLogin = (Button)findViewById(R.id.bLogin);
//        tUsername = (EditText)findViewById(R.id.tUsername);
//        tPassword = (EditText)findViewById(R.id.tPassword);
        tStatus = (TextView)findViewById(R.id.tStatus);
		tServer = (EditText)findViewById(R.id.tServerAPI);
//        cSavePwd = (CheckBox)findViewById(R.id.cSavePwd);
//        cAutoLogin = (CheckBox)findViewById(R.id.cAutoLogin);
        
        bLoginOAuth = (Button)findViewById(R.id.bLoginOAuth);
        bGetOAuthCode = (Button)findViewById(R.id.bGetOAuthCode);
        tOAuthCode = (EditText)findViewById(R.id.tOAuthCode);
        
//        bLogin.setOnClickListener(loginListener);
        bLoginOAuth.setOnClickListener(loginOAuthListener);
        bGetOAuthCode.setOnClickListener(getOAuthCodeListener);
        
        loadSettings();
/*        if (pref.getBoolean("auto_login", true))
        	doLogin();*/
        if (getIntent().getData() != null) {
        	Uri uri = getIntent().getData();
        	try {
        		String oauth_code = uri.getQueryParameter("code");
        		if (oauth_code == null) {
        			throw new Exception();
        		}
            	tOAuthCode.setText(oauth_code);
            	onOAuthGotCode(tOAuthCode.getText().toString());
        	} catch (Exception e) {
        		try {
        			String error = uri.getQueryParameter("error");
        			showMsg("Login error: " + error);
        		} catch (Exception ex) {
        			showMsg("Login error!");
        		}
        	}
        }
        TryRelogin();
    }
    
    void showMsg(String msg) {
    	Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onDestroy()
    {
    	super.onDestroy();
    	loginDialog = null;
    }
    
    void loadSettings()
    {
		tServer.setText(pref.getString("server_api", ""));
		basePath = pref.getString("server_api", "");
/*    	tUsername.setText(pref.getString("username", ""));
        Boolean save_pw = pref.getBoolean("save_password", false);
        cSavePwd.setChecked(save_pw);
        if (save_pw)
        {
        	tPassword.setText(pref.getString("password", ""));
        }
        Boolean auto_login = pref.getBoolean("auto_login", false);
        cAutoLogin.setChecked(auto_login);*/
    }
    
    void TryRelogin()
    {
		if (basePath.equals(""))
			return;

    	if (pref.getString("token", "").equals(""))
    		return;
    	
    	loginDialog = ProgressDialog.show(myAct, "Log In", "Logging in...");
		Auth.doReauth(pref.getString("token", ""), handler, (AuthHandler)myAct, basePath);
    }

	void saveSettings()
	{
        SharedPreferences.Editor ed = pref.edit();
		ed.putString("server_api", tServer.getText().toString());
/*        ed.putString("username", tUsername.getText().toString());
        ed.putBoolean("save_password", cSavePwd.isChecked());
        ed.putBoolean("auto_login", cAutoLogin.isChecked());
        if (cSavePwd.isChecked())
        	ed.putString("password", tPassword.getText().toString());
        else
        	ed.putString("password", "");*/
        ed.commit();
	}
    
    protected void onPause() {
        super.onPause();

		saveSettings();

    }

    void showToast(String text)
    {
    	showToast(text,  Toast.LENGTH_SHORT);
    }
    
    void showToast(String text, int time)
    {
		Toast toast = Toast.makeText(getApplicationContext(), text, time);
		toast.show();
    }
    
/*    private OnClickListener loginListener = new OnClickListener()
    {
		@Override
		public void onClick(View arg0) {
			doLogin();
		}
    };*/
    
    private OnClickListener loginOAuthListener = new OnClickListener()
    {
    	public void onClick(View arg0) {
    	    onOAuthGotCode(tOAuthCode.getText().toString());
    	}
    };
    
    private OnClickListener getOAuthCodeListener = new OnClickListener()
    {
    	public void onClick(View arg0) {
    		doLoginOAuth();
    	}
    };
    
/*    public void doLogin()
    {
    	loginDialog = ProgressDialog.show(myAct, "Log In", "Logging in...");
		
		String name = tUsername.getText().toString();
		String pass = tPassword.getText().toString();
		
		Auth.doAuth(name, pass, handler, (AuthHandler)myAct);
    }*/
    
    public void doLoginOAuth()
    {
		saveSettings();
		basePath = pref.getString("server_api", "");
    	String url = basePath + "/auth/auth?response_type=code&client_id="
    	+ Defs.OAuthClientID + "&redirect_uri="
    	+ Utils.getOAuthRedirectURI(basePath);
    	
    	Intent i = new Intent(Intent.ACTION_VIEW);
    	i.setData(Uri.parse(url));
    	startActivity(i);
    	finish();
    }
    
    void authResult(boolean result)
    {
    	if (loginDialog != null)
    		loginDialog.dismiss();
		if (result)
		{
			Intent intent = new Intent(this, Main.class);
			
			startActivity(intent);
			finish();
		}                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     
    }
    
    public void onAuthIOException(IOException e)
    {
		e.printStackTrace();
		setLoginStatus("Error(Exception): " + e.getMessage());
		authResult(false);
    }
    
    public void onAuthParseException(Exception e)
    {
    	e.printStackTrace();
    	setLoginStatus("Error parsing result: " + e.getMessage());
		authResult(false);
    }
    
    public void onAuthFail(String reason)
    {
		setLoginStatus("Error: " + reason);
		authResult(false);
    }
    
    public void onAuthOK(String token)
    {
    	setLoginStatus("OK, token: " + token);
    	Editor edit = pref.edit();
    	edit.putString("token", token);
    	edit.commit();
    	authResult(true);
    }
    
    public void setLoginStatus(final String status)
    {
    	tStatus.post(new Runnable(){ public void run(){ tStatus.setText(status); } });
    }
        
    void onOAuthGotCode(String code)
    {
    	loginDialog = ProgressDialog.show(myAct, "Log In", "Logging in...");
    	Auth.doOAuth(code, handler, (AuthHandler)myAct, basePath);
    }
}


/*
private void trustEveryone() {
    try {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier(){
                    public boolean verify(String hostname, SSLSession session) {
                            return true;
                    }});
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[]{new X509TrustManager(){
                    public void checkClientTrusted(X509Certificate[] chain,
                                    String authType) throws CertificateException {}
                    public void checkServerTrusted(X509Certificate[] chain,
                                    String authType) throws CertificateException {}
                    public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                    }}}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(
                            context.getSocketFactory());
    } catch (Exception e) { // should never happen
            e.printStackTrace();
    }
}
*/
/*
This code is public domain: you are free to use, link and/or modify it in any way you want, for all purposes including commercial applications.
*/
/*	public static HttpClient wrapClient(HttpClient base) {
	try {
		SSLContext ctx = SSLContext.getInstance("TLS");
		X509TrustManager tm = new X509TrustManager() {

			public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
			}

			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		};
		ctx.init(null, new TrustManager[]{tm}, null);
		SSLSocketFactory ssf = ctx.getSocketFactory();
//		ssf.setHostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		ClientConnectionManager ccm = base.getConnectionManager();
		SchemeRegistry sr = ccm.getSchemeRegistry();
		sr.register(new Scheme("https", (SocketFactory) ssf, 8080));
		return new DefaultHttpClient(ccm, base.getParams());
	} catch (Exception ex) {
		ex.printStackTrace();
		return null;
	}
}*/
