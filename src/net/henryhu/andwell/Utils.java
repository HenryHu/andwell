package net.henryhu.andwell;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Utils {
	public static final String PREFS_FILE = "MainPref";
	public static final int soTimeoutMs = 10000;
	public static final int connTimeoutMs = 20000;
	public static boolean debug = false;
	
//	static Object clientLock = new Object();
//	static HttpClient client = null;

	public static String getOAuthRedirectURI(String basePath)
	{
		return "andwell://andwell/oauth_redirect";
	}

	/*
	public static HttpClient getNewHttpClient() {
		HttpClient result;
	    try {
	        HttpParams params = new BasicHttpParams();
		    HttpConnectionParams.setConnectionTimeout(params, connTimeoutMs);
		    HttpConnectionParams.setSoTimeout(params, soTimeoutMs);

	        SchemeRegistry registry = new SchemeRegistry();
	        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
	        registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 8080));

	        ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

	        result = new DefaultHttpClient(ccm, params);
	    } catch (Exception e) {
	        result = new DefaultHttpClient();
	    }
	    return result;
	}
	*/

	public static HttpURLConnection doGet(String basePath, String path, RequestArgs params)
	throws IOException 
	{
		String args = params.getEncodedForm();

		if (debug) {
			Log.d("get path", basePath + path + "?" + args);
		}

		URL url = new URL(basePath + path + "?" + args);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(connTimeoutMs);
		conn.setReadTimeout(soTimeoutMs);

		conn.connect();
		return conn;

		/*
		HttpGet get = new HttpGet(basePath + path + "?" + args);
		

		HttpResponse resp;
		synchronized(clientLock) {
			if (client == null) {
				client = getNewHttpClient();
			}
			resp = client.execute(get);
		}
		return resp;
		*/
	}

	public static HttpURLConnection doPost(String basePath, String path, RequestArgs params)
	throws IOException 
	{
		String args = params.getEncodedForm();

		URL url = new URL(basePath + path);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(connTimeoutMs);
		conn.setReadTimeout(soTimeoutMs);

		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setDoOutput(true);
		conn.setFixedLengthStreamingMode(args.length());
		OutputStreamWriter osw = new OutputStreamWriter(new BufferedOutputStream(conn.getOutputStream()));
		osw.write(args);

		conn.connect();
		return conn;

		/*
		HttpPost post = new HttpPost(basePath + path);
		StringEntity ent = null;
		try {
			ent = new StringEntity(URLEncodedUtils.format(params, "UTF-8"));
		}
		catch (UnsupportedEncodingException e)	{ }
		ent.setContentType(URLEncodedUtils.CONTENT_TYPE);
		post.setEntity(ent);
		HttpResponse resp;
		synchronized(clientLock) {
			if (client == null) {
				client = getNewHttpClient();
			}

			resp = client.execute(post);
		}
		return resp;
		*/
	}
	
	public static String fillTo(String orig, int target)
	{
		return String.format("%" + String.valueOf(target) + "s", orig);
	}
	
	public static String readAll(InputStream is) throws IOException
	{
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			StringBuilder sb = new StringBuilder(1024);
			char[] buf = new char[1024];
			int nread;
			while ((nread = br.read(buf)) != -1)
			{
				sb.append(buf, 0, nread);
			}
			br.close();
			if (debug)
				Log.d("ReadAll", "ret: " + sb.toString());
			return sb.toString();
		} finally {
			is.close();
		}
	}
	
	public static void checkResult(HttpURLConnection resp)
			throws IOException, NotFoundException, OutOfRangeException, ServerErrorException {
		switch (resp.getResponseCode()) {
			case 200: return;
			case 404: throw new NotFoundException(resp.getResponseMessage());
			case 416: throw new OutOfRangeException(resp.getResponseMessage());
			default: throw new ServerErrorException(resp.getResponseMessage());
		}
	}
	
	public static String readResp(HttpURLConnection resp) throws IllegalStateException, IOException {
		return readAll(resp.getInputStream());
	}
	
	public static void showToast(Context context, String message) {
		Toast toast = Toast.makeText(context,
				message,
				Toast.LENGTH_LONG);
		toast.show();
	}
	
	public static boolean useDualPane(Context context, double inchLeft, double inchRight) {
		DisplayMetrics dm = new DisplayMetrics();
	    WindowManager wm = (WindowManager)(context.getSystemService(Context.WINDOW_SERVICE));
	    wm.getDefaultDisplay().getMetrics(dm);
	    double x = dm.widthPixels/dm.xdpi;
	    // XXX: shall we depend on the real requirements of both panes?
	    // XXX: what the hell is this magic number?
	    // TODO: automatic resize
	    return (x >= inchLeft + inchRight);
//	    double y = dm.heightPixels/dm.ydpi;
	}

	public static JSONObject getJsonResp(String basePath, String action, RequestArgs params)
			throws IOException, OutOfRangeException, NotFoundException, ServerErrorException, JSONException {
		HttpURLConnection conn = doGet(basePath, action, params);
		checkResult(conn);
		return new JSONObject(readResp(conn));
	}

	public static JSONArray getJsonArrayResp(String basePath, String action, RequestArgs params)
			throws IOException, OutOfRangeException, NotFoundException, ServerErrorException, JSONException {
		HttpURLConnection conn = doGet(basePath, action, params);
		checkResult(conn);
		return new JSONArray(readResp(conn));
	}
}
