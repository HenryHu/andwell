package net.henryhu.andwell;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.util.Log;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.TextView;

public class PostViewFragment extends Fragment {
	private SharedPreferences pref = null;
	TextView tContent = null;
	TextView tQMD = null;
	Button bPrev = null, bNext = null, bUp = null, bDown = null;
	ScrollView sPost;
	HorizontalScrollView sQmd;
	String basePath;
	ProgressDialog busyDialog = null;
	int post_id, post_xid;
	String board;
	ArrayList<Integer> post_viewed;
	GestureDetector gestures;
	float FLING_MIN_DIST = 0.1f;
	float FLING_MIN_SPEED = 0.1f;
	Activity myAct = null;
	PostViewListener listener = null;
	
	interface PostViewListener {
		public void onPostView(int post_id, int post_xid);
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (PostViewListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement PostViewListener");
        }
    }
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myAct = getActivity();
        pref = myAct.getSharedPreferences(Utils.PREFS_FILE, Context.MODE_PRIVATE);
        setHasOptionsMenu(true);
       
		basePath = pref.getString("server_api", "");
		gestures = new GestureDetector(myAct, new SimpleOnGestureListener() {
			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2,
					float velocityX, float velocityY) {
				float width = tContent.getWidth();
				if (e1 == null) {
					Log.e("onFling", "e1 null");
					return false;
				}
				if (e2 == null) {
					Log.e("onFling", "e2 null");
					return false;
				}
				Log.d("PostView.onFling", 
						String.format("e1.X: %f e2.X: %f vX: %f vY: %f width: %f", 
								e1.getX(), e2.getX(), velocityX, velocityY, width));
				if (Math.abs(e1.getX() - e2.getX()) / width > FLING_MIN_DIST
						&& Math.abs(velocityX) / width > FLING_MIN_SPEED) {
					if (e2.getX() > e1.getX()) {
						switchPost(true);
					} else {
						switchPost(false);
					}
					return true;
				}
				return false;
			}

			public void onLongPress(MotionEvent e) {
				
			}
		});
		
        post_id = getArguments().getInt("id");
        post_xid = getArguments().getInt("xid");
    	board = getArguments().getString("board");
        post_viewed = new ArrayList<Integer>();
        listener.onPostView(post_id, post_xid);
    }
    
    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	if (container == null)
    		return null;
    	View contentView = inflater.inflate(R.layout.postview, container, false);
    	tContent = (TextView)contentView.findViewById(R.id.tContent);
    	tQMD = (TextView)contentView.findViewById(R.id.tQMD);
    	bPrev = (Button)contentView.findViewById(R.id.bPrev_PostView);
    	bNext = (Button)contentView.findViewById(R.id.bNext_PostView);
    	bUp = (Button)contentView.findViewById(R.id.bUp_PostView);
    	bDown = (Button)contentView.findViewById(R.id.bDown_PostView);
    	sPost = (ScrollView)contentView.findViewById(R.id.scrollPost_PostView);
    	sQmd = (HorizontalScrollView)contentView.findViewById(R.id.sQmd_PostView);
    	bUp.setOnClickListener(new ButtonClickListener(true));
    	bDown.setOnClickListener(new ButtonClickListener(false));
    	bPrev.setOnClickListener(new ThreadClickListener(false));
    	bNext.setOnClickListener(new ThreadClickListener(true));
    	
    	OnTouchListener detectGesture = new OnTouchListener() {

			public boolean onTouch(View v, MotionEvent event) {
		    	return gestures.onTouchEvent(event);
			}
			
		};
    	tContent.setOnTouchListener(detectGesture);
		sPost.setOnTouchListener(detectGesture);
		
        TextPaint tp = tQMD.getPaint();
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<80; i++)
        	sb.append(" ");
        tQMD.setMaxWidth((int) tp.measureText(sb.toString()));
        LoadPost(post_id);

    	return contentView;
    }
    
    class ThreadClickListener implements OnClickListener {
    	private boolean _forward, _last_one, _only_new;
    	ThreadClickListener(boolean forward) {
    		this(forward, false, false);
    	}

    	ThreadClickListener(boolean forward, boolean last_one, boolean only_new)
    	{
    		_forward = forward;
    		_last_one = last_one;
    		_only_new = only_new;
    	}
    	
		public void onClick(View arg0) {
    		new LoadNextPostTask().execute(_forward, _last_one, _only_new);
    	}
    }
    
    void switchPost(boolean up) {
		int delta = 0;
		if (up)
			delta = -1;
		else
			delta = 1;
		int new_post_id = post_id + delta;
		if (new_post_id <= 1)
		{
			Utils.showToast(myAct, getString(R.string.already_first_post));
		}
		else
			LoadPost(new_post_id);

    }

    class ButtonClickListener implements OnClickListener {
    	private boolean _up;
    	ButtonClickListener(boolean up)
    	{
    		_up = up;
    	}
		public void onClick(View arg0) {
			switchPost(_up);
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
    		args.add("board", board);
    		
    		try {
    			HttpResponse resp = Utils.doGet(basePath, "/post/view", args.getValue());
    			Pair<String, String> result = Utils.parseResult(resp);
    			if (result.first.equals("OK"))
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
    				return result;
    			}
    		}
    		catch (IOException e)
    		{
    			return new Pair<String, String>("IOException " + e.getMessage(), "");
    		}
    	}
    	
    	protected void onPreExecute()
    	{
    		Log.d("PostViewFragment", "LoadPost: PreExec");
    		showBusy("Please wait", "Loading post...");
    	}
    	
    	protected void onProgressUpdate(Integer... progress)
    	{
    		updateBusy("Loaded " + progress[0] + "%");
    	}
    	
    	protected void onPostExecute(Pair<String, String> result)
    	{
    		Log.d("PostViewFragment", "LoadPost: PostExec");
    		hideBusy();
    		checkError(result.first);
    		if (result.first.equals("OK"))
    		{
    			post_id = new_post_id;
    			post_xid = new_post_xid;
    			post_viewed.add(post_xid);
    			listener.onPostView(post_id, post_xid);
    			SpannableStringBuilder content = new SpannableStringBuilder();
    			SpannableStringBuilder qmd = new SpannableStringBuilder();
    			parsePost(result.second, content, qmd, tContent.getPaint(), tQMD.getPaint());
    			tContent.setText(content);
    			tQMD.setText(qmd);
    		}
    		updateResult();
    	}
    }
    
    private void updateResult() {
    	Intent data = new Intent(myAct, PostViewActivity.class);
    	Bundle extras = new Bundle();
    	extras.putInt("id", post_id);
    	extras.putInt("xid", post_xid);
    	extras.putIntegerArrayList("post_viewed", post_viewed);

    	data.putExtras(extras);

    	myAct.setResult(Activity.RESULT_OK, data);
    }
    
    private class LoadNextPostTask extends AsyncTask<Boolean, Integer, Pair<String, Integer>> {
    	boolean forward, last_one, only_new;
    	protected Pair<String, Integer> doInBackground(Boolean... arg)
    	{
    		RequestArgs args = new RequestArgs(pref.getString("token", ""));
    		args.add("id", post_id);
    		args.add("board", board);
    		forward = arg[0];
    		last_one = arg[1];
    		only_new = arg[2];
    		if (!forward)
    			args.add("direction", "backward");
    		if (last_one)
    			args.add("last_one", 1);
    		if (only_new)
    			args.add("only_new", 1);
    		
    		try {
    			HttpResponse resp = Utils.doGet(basePath, "/post/nextid", args.getValue());
    			Pair<String, String> result = Utils.parseResult(resp);
    			if (result.first.equals("OK"))
    			{
    				String reply = Utils.readResp(resp);
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
   					return new Pair<String, Integer>(result.first, 0);
    			}
    		}
    		catch (IOException e)
    		{
    			return new Pair<String, Integer>("IOException " + e.getMessage(), 0);
    		}
    	}
    	
    	protected void onPreExecute()
    	{
    		showBusy(getString(R.string.please_wait), getString(R.string.loading_next_post));
    	}
    	
    	protected void onProgressUpdate(Integer... progress)
    	{
    		updateBusy(getString(R.string.got_next_post_id));
    	}
    	
    	protected void onPostExecute(Pair<String, Integer> result)
    	{
    		hideBusy();
    		if (result.first.equals("NO MORE"))
    		{
    			if (only_new && !forward) {
    				// maybe forward would be ok...
    				new LoadNextPostTask().execute(true, last_one, only_new);
    				return;
    			}
    			String msg;
    			if (only_new)
    				msg = getString(R.string.no_new_post);
    			else if (forward)
    				msg = getString(R.string.at_thread_tail);
    			else
    				msg = getString(R.string.at_thread_head);
    			Utils.showToast(myAct, msg);
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
			String errMsg = getString(R.string.no_more_post);
			Utils.showToast(myAct, errMsg);
		} else {
			String errMsg = getString(R.string.fail_to_load_post) + result;
			Utils.showToast(myAct, errMsg);
		}
    }
    
    public void parsePost(String orig, SpannableStringBuilder content, SpannableStringBuilder qmd, Paint pContent, Paint pQmd)
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
    	
    	content.append(new TermFormatter().parseFormat(o_content, false, pContent));
    	qmd.append(new TermFormatter().parseFormat(o_qmd, true, pQmd));
//    	Log.d("QMD Start: ", String.valueOf(qmdStart));
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
    	inflater.inflate(R.menu.postview, menu);
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
        	return true;
        case R.id.mThreadFirst_PostView:
        	new LoadNextPostTask().execute(false, true, false);
        	return true;
        case R.id.mThreadFirstNew_PostView:
        	new LoadNextPostTask().execute(false, true, true);
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    public void doReply(String mode) {
    	RequestArgs args = new RequestArgs(pref.getString("token", ""));
    	args.add("id", post_id);
    	args.add("xid", post_xid);
    	args.add("board", board);
    	args.add("mode", mode);
    	new QuotePostTask().execute(args);
    }
    
    public int getPostId() { return post_id; }
    public int getPostXid() { return post_xid; }
    
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
			showBusy(getString(R.string.please_wait), getString(R.string.quoting_post));
		}
		protected void onPostExecute(Pair<String, Object> result)
    	{
			hideBusy();
    		if (result.first.equals("OK"))
    		{
    	    	try {
    	    		JSONObject obj = (JSONObject)result.second;
    	    		Intent intent = new Intent(myAct, NewPostActivity.class);
    	    		intent.putExtra("board", board);
    	    		intent.putExtra("re_id", args.getInt("id"));
    	    		intent.putExtra("re_xid", args.getInt("xid"));
    	    		intent.putExtra("title", obj.getString("title"));
    	    		intent.putExtra("content", 
       	    			((args.getString("mode").equals("R") ? "" : 
  	    				"\nSent from AndWell\n") + obj.getString("content")));
    	    		startActivity(intent);
//    	    		startActivityForResult(intent, ACTION_REPLY);
    	    	} catch (JSONException e) {
    	    		Utils.showToast(myAct, getString(R.string.illegal_reply));
    	    	}    			
    		} else {
   				Utils.showToast(myAct, getString(R.string.error_with_msg) + result.second.toString());
    		}
    	}
    }
    
    void showBusy(String title, String msg) {
    	if (busyDialog != null) {
    		busyDialog.dismiss();
    	}
		busyDialog = ProgressDialog.show(myAct, title, msg);
    }
    
    void updateBusy(String msg) {
		if (busyDialog != null)
			busyDialog.setMessage(msg);
    }
    
    void hideBusy() {
		if (busyDialog != null) {
			busyDialog.dismiss();
			busyDialog = null;
		} else {
		}
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	if (busyDialog != null) {
    		busyDialog.dismiss();
    		busyDialog = null;
    	}
    }
    
    @Override
    public void onDestroy()
    {
    	super.onDestroy();
    }
}
