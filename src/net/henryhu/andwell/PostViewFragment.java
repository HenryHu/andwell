package net.henryhu.andwell;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
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
	BusyDialog busy;
	int post_id, post_xid;
	String board;
	String token;
	GestureDetector gestures;
	float FLING_MIN_DIST = 0.1f;
	float FLING_MIN_SPEED = 0.1f;
	Activity myAct = null;
	PostViewListener listener = null;
	static final int ACTION_REPLY = 1;
	
	interface PostViewListener {
		public void onPostView(int post_id, int post_xid);
		public void onPostReply();
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
        busy = new BusyDialog(myAct);
        pref = myAct.getSharedPreferences(Utils.PREFS_FILE, Context.MODE_PRIVATE);
        setHasOptionsMenu(true);
       
		basePath = pref.getString("server_api", "");
		token = pref.getString("token", "");
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
    		@Override
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
    	
    	@Override
		public void onClick(View arg0) {
    		new LoadNextPostTask(new MyLoadNextPostListener()).execute(new LoadNextPostArg(token, basePath, board, post_id, _forward, _last_one, _only_new));
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
    
    class MyLoadNextPostListener extends LoadNextPostListener {
    	@Override
    	protected void onPreExecute()
    	{
    		busy.show(getString(R.string.please_wait), getString(R.string.loading_next_post));
    	}
    	
    	@Override
    	protected void onProgressUpdate(Integer progress)
    	{
    		busy.update(getString(R.string.got_next_post_id));
    	}
    	
    	@Override
    	protected void onPostExecute(LoadNextPostArg arg, LoadNextPostResult result)
    	{
    		busy.hide();
    		LoadPost(result.next_id);
    	}
    	
    	@Override
    	protected void onException(LoadNextPostArg arg, Exception e) {
    		busy.hide();
			String msg;
    		if (e instanceof NotFoundException) {
    			if (arg.only_new && !arg.forward) {
    				// maybe forward would be ok...
    				new LoadNextPostTask(new MyLoadNextPostListener()).execute(new LoadNextPostArg(token, basePath, board, post_id, true, arg.last_one, arg.only_new));
    				return;
    			}
    			if (arg.only_new)
    				msg = getString(R.string.no_new_post);
    			else if (arg.forward)
    				msg = getString(R.string.at_thread_tail);
    			else
    				msg = getString(R.string.at_thread_head);
    		} else {
    			msg = Exceptions.getErrorMsg(e); 
    		}
			Utils.showToast(myAct, msg);
    	}
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

    void LoadPost(int post_id_to_load)
    {
    	new LoadPostTask(new LoadPostListener() {
    		@Override
    		protected void onPreExecute()
    		{
    			Log.d("PostViewFragment", "LoadPost: PreExec");
    			busy.show(getString(R.string.please_wait), getString(R.string.loading_post));
    		}
    		
    		@Override
    		protected void onProgressUpdate(Integer progress)
    		{
    			busy.update("Loaded " + String.valueOf(progress) + "%");
    		}

    		@Override
    		protected void onPostExecute(LoadPostArg arg, LoadPostResult result)
    		{
    			Log.d("PostViewFragment", "LoadPost: PostExec");
    			busy.hide();
    			post_id = arg.new_post_id;
    			post_xid = result.new_post_xid;
    			listener.onPostView(post_id, post_xid);
    			SpannableStringBuilder content = new SpannableStringBuilder();
    			SpannableStringBuilder qmd = new SpannableStringBuilder();
    			parsePost(result.content, content, qmd, tContent.getPaint(), tQMD.getPaint());
    			tContent.setText(content);
    			tQMD.setText(qmd);
    		}
    		
    		@Override
    		protected void onException(LoadPostArg arg, Exception e) {
    			busy.hide();
    			showError(e);
    		}

    	}).execute(new LoadPostArg(basePath, token, board, post_id_to_load));
    }

    void showError(Exception e)
    {
    	String errMsg;
    	if (e instanceof OutOfRangeException) {
			errMsg = getString(R.string.no_more_post);
		} else {
			errMsg = getString(R.string.fail_to_load_post) + Exceptions.getErrorMsg(e);
		}
		Utils.showToast(myAct, errMsg);
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
        	new LoadNextPostTask(new MyLoadNextPostListener())
        		.execute(new LoadNextPostArg(token, basePath, board, post_id, false, true, false));
        	return true;
        case R.id.mThreadFirstNew_PostView:
        	new LoadNextPostTask(new MyLoadNextPostListener())
        		.execute(new LoadNextPostArg(token, basePath, board, post_id, false, true, true));
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    public void doReply(String mode) {
    	RequestArgs args = new RequestArgs(token);
    	args.add("id", post_id);
    	args.add("xid", post_xid);
    	args.add("board", board);
    	args.add("mode", mode);
    	new QuotePostTask(new MyQuotePostListener(this, busy, ACTION_REPLY)).execute(new QuotePostArg(basePath, args));
    }
    
    public int getPostId() { return post_id; }
    public int getPostXid() { return post_xid; }
    
    @Override
    public void onPause() {
    	super.onPause();
    }
    
    @Override
    public void onDestroy()
    {
    	super.onDestroy();
    	busy.hide();
    }
    
    @Override
    public void onActivityResult(int request, int result, Intent data) {
    	if (request == ACTION_REPLY)
    		if (result == Activity.RESULT_OK)
    			listener.onPostReply();
    }
}
