package org.net9.andwell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class PostViewActivity extends Activity {
	private Activity myAct = null;
	private SharedPreferences pref = null;
	ProgressDialog loadDialog;
	TextView tContent = null;
	TextView tQMD = null;
	ImageButton bPrev = null, bNext = null, bUp = null, bDown = null;
	float target_prop;
	String basePath;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myAct = this;
        pref = getSharedPreferences(Utils.PREFS_FILE, MODE_PRIVATE);
        setContentView(R.layout.postview);
        tContent = (TextView)findViewById(R.id.tContent);
        tQMD = (TextView)findViewById(R.id.tQMD);
        bPrev = (ImageButton)findViewById(R.id.bPrev_PostView);
        bNext = (ImageButton)findViewById(R.id.bNext_PostView);
        bUp = (ImageButton)findViewById(R.id.bUp_PostView);
        bDown = (ImageButton)findViewById(R.id.bDown_PostView);
        bUp.setOnClickListener(new ButtonClickListener(true));
        bDown.setOnClickListener(new ButtonClickListener(false));
        bPrev.setOnClickListener(new ThreadClickListener(false));
        bNext.setOnClickListener(new ThreadClickListener(true));
		basePath = pref.getString("server_api", "");
        
        TextPaint tp = tQMD.getPaint();
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<80; i++)
        	sb.append(" ");
        tQMD.setMaxWidth((int) tp.measureText(sb.toString()));
        float space_width = tp.measureText(" ");
        float chn_width = tp.measureText("一");
        target_prop = 2 / (chn_width / space_width);
        Log.d("target_prop: ", String.valueOf(target_prop));

        updateTitle();
        

