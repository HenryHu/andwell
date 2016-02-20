package net.henryhu.andwell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;

public class Auth {
	static Thread startThread(final Runnable runnable)
	{
		Thread t = new Thread() {
			@Override
			public void run() {
				runnable.run();
			}
		};
		t.start();
		return t;
	}
	
	static class IOExceptionRunnable implements Runnable {
		AuthHandler ctx;
		IOException e;
		
		public IOExceptionRunnable(AuthHandler _ctx, IOException _e)
		{
			ctx = _ctx;
			e = _e;
		}
		
		public void run()
		{
			ctx.onAuthIOException(e);
		}
	}
	
	static class AuthFailRunnable implements Runnable {
		AuthHandler ctx;
		String reason;
		
		public AuthFailRunnable(AuthHandler _ctx, String _reason)
		{
			ctx = _ctx;
			reason = _reason;
		}
		
		public void run()
		{
			ctx.onAuthFail(reason);
		}
	}
	
	static class AuthOKRunnable implements Runnable {
		AuthHandler ctx;
		String token;
		
		public AuthOKRunnable(AuthHandler _ctx, String _token)
		{
			ctx = _ctx;
			token = _token;
		}
		
		public void run()
		{
			ctx.onAuthOK(token);
		}
	}
	
	static class AuthParseRunnable implements Runnable {
		AuthHandler ctx;
		Exception e;
		
		public AuthParseRunnable(AuthHandler _ctx, Exception _e)
		{
			ctx = _ctx;
			e = _e;
		}
		
		public void run()
		{
			ctx.onAuthParseException(e);
		}
	}
	
	public static boolean reauth(String basePath, String token, Handler handler, AuthHandler ctx)
	{
		RequestArgs args = new RequestArgs(token);
		
		HttpURLConnection resp;
		try {
			resp = Utils.doGet(basePath, "/session/verify", args);
		}
		catch (IOException e)
		{
			handler.post(new IOExceptionRunnable(ctx, e));
			return false;
		}
		try {
			Utils.checkResult(resp);
		} catch (Exception e) {
			final String reason = Exceptions.getErrorMsg(e);
			handler.post(new AuthFailRunnable(ctx, reason));
			return false;
		}
		
		try {
			String ret = Utils.readResp(resp);
			JSONObject obj;
			try {
				obj = new JSONObject(ret);
				if (obj.getString("status").equals("ok"))
				{
					handler.post(new AuthOKRunnable(ctx, token));
					return true;
				}
				handler.post(new AuthFailRunnable(ctx, obj.getString("status")));
				return false;
			} catch (final JSONException e)
			{
				handler.post(new AuthParseRunnable(ctx, e));
				return false;
			}
		}
		catch (final IOException e)
		{
			handler.post(new IOExceptionRunnable(ctx, e));
			return false;
		}

	}
	
	public static boolean oauth(String basePath, String code, Handler handler, AuthHandler ctx)
	{
		RequestArgs args = new RequestArgs("");
		args.add("grant_type", "authorization_code");
		args.add("client_id", Defs.OAuthClientID);
		args.add("client_secret", Defs.OAuthClientSecret);
		args.add("code", code);
		args.add("redirect_uri", Utils.getOAuthRedirectURI(basePath));
		
		HttpURLConnection resp;
		try {
			resp = Utils.doGet(basePath, "/auth/token", args);
			if (resp.getResponseCode() != 200) {
				final String reason = resp.getResponseMessage();
				handler.post(new AuthFailRunnable(ctx, reason));
				return false;
			} else {
				BufferedReader br = new BufferedReader(new InputStreamReader(resp.getInputStream()));
				String ret = br.readLine();
				JSONObject obj;
				try {
					obj = new JSONObject(ret);
					final String token = obj.getString("access_token");
					handler.post(new AuthOKRunnable(ctx, token));
					return true;
				} catch (final JSONException e)
				{
					handler.post(new AuthParseRunnable(ctx, e));
					return false;
				}
			}
		}
		catch (IOException e)
		{
			handler.post(new IOExceptionRunnable(ctx, e));
			return false;
		}

	}

    /*
	public static boolean auth(String name, String pass, Handler handler,
			final AuthHandler ctx, final String basePath)
	{
		String epwd = Base64.encodeToString(pass.getBytes(), Base64.DEFAULT);
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("user", name));
		params.add(new BasicNameValuePair("pass", epwd));

		HttpResponse resp;
		try {
			resp = Utils.doPost(basePath, "/auth/pwauth", params);
		}
		catch (final IOException e)
		{
			handler.post(new Runnable() { public void run() {ctx.onAuthIOException(e);}});
			return false;
		}
		if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
		{
			final String reason = resp.getStatusLine().getReasonPhrase();
			handler.post(new Runnable() { public void run() {ctx.onAuthFail(reason);}});
			return false;
		} else {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
				String ret = br.readLine();
				JSONObject obj;
				try {
					obj = new JSONObject(ret);
					final String token = obj.getString("access_token");
					handler.post(new Runnable() { public void run() {ctx.onAuthOK(token);}});
					return true;
				} catch (final JSONException e)
				{
					handler.post(new Runnable() { public void run() {ctx.onAuthParseException(e);}});
					return false;
				}
			}
			catch (final IOException e)
			{
				handler.post(new Runnable() { public void run() {ctx.onAuthIOException(e);}});
				return false;
			}
		}
	}

	static Thread doAuth(final String name, final String pass,
			final Handler handler, final AuthHandler ctx, final String basePath)
	{
		Runnable runnable = new Runnable() {
			public void run() {
				auth(name, pass, handler, ctx, basePath);
			}
		};
		return startThread(runnable);
	}
	*/
	
	static Thread doOAuth(String code, Handler handler, AuthHandler ctx, String basePath)
	{
		return startThread(new OAuthRunnable(code, handler, ctx, basePath));
	}
	
	static class OAuthRunnable implements Runnable {
		private String code;
		private AuthHandler ctx;
		private Handler handler;
		private String basePath;
		
		public OAuthRunnable(String _code, Handler _handler, AuthHandler _ctx, String _basePath)
		{
			code = _code;
			ctx = _ctx;
			handler = _handler;
			basePath = _basePath;
		}
		
		public void run() {
			oauth(basePath, code, handler, ctx);
		}
	}
	
	static Thread doReauth(String token, Handler handler, AuthHandler ctx, String basePath)
	{
		return startThread(new ReauthRunnable(token, handler, ctx, basePath));
	}
	
	static class ReauthRunnable implements Runnable {
		private AuthHandler ctx;
		private Handler handler;
		private String token;
		private String basePath;
		
		public ReauthRunnable(String _token, Handler _handler, AuthHandler _ctx, String _basePath)
		{
			ctx = _ctx;
			handler = _handler;
			token = _token;
			basePath = _basePath;
		}
		
		public void run() {
			reauth(basePath, token, handler, ctx);
		}
	}

}
