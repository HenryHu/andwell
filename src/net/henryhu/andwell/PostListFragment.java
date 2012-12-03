package net.henryhu.andwell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

public class PostListFragment extends ListFragment implements InputDialogFragment.InputDialogListener {
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
	PostListener listener = null;
	
	interface PostListener {
		public void onPostSelected(PostItem post);
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (PostListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement PostListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myAct = getActivity();
        pref = myAct.getSharedPreferences(Utils.PREFS_FILE, Context.MODE_PRIVATE);
        adapter = new PostListAdapter(myAct, R.layout.postlist, postslist);
        setListAdapter(adapter);
        setHasOptionsMenu(true);
        
		basePath = pref.getString("server_api", "");
       	loaded = false;
    }
    
    @Override
    public void onActivityCreated(Bundle saved) {
    	super.onActivityCreated(saved);
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
        registerForContextMenu(getListView());
    }
    
    @Override
    public void onListItemClick(ListView parent, View view,
    		int position, long id) {
    	PostItem item = (PostItem)parent.getItemAtPosition(position);
    	if (item.id == PostItem.ID_MORE)
    	{
    		int end = postslist.get(postslist.size() - 2).id() - 1;
    		if (end == 0)
    		{
    			Utils.showToast(myAct, getString(R.string.already_first_post)); 
    		} else {
    			postslist.remove(postslist.size() - 1);
    			new LoadPostsTask().execute(0, 20, end);
    		}
    	} else if (item.id() == PostItem.ID_UPDATE)
    	{
    		if (postslist.size() >= 3)
    			new LoadPostsTask().execute(postslist.get(1).id() + 1, 20, 0, 0);
    		else
    			loadPosts(0, 20, 0, 0);
    	} else {
    		((PostListener)myAct).onPostSelected(item);
    		item.setRead(true);
    		adapter.notifyDataSetChanged();
    	}
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
    	inflater.inflate(R.menu.postlist, menu);
    }
    
    public void onInputOK(int type, String value) {
		int postid = Integer.parseInt(value);
		if (postid < 1)
			postid = 1;
		int startid = postid - 10;
		if (startid < 1)
			startid = 1;
		
		loadPosts(startid, 20, 0, postid);
    }
    
    public void onInputCancel(int type) {
    	
    }
    
	public void showInputDialog(String title, String prompt, String initval, int type) {
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		Fragment prev = getFragmentManager().findFragmentByTag("inputdialog");
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);

		DialogFragment newFragment = InputDialogFragment.newInstance(title, prompt, initval, type, this);
		newFragment.show(ft, "inputdialog");
	}
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = myAct.getMenuInflater();
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
        	showInputDialog(getString(R.string.jumpto_title), getString(R.string.jumpto), "", INPUT_DIALOG_ID);
        	return true;
        case R.id.mPostNew_PostList:
        	newPost();
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    void newPost() {
    	Intent intent = new Intent(myAct, NewPostActivity.class);
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
    			errMsg = getString(R.string.no_more_post);
    		
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
    		Log.d("PostListFragment", "LoadPosts: PreExec");
    		showBusy(getString(R.string.please_wait), getString(R.string.loading_posts));
    	}
    	
    	protected void onProgressUpdate(Integer... progress)
    	{
    		updateBusy("Loaded " + progress[0] + " posts");
    	}
    	
    	protected void onPostExecute(String result)
    	{
    		Log.d("PostListFragment", "LoadPosts: PostExec");
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
    			Utils.showToast(myAct, errMsg);
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
    
    public int getFirstPost() {
    	if (postslist.size() >= 3)
    		return postslist.get(postslist.size() - 2).id;
    	else
    		return 0;
    }
    
    public int getLastPost() {
    	if (postslist.size() >= 3)
    		return postslist.get(1).id;
    	else
    		return 0;    	
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
    	    		intent.putExtra("board", pref.getString("board", "test"));
    	    		intent.putExtra("re_id", args.getInt("id"));
    	    		intent.putExtra("re_xid", args.getInt("xid"));
    	    		intent.putExtra("title", obj.getString("title"));
    	    		intent.putExtra("content", 
    	    			((args.getString("mode").equals("R") ? "" : 
    	    				"\nSent from AndWell\n") + obj.getString("content")));
    	    		startActivityForResult(intent, ACTION_REPLY);
    	    	} catch (JSONException e) {
    	    		Utils.showToast(myAct, getString(R.string.illegal_reply));
    	    	}    			
    		} else {
    			Utils.showToast(myAct, result.first + ": " + result.second.toString());
    		}
    	}
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
    				errMsg = getString(R.string.fail_to_load_posts) + result.first + " : " + result.second;
    			Utils.showToast(myAct, errMsg);
    		}
    	}
    }

    void showBusy(String title, String msg) {
    	if (busyDialog != null)
    		busyDialog.dismiss();
    	
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
		}
    }
    
    public void onPostView(int post_id, int post_xid) {
    	for (int i=0; i<postslist.size(); i++) {
    		PostItem post = postslist.get(i);
    		if (post.xid() == post_xid) {
    			post.setRead(true);
    			adapter.notifyDataSetChanged();
    			getListView().setSelection(i);
    			break;
    		}
    	}
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (requestCode == ACTION_POST || requestCode == ACTION_REPLY) {
    		if (resultCode == Activity.RESULT_OK) {
                loadPosts(0, 20, 0, 0);
    		}
    	} else if (requestCode == ACTION_VIEW) {
    		int post_id = data.getExtras().getInt("id");
    		int post_xid = data.getExtras().getInt("xid");
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
        		onPostView(post_id, post_xid);
        	}
    	}
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	if (!loaded) {
    		int first_post = 0;
    		int last_post = 0;
    		int post_sel = 0;
    		if (getArguments() != null) {
    			first_post = getArguments().getInt("first_post");
    			last_post = getArguments().getInt("last_post");
    			post_sel = getArguments().getInt("post_id");
    		}
    		if ((first_post != 0) && (last_post != 0))
    			loadPosts(first_post, 0, last_post, post_sel);
    		else
    			loadPosts(0, 20, 0, 0);
    	}
    }
    
    @Override
    public void onDestroy()
    {
    	super.onDestroy();
    	busyDialog = null;
    }
}