/*        Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/WenQuanYiZenHeiMono.ttf");
        tQMD.setTypeface(tf);*/
        LoadPost(pref.getInt("post_id", 0));
    }
    
    public void updateTitle()
    {
    	setTitle(pref.getString("board", "") + " - " + pref.getInt("post_id", 0));
    }
    
    class ThreadClickListener implements OnClickListener {
    	private boolean _forward;
    	ThreadClickListener(boolean forward)
    	{
    		_forward = forward;
    	}
    	
    	@Override
		public void onClick(View arg0) {
    		new LoadNextPostTask().execute(_forward);
    	}
    }

    class ButtonClickListener implements OnClickListener {
    	private boolean _up;
    	ButtonClickListener(boolean up)
    	{
    		_up = up;
    	}
		@Override
		public void onClick(View arg0) {
			int delta = 0;
			if (_up)
				delta = -1;
			else
				delta = 1;
			int post_id = pref.getInt("post_id", 0) + delta;
			if (post_id <= 1)
			{
				Toast toast = Toast.makeText(getApplicationContext(), 
						"You are at the start of the history", Toast.LENGTH_SHORT);
				toast.show();
			}
			else
				LoadPost(post_id);
		}
    };

    void LoadPost(int post_id)
    {
    	new LoadPostTask().execute(post_id);
    }

    private class LoadPostTask extends AsyncTask<Integer, Integer, Pair<String, String>> {
    	int post_id;
    	protected Pair<String, String> doInBackground(Integer... arg)
    	{
    		RequestArgs args = new RequestArgs(pref.getString("token", ""));
    		post_id = arg[0];
    		args.add("id", String.valueOf(post_id));
    		args.add("board", pref.getString("board", "test"));
    		
    		try {
    			HttpResponse resp = Utils.doGet(basePath, "/post/view", args.getValue());
    			int respCode = resp.getStatusLine().getStatusCode();
    			if (respCode == 200)
    			{
    				BufferedReader br = new BufferedReader(
    						new InputStreamReader(resp.getEntity().getContent()));
    				StringBuilder sb = new StringBuilder(1024);
    				char[] buf = new char[1024];
    				int nread = 0;
    				while ((nread = br.read(buf)) != -1)
    				{
    					sb.append(buf, 0, nread);
    				}
    				br.close();
    				
    				JSONObject obj = null;
    				try {
    					obj = new JSONObject(sb.toString());
    					String content = obj.getString("content");
    					return new Pair<String, String>("OK", content);
    				} catch (JSONException e)
    				{
    					return new Pair<String, String>("JSON parse error: " + e.getMessage(), "");
    				}
    			} else {
    				if (respCode == 416)
    					return new Pair<String, String>("OUT OF RANGE", "");
    				else
    					return new Pair<String, String>(resp.getStatusLine().getReasonPhrase(), "");
    			}
    		}
    		catch (IOException e)
    		{
    			return new Pair<String, String>("IOException " + e.getMessage(), "");
    		}
    	}
    	
    	protected void onPreExecute()
    	{
    		loadDialog = ProgressDialog.show(myAct, "Please wait", "Loading post...");
    	}
    	
    	protected void onProgressUpdate(Integer... progress)
    	{
    		if (loadDialog != null)
    			loadDialog.setMessage("Loaded " + progress[0] + "%");
    	}
    	
    	protected void onPostExecute(Pair<String, String> result)
    	{
    		if (loadDialog != null)
    			loadDialog.dismiss();
    		checkError(result.first);
    		if (result.first.equals("OK"))
    		{
    			pref.edit().putInt("post_id", post_id).commit();
    			updateTitle();
    			SpannableStringBuilder content = new SpannableStringBuilder();
    			SpannableStringBuilder qmd = new SpannableStringBuilder();
    			parsePost(result.second, content, qmd);
    			tContent.setText(content);
    			tQMD.setText(qmd);
    		}
    	}
    }
    
    private class LoadNextPostTask extends AsyncTask<Boolean, Integer, Pair<String, Integer>> {
    	boolean forward;
    	protected Pair<String, Integer> doInBackground(Boolean... arg)
    	{
    		RequestArgs args = new RequestArgs(pref.getString("token", ""));
    		args.add("id", String.valueOf(pref.getInt("post_id", 0)));
    		args.add("board", pref.getString("board", "test"));
    		forward = arg[0];
    		if (!forward)
    			args.add("direction", "backward");
    		
    		try {
    			HttpResponse resp = Utils.doGet(basePath, "/post/nextid", args.getValue());
    			int retCode = resp.getStatusLine().getStatusCode();
    			if (retCode == 200)
    			{
    				String reply = Utils.readAll(resp.getEntity().getContent());
    				
    				JSONObject obj = null;
    				try {
    					obj = new JSONObject(reply);
    					int nextid = obj.getInt("nextid");
    					publishProgress();
    					return new Pair<String, Integer>("OK", nextid);
    				} catch (JSONException e)
    				{
    					return new Pair<String, Integer>("JSON parse error: " + e.getMessage(), 0);
    				}
    			} else {
    				if (retCode == 404)
    					return new Pair<String, Integer>("NO MORE", 0);
    				else if (retCode == 416)
    					return new Pair<String, Integer>("OUT OF RANGE", 0);
    				else
    					return new Pair<String, Integer>(resp.getStatusLine().getReasonPhrase(), 0);
    			}
    		}
    		catch (IOException e)
    		{
    			return new Pair<String, Integer>("IOException " + e.getMessage(), 0);
    		}
    	}
    	
    	protected void onPreExecute()
    	{
    		loadDialog = ProgressDialog.show(myAct, "Please wait", "Loading next post...");
    	}
    	
    	protected void onProgressUpdate(Integer... progress)
    	{
    		if (loadDialog != null)
    			loadDialog.setMessage("Got next post id...");
    	}
    	
    	protected void onPostExecute(Pair<String, Integer> result)
    	{
    		if (loadDialog != null)
    			loadDialog.dismiss();
    		if (result.first.equals("NO MORE"))
    		{
    			String msg;
    			if (forward)
    				msg = "You are at the tail of the thread";
    			else
    				msg = "You are at the head of the thread";
    			Toast toast = Toast.makeText(getApplicationContext(), 
    					msg, Toast.LENGTH_SHORT);
    			toast.show();
    		} else {
    			checkError(result.first);
    			if (result.first.equals("OK"))
    			{
    				LoadPost(result.second);
    			}
    		}
    	}
    }
    
    void checkError(String result)
    {
    	if (result.equals("OK"))
		{
		} else if (result.equals("OUT OF RANGE"))
		{
			String errMsg = "no more post";
			Toast toast = Toast.makeText(getApplicationContext(), 
					errMsg, Toast.LENGTH_SHORT);
			toast.show();
		} else {
			String errMsg;
				errMsg = "failed to load post: " + result;
			Toast toast = Toast.makeText(getApplicationContext(), 
					errMsg, Toast.LENGTH_SHORT);
			toast.show();
		}
    }
    
    public void parsePost(String orig, SpannableStringBuilder content, SpannableStringBuilder qmd)
    {
    	String o_content, o_qmd;
    	int qmdStart = orig.lastIndexOf("\n--\n");
    	if (qmdStart != -1)
    	{
    		o_content = orig.substring(0, qmdStart);
    		o_qmd = orig.substring(qmdStart);
    	} else {
    		o_content = orig;
    		o_qmd = "";
    	}
    	content.append(new TermFormatter().parseFormat(o_content, false));
    	qmd.append(new TermFormatter().parseFormat(o_qmd, true));
//    	Log.d("QMD Start: ", String.valueOf(qmdStart));
    }
    
    @Override
    public void onDestroy()
    {
    	super.onDestroy();
    	loadDialog = null;
    }
}
