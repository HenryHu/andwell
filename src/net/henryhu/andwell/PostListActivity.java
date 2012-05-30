package net.henryhu.andwell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import net.henryhu.andwell.R;

import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class PostListActivity extends ListActivity {
	private Activity myAct = null;
	private SharedPreferences pref = null;
	List<PostItem> postslist = new ArrayList<PostItem>();
	ProgressDialog busyDialog = null;
	ArrayAdapter<PostItem> adapter;
	static final int INPUT_DIALOG_ID = 0;
	public static final int ACTION_POST = 1;
	public static final int ACTION_REPLY = 2;
	public static final int ACTION_VIEW = 3;
	Dialog inputDialog = null;
	EditText tValue = null;
	String basePath;
	boolean loaded = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myAct = this;
        pref = getSharedPreferences(Utils.PREFS_FILE, MODE_PRIVATE);
        adapter = new ArrayAdapter<PostItem>(this, R.layout.postlist, postslist);
        setListAdapter(adapter);
        
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
		basePath = pref.getString("server_api", "");
        
        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                int position, long id) {
              // When clicked, show a toast with the TextView text
            	PostItem item = postslist.get(position);
            	if (item.id() == PostItem.ID_MORE)
            	{
            		int end = postslist.get(position - 1).id() - 1;
            		if (end == 0)
            		{
            			Toast toast = Toast.makeText(getApplicationContext(),
            					"You are at the start of the history.",
            					Toast.LENGTH_SHORT);
            			toast.show();
            		} else {
            			postslist.remove(position);
                		new LoadPostsTask().execute(0, 20, end);
            		}
            	} else if (item.id() == PostItem.ID_UPDATE)
            	{
            		if (postslist.size() >= 3)
            			new LoadPostsTask().execute(postslist.get(1).id() + 1, 20, 0, 0);
            		else
            			loadPosts(0, 20, 0, 0);
            	} else {
            		Intent intent = new Intent(myAct, PostViewActivity.class);
            		intent.putExtra("id", item.id());
            		intent.putExtra("xid", item.xid());
            		intent.putExtra("board", pref.getString("board", ""));
            		postslist.get(position).setRead(true);
            		adapter.notifyDataSetChanged();
            		startActivityForResult(intent, ACTION_VIEW);
            	}
            }
          });
        
        registerForContextMenu(getListView());
       	loaded = false;
    }
    
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        switch(id) {
        case INPUT_DIALOG_ID:
        	dialog = createInputDialog("Please input post ID", "", "");
        	inputDialog = dialog;
            break;
        default:
            dialog = null;
        }
        return dialog;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.postlistmenu, menu);
    	return true;
    }
    
    Dialog createInputDialog(String title, String prompt, String initval)
    {
    	Dialog dialog = new Dialog(this);


    	dialog.setContentView(R.layout.inputdialog);
    	dialog.setTitle(title);

    	TextView text = (TextView) dialog.findViewById(R.id.tPrompt_inputdialog);
    	text.setText(prompt);
    	tValue = (EditText) dialog.findViewById(R.id.tValue_inputdialog);
    	tValue.setText(initval);
    	Button bOK = (Button) dialog.findViewById(R.id.bOK_inputdialog);
    	Button bCancel = (Button) dialog.findViewById(R.id.bCancel_inputdialog);
    	bOK.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				int postid = Integer.parseInt(tValue.getText().toString());
				if (postid < 1)
					postid = 1;
				int startid = postid - 10;
				if (startid < 1)
					startid = 1;
				
				loadPosts(startid, 20, 0, postid);
				dismissDialog(INPUT_DIALOG_ID);
			}
    	});
    	bCancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				dismissDialog(INPUT_DIALOG_ID);
			}
    	});

    	
    	return dialog;
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.postlist_context, menu);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.mReply_PostList:
            	doReply(info.id, false);
                return true;
            case R.id.mRModeReply_PostList:
                doReply(info.id, true);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.mRefreshAll_PostList:
            loadPosts(0, 20, 0, 0);
            return true;
        case R.id.mJumpTo_PostList:
        	showDialog(INPUT_DIALOG_ID);
        	return true;
        case R.id.mPostNew_PostList:
        	newPost();
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    void newPost() {
    	Intent intent = new Intent(getApplicationContext(), NewPostActivity.class);
    	intent.putExtra("board", pref.getString("board", "test"));
    	startActivityForResult(intent, ACTION_POST);
    }
    
    void loadPosts(int start, int count, int end, int selectid)
    {
    	loaded = true;
    	postslist.clear();
        postslist.add(new PostItem(PostItem.ID_UPDATE));
    	new LoadPostsTask().execute(start, count, end, -1, selectid);
    }

    private class LoadPostsTask extends AsyncTask<Integer, Integer, String> {
    	private String errMsg = "";
    	private int selectid = 0;
    	protected String doInBackground(Integer... arg)
    	{
    		RequestArgs args = new RequestArgs(pref.getString("token", ""));
    		int insertpos = -1; // -1: tail 0:head
    		args.add("name", pref.getString("board", "test"));
    		if (arg.length > 0)
    			args.add("start", String.valueOf(arg[0]));
    		if (arg.length > 1)
    			if (!arg[1].equals(0))
    			args.add("count", String.valueOf(arg[1]));
    		if (arg.length > 2)
    			if (!arg[2].equals(0))
    				args.add("end", String.valueOf(arg[2]));
    		if (arg.length > 3)
    			insertpos = arg[3];
    		if (arg.length > 4)
    			selectid = arg[4];
    		
    		if (insertpos == 0)
    			errMsg = "no more post";
    		
    		try {
    			HttpResponse resp = Utils.doGet(basePath, "/board/post_list", args.getValue());
    			Pair<String, String> result = Utils.parseResult(resp);
    			if (result.first.equals("OK"))
    			{
    				String ret = Utils.readResp(resp);
    				
    				JSONArray obj = null;
    				try {
    					obj = new JSONArray(ret);
    					int cnt = 0;
    					for (int i=obj.length() - 1; i>=0; i--)
    					{
    						JSONObject post = obj.getJSONObject(i);
    						PostItem item = new PostItem(post);
    						if (insertpos == -1)
    							postslist.add(item);
    						else
    							postslist.add(obj.length() - i, item);
    						cnt++;
    						publishProgress(cnt);
    					}
    					if (insertpos == -1)
    						postslist.add(new PostItem(PostItem.ID_MORE));
    					return "OK";
    				} catch (JSONException e)
    				{
    					return "JSON parse error: " + e.getMessage();
    				}
    			} else {
    				return result.first;
    			}
    		}
    		catch (IOException e)
    		{
    			return "IOException " + e.getMessage();
    		}
    	}
    	
    	protected void onPreExecute()
    	{
    		showBusy("Please wait", "Loading posts...");
    	}
    	
    	protected void onProgressUpdate(Integer... progress)
    	{
    		updateBusy("Loaded " + progress[0] + " posts");
    	}
    	
    	protected void onPostExecute(String result)
    	{
    		hideBusy();
			adapter.notifyDataSetChanged();
    		if (result.equals("OK"))
    		{
    			if (selectid != 0)
    			{
    				for (int i=0; i<postslist.size(); i++)
    				{
    					if (postslist.get(i).id() == selectid)
    					{
    						getListView().setSelection(i);
    						break;
    					}
    				}
    			}
    		} else {
    			if (errMsg.equals("")) {
       				loaded = false;
    				errMsg = "failed to load posts: " + result;
    			}
   				Toast toast = Toast.makeText(getApplicationContext(), 
   					errMsg, Toast.LENGTH_SHORT);
   				toast.show();
    		}
    	}
    }
    
    public void updatePostItem(int position)
    {
    	new updatePostItemTask().execute(position, postslist.get(position).id());
    }
    
    public void doReply(long id, boolean rmode) {
    	RequestArgs args = new RequestArgs(pref.getString("token", ""));
    	args.add("id", postslist.get((int) id).id());
    	args.add("xid", postslist.get((int) id).xid());
    	args.add("board", pref.getString("board", ""));
    	if (rmode)
    		args.add("mode", "R");
    	else
    		args.add("mode", "S");
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
    			return new Pair<String, Object>("Error", ret.second);
    		}
    		catch (IOException e)
    		{
    			return new Pair<String, Object>("IOException" + e.getMessage(), "");
    		} catch (JSONException e) {
    			return new Pair<String, Object>("JSON parse error", e.getMessage());
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
    	    		intent.putExtra("board", pref.getString("board", "test"));
    	    		intent.putExtra("re_id", args.getInt("id"));
    	    		intent.putExtra("re_xid", args.getInt("xid"));
    	    		intent.putExtra("title", obj.getString("title"));
    	    		intent.putExtra("content", 
    	    			((args.getString("mode").equals("R") ? "" : 
    	    				"\nSent from AndWell\n") + obj.getString("content")));
    	    		startActivityForResult(intent, ACTION_REPLY);
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

    private class updatePostItemTask extends AsyncTask<Integer, Integer, Pair<String, Object>> {
    	private String errMsg = "";
    	private int position;
    	private int postid;
    	protected Pair<String, Object> doInBackground(Integer... arg)
    	{
    		RequestArgs args = new RequestArgs(pref.getString("token", ""));
    		args.add("name", pref.getString("board", "test"));
    		position = arg[0];
    		postid = arg[1];
   			args.add("start", String.valueOf(postid));
   			args.add("count", "1");
    		
    		try {
    			HttpResponse resp = Utils.doGet(basePath, "/board/post_list", args.getValue());
    			Pair<String, String> result = Utils.parseResult(resp);
    			if (result.first.equals("OK"))
    			{
    				String ret = Utils.readResp(resp);
    				
    				JSONArray obj = null;
    				try {
    					obj = new JSONArray(ret);
    					JSONObject post = obj.getJSONObject(0);

    					return new Pair<String, Object>("OK", new PostItem(post));
    				} catch (JSONException e)
    				{
    					return new Pair<String, Object>("JSON parse error", e.getMessage());
    				}
    			} else {
    				return new Pair<String, Object>("Request error", result.second);
    			}
    		}
    		catch (IOException e)
    		{
    			return new Pair<String, Object>("IOException", e.getMessage());
    		}
    	}
    	
    	protected void onPostExecute(Pair<String, Object> result)
    	{
    		if (result.first.equals("OK"))
    		{
    			postslist.set(position, (PostItem)result.second);
    			adapter.notifyDataSetChanged();
    		} else {
    			if (errMsg.equals(""))
    				errMsg = "failed to load posts: " + result.first + " : " + result.second;
   				Toast toast = Toast.makeText(getApplicationContext(), 
   					errMsg, Toast.LENGTH_SHORT);
   				toast.show();
    		}
    	}
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
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (requestCode == ACTION_POST || requestCode == ACTION_REPLY) {
    		if (resultCode == RESULT_OK) {
                loadPosts(0, 20, 0, 0);
    		}
    	} else if (requestCode == ACTION_VIEW) {
    		int post_id = data.getExtras().getInt("id");
    		if (!loaded || postslist.size() < 3 || postslist.get(1).id() < post_id 
    				|| postslist.get(postslist.size() - 2).id() > post_id) {
            	int startid = post_id - 10;
            	if (startid < 1)
            		startid = 1;

        		loadPosts(startid, 20, 0, post_id);
        	} else {
        		ArrayList<Integer> post_viewed = data.getExtras().getIntegerArrayList("post_viewed");
        		if (post_viewed != null) {
        			for (int v_xid : post_viewed) {
        				for (int i=0; i<postslist.size(); i++) {
        					if (postslist.get(i).xid() == v_xid) {
        						postslist.get(i).setRead(true);
        						break;
        					}
        				}
        			}
        			adapter.notifyDataSetChanged();
        		}
        		for (int i=0; i<postslist.size(); i++)
				{
					if (postslist.get(i).id() == post_id)
					{
						getListView().setSelection(i);
						break;
					}
				}
        	}
    	}
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	if (!loaded) {
    		loadPosts(0, 20, 0, 0);
    	}
    }
    
    @Override
    public void onDestroy()
    {
    	super.onDestroy();
    	busyDialog = null;
    }
    
    class PostItem {
    	static final int ID_MORE = -1;
    	static final int ID_UPDATE = -2;
    	String _title;
    	String _author;
    	int _id;
    	int _xid;
    	boolean _read;
    	public String toString()
    	{
    		if (_id == ID_MORE)
    			return "more";
    		if (_id == ID_UPDATE)
    			return "update";
			String sunread = "  ";
			if (!_read)
				sunread = " *";
    		
			return _id + sunread + "  " + _author + "\n" + _title;
    	}
    	
    	public int id()
    	{
    		return _id;
    	}
    	
    	public int xid()
    	{
    		return _xid;
    	}
    	
    	public void setRead(boolean read)
    	{
    		_read = read;
    	}
    	
    	PostItem(JSONObject post) throws JSONException
    	{
			_id = post.getInt("id");
    		_title = post.getString("title");
			_author = post.getString("owner");
			_read = post.getBoolean("read");
			_xid = post.getInt("xid");
    	}
    	
    	PostItem(int id)
    	{
    		_id = id;
    		_title = "";
    		_author = "";
    		_read = true;
    	}
    }
}
