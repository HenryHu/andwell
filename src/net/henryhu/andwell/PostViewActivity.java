package net.henryhu.andwell;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class PostViewActivity extends Activity {
	private SharedPreferences pref = null;
	TextView tContent = null;
	TextView tQMD = null;
	Button bPrev = null, bNext = null, bUp = null, bDown = null;
	float target_prop;
	String basePath;
	ProgressDialog busyDialog = null;
	int post_id, post_xid;
	ArrayList<Integer> post_viewed;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pref = getSharedPreferences(Utils.PREFS_FILE, MODE_PRIVATE);
        setContentView(R.layout.postview);
        tContent = (TextView)findViewById(R.id.tContent);
        tQMD = (TextView)findViewById(R.id.tQMD);
        bPrev = (Button)findViewById(R.id.bPrev_PostView);
        bNext = (Button)findViewById(R.id.bNext_PostView);
        bUp = (Button)findViewById(R.id.bUp_PostView);
        bDown = (Button)findViewById(R.id.bDown_PostView);
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
/*        Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/WenQuanYiZenHeiMono.ttf");
        tQMD.setTypeface(tf);*/
        post_id = getIntent().getExtras().getInt("id");
        post_xid = getIntent().getExtras().getInt("xid");
        post_viewed = new ArrayList<Integer>();
        updateTitle();
        LoadPost(post_id);
    }
    
    public void updateTitle()
    {
    	setTitle(pref.getString("board", "") + " - " + post_id);
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
			int new_post_id = post_id + delta;
			if (new_post_id <= 1)
			{
				Toast toast = Toast.makeText(getApplicationContext(), 
						"You are at the start of the history", Toast.LENGTH_SHORT);
				toast.show();
			}
			else
				LoadPost(new_post_id);
		}
    };

    void LoadPost(int post_id)
    {
    	new LoadPostTask().execute(post_id);
    }

    private class LoadPostTask extends AsyncTask<Integer, Integer, Pair<String, String>> {
    	int new_post_id, new_post_xid;
    	protected Pair<String, String> doInBackground(Integer... arg)
    	{
    		RequestArgs args = new RequestArgs(pref.getString("token", ""));
    		new_post_id = arg[0];
    		args.add("id", String.valueOf(new_post_id));
    		args.add("board", pref.getString("board", "test"));
    		
    		try {
    			HttpResponse resp = Utils.doGet(basePath, "/post/view", args.getValue());
    			int respCode = resp.getStatusLine().getStatusCode();
    			if (respCode == 200)
    			{
    				JSONObject obj = null;
    				try {
    					obj = new JSONObject(Utils.readResp(resp));

    					String content = obj.getString("content");
    					new_post_xid = obj.getInt("xid"); 
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
    		showBusy("Please wait", "Loading post...");
    	}
    	
    	protected void onProgressUpdate(Integer... progress)
    	{
    		updateBusy("Loaded " + progress[0] + "%");
    	}
    	
    	protected void onPostExecute(Pair<String, String> result)
    	{
    		hideBusy();
    		checkError(result.first);
    		if (result.first.equals("OK"))
    		{
    			post_id = new_post_id;
    			post_xid = new_post_xid;
    			post_viewed.add(post_xid);
    			updateTitle();
    			SpannableStringBuilder content = new SpannableStringBuilder();
    			SpannableStringBuilder qmd = new SpannableStringBuilder();
    			parsePost(result.second, content, qmd);
    			tContent.setText(content);
    			tQMD.setText(qmd);
    		}
    		
    		Intent data = new Intent(getApplicationContext(), PostViewActivity.class);
    	    Bundle extras = new Bundle();
    	    extras.putInt("id", post_id);
    	    extras.putInt("xid", post_xid);
    	    extras.putIntegerArrayList("post_viewed", post_viewed);
    	    
    	    data.putExtras(extras);

    		setResult(RESULT_OK, data);
    	}
    }
    
    private class LoadNextPostTask extends AsyncTask<Boolean, Integer, Pair<String, Integer>> {
    	boolean forward;
    	protected Pair<String, Integer> doInBackground(Boolean... arg)
    	{
    		RequestArgs args = new RequestArgs(pref.getString("token", ""));
    		args.add("id", post_id);
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
    		showBusy("Please wait", "Loading next post...");
    	}
    	
    	protected void onProgressUpdate(Integer... progress)
    	{
    		updateBusy("Got next post id...");
    	}
    	
    	protected void onPostExecute(Pair<String, Integer> result)
    	{
    		hideBusy();
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
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.postview, menu);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.mReply_PostView:
        	doReply("S");
        	return true;
        case R.id.mRReply_PostView:
        	doReply("R");
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    public void doReply(String mode) {
    	RequestArgs args = new RequestArgs(pref.getString("token", ""));
    	args.add("id", post_id);
    	args.add("xid", post_xid);
    	args.add("board", getIntent().getExtras().getString("board"));
    	args.add("mode", mode);
    	new QuotePostTask().execute(args);
    }
    
    private class QuotePostTask extends AsyncTask<RequestArgs, String, Pair<String, Object>> {
    	RequestArgs args;
		@Override
		protected Pair<String, Object> doInBackground(RequestArgs... arg0) {
			args = arg0[0];
			try {
    			HttpResponse resp = Utils.doGet(basePath, "/post/quote", args.getValue());
    			Pair<String, String> ret = Utils.parseResult(resp);
    			if (ret.first.equals("OK")) {
    				String res = Utils.readResp(resp);
    				JSONObject obj = new JSONObject(res);
   					return new Pair<String, Object>("OK", obj);
    			}
    			return new Pair<String, Object>(ret.first, ret.second);
    		}
    		catch (IOException e)
    		{
    			return new Pair<String, Object>("IOException " + e.getMessage(), "");
    		} catch (JSONException e) {
    			return new Pair<String, Object>("JSON parse error: ", e.getMessage());
			}
		}
		protected void onPreExecute() {
			showBusy("Please wait...", "Quoting post...");
		}
		protected void onPostExecute(Pair<String, Object> result)
    	{
			hideBusy();
    		if (result.first.equals("OK"))
    		{
    	    	try {
    	    		JSONObject obj = (JSONObject)result.second;
    	    		Intent intent = new Intent(getApplicationContext(), NewPostActivity.class);
    	    		intent.putExtra("board", getIntent().getExtras().getString("board"));
    	    		intent.putExtra("re_id", args.getInt("id"));
    	    		intent.putExtra("re_xid", args.getInt("xid"));
    	    		intent.putExtra("title", obj.getString("title"));
    	    		intent.putExtra("content", 
       	    			((args.getString("mode").equals("R") ? "" : 
  	    				"\nSent from AndWell\n") + obj.getString("content")));
    	    		startActivity(intent);
//    	    		startActivityForResult(intent, ACTION_REPLY);
    	    	} catch (JSONException e) {
    	    		showMsg("illegal reply from server");
    	    	}    			
    		} else {
   				showMsg(result.first + ": " + result.second.toString());
    		}
    	}
    }
    
    void showMsg(String msg) {
    	Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }
    

    void showBusy(String title, String msg) {
    	if (busyDialog != null)
    		busyDialog.dismiss();
    	
		busyDialog = ProgressDialog.show(this, title, msg);
    }
    
    void updateBusy(String msg) {
		if (busyDialog != null)
			busyDialog.setMessage(msg);
    }
    
    void hideBusy() {
		if (busyDialog != null) {
			busyDialog.dismiss();
			busyDialog = null;
		}
    }
    
    @Override
    public void onDestroy()
    {
    	super.onDestroy();
    	busyDialog = null;
    }
}
