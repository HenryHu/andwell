package org.net9.andwell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.util.Base64;

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
	};
	
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
	};
	
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
	};
	
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
	};
	
	public static boolean reauth(String token, Handler handler, AuthHandler ctx)
	{
		RequestArgs args = new RequestArgs(token);
		
		HttpResponse resp = null;
		try {
			resp = Utils.doGet("/session/verify", args.getValue());
		}
		catch (IOException e)
		{
			handler.post(new IOExceptionRunnable(ctx, e));
			return false;
		}
		if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
		{
			final String reason = resp.getStatusLine().getReasonPhrase();
			handler.post(new AuthFailRunnable(ctx, reason));
			return false;
		} else {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
				String ret = br.readLine();
				JSONObject obj = null;
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
	}
	
	public static boolean oauth(String code, Handler handler, AuthHandler ctx)
	{
		RequestArgs args = new RequestArgs("");
		args.add("grant_type", "authorization_code");
		args.add("client_id", Defs.OAuthClientID);
		args.add("client_secret", Defs.OAuthClientSecret);
		args.add("code", code);
		args.add("redirect_uri", Utils.OAuthRedirectURI);
		
		HttpResponse resp = null;
		try {
			resp = Utils.doGet("/auth/token", args.getValue());
		}
		catch (IOException e)
		{
			handler.post(new IOExceptionRunnable(ctx, e));
			return false;
		}
		if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
		{
			final String reason = resp.getStatusLine().getReasonPhrase();
			handler.post(new AuthFailRunnable(ctx, reason));
			return false;
		} else {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
				String ret = br.readLine();
				JSONObject obj = null;
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
			catch (final IOException e)
			{
				handler.post(new IOExceptionRunnable(ctx, e));
				return false;
			}
		}

	}
	
	public static boolean auth(String name, String pass, Handler handler,
			final AuthHandler ctx)
	{
		String epwd = Base64.encodeToString(pass.getBytes(), Base64.DEFAULT);
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("user", name));
		params.add(new BasicNameValuePair("pass", epwd));

		HttpResponse resp = null;
		try {
			resp = Utils.doPost("/auth/pwauth", params);
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
				JSONObject obj = null;
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
			final Handler handler, final AuthHandler ctx)
	{
		Runnable runnable = new Runnable() {
			public void run() {
				auth(name, pass, handler, ctx);
			}
		};
		return startThread(runnable);
	}
	
	static Thread doOAuth(String code, Handler handler, AuthHandler ctx)
	{
		return startThread(new OAuthRunnable(code, handler, ctx));
	}
	
	static class OAuthRunnable implements Runnable {
		private String code;
		private AuthHandler ctx;
		private Handler handler;
		
		public OAuthRunnable(String _code, Handler _handler, AuthHandler _ctx)
		{
			code = _code;
			ctx = _ctx;
			handler = _handler;
		}
		
		public void run() {
			oauth(code, handler, ctx);
		}
	}
	
	static Thread doReauth(String token, Handler handler, AuthHandler ctx)
	{
		return startThread(new ReauthRunnable(token, handler, ctx));
	}
	
	static class ReauthRunnable implements Runnable {
		private AuthHandler ctx;
		private Handler handler;
		private String token;
		
		public ReauthRunnable(String _token, Handler _handler, AuthHandler _ctx)
		{
			ctx = _ctx;
			handler = _handler;
			token = _token;
		}
		
		public void run() {
			reauth(token, handler, ctx);
		}
	}

}
