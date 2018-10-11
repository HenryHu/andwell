package net.henryhu.andwell;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class PostListFragment extends ListFragment implements InputDialogFragment.InputDialogListener {
	private Activity myAct = null;
	private SharedPreferences pref = null;
	private SwipeRefreshLayout mSwipeRefreshView;
	private FloatingActionButton mFab;
	List<PostItem> postslist = new ArrayList<PostItem>();
	BusyDialog busy;
	ArrayAdapter<PostItem> adapter;
	static final int INPUT_DIALOG_ID = 0;
	public static final int ACTION_POST = 1;
	public static final int ACTION_REPLY = 2;
	public static final int ACTION_VIEW = 3;
	String basePath;
	boolean loaded = false;
	PostListener listener = null;
	String token;
	String board;
	
	interface PostListener {
		void onPostSelected(PostItem post);
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
        busy = new BusyDialog(myAct);
        pref = myAct.getSharedPreferences(Utils.PREFS_FILE, Context.MODE_PRIVATE);
        adapter = new PostListAdapter(myAct, R.layout.postlist, postslist);
        setListAdapter(adapter);
        setHasOptionsMenu(true);
        
		basePath = pref.getString("server_api", "");
		token = pref.getString("token", "");
		board = pref.getString("board", "test");
       	loaded = false;
    }

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	    ViewGroup views = (ViewGroup)inflater.inflate(R.layout.postlist_list, container, false);
	    mSwipeRefreshView = views.findViewById(R.id.swiperefresh);
		mSwipeRefreshView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				refreshPosts();
			}
		});
		mFab = views.findViewById(R.id.fab);
		mFab.setOnClickListener(new View.OnClickListener() {
		    @Override
			public void onClick(View view) {
		        newPost();
			}
		});
		return views;
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
        assert item != null;
    	if (item.id == PostItem.ID_MORE)
    	{
    		if (postslist.size() > 2) {
    			int end = postslist.get(postslist.size() - 2).id() - 1;
    			if (end == 0)
    			{
    				Utils.showToast(myAct, getString(R.string.already_first_post)); 
    			} else {
    				postslist.remove(postslist.size() - 1);
    				adapter.notifyDataSetChanged();
    				new LoadPostsTask(new MyLoadPostsListener()).execute(new LoadPostsArg(basePath, token, board, 0, 20, end));
    			}
    		} else
    			loadPosts(0, 20, 0, 0);
    	} else if (item.id() == PostItem.ID_UPDATE)
    	{
    		if (postslist.size() >= 3)
    			new LoadPostsTask(new MyLoadPostsListener()).execute(new LoadPostsArg(basePath, token, board, postslist.get(1).id() + 1, 20, 0, 0));
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
        assert info != null;
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
            refreshPosts();
            return true;
        case R.id.mJumpTo_PostList:
        	showInputDialog(getString(R.string.jumpto_title), getString(R.string.jumpto_withid), "", INPUT_DIALOG_ID);
        	return true;
        case R.id.mPostNew_PostList:
        	newPost();
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    void refreshPosts() {
		loadPosts(0, 20, 0, 0);
	}
    
    void newPost() {
    	Intent intent = new Intent(myAct, NewPostActivity.class);
    	intent.putExtra("board", board);
    	startActivityForResult(intent, ACTION_POST);
    }
    
    void loadPosts(int start, int count, int end, int selectid)
    {
    	loaded = true;
    	postslist.clear();
        //postslist.add(new PostItem(PostItem.ID_UPDATE));
        adapter.notifyDataSetChanged();
    	new LoadPostsTask(new MyLoadPostsListener()).execute(new LoadPostsArg(basePath, token, board, start, count, end, -1, selectid));
    }

    class MyLoadPostsListener extends LoadPostsListener {
    	@Override
    	protected void onPreExecute()
    	{
    		Log.d("PostListFragment", "LoadPosts: PreExec");
    		busy.show(getString(R.string.please_wait), getString(R.string.loading_posts));
    	}
    	
    	@Override
    	protected void onProgressUpdate(LoadPostsProgress progress)
    	{
    		if (progress.insertpos == -1)
    			postslist.add(progress.item);
    		else
    			postslist.add(progress.insertpos, progress.item);
    		adapter.notifyDataSetChanged();
    		busy.update("Loaded " + progress.count + " posts");
    	}
    	
    	@Override
    	protected void onPostExecute(LoadPostsArg arg, String result)
    	{
    		Log.d("PostListFragment", "LoadPosts: PostExec");
    		busy.hide();
    		adapter.notifyDataSetChanged();
    		if (arg.selectid != 0)
    		{
    			for (int i=0; i<postslist.size(); i++)
    			{
    				if (postslist.get(i).id() == arg.selectid)
    				{
    					getListView().setSelection(i);
    					break;
    				}
    			}
    		}
    		mSwipeRefreshView.setRefreshing(false);
    	}
    	
    	@Override
    	protected void onException(LoadPostsArg arg, Exception e) {
    		busy.hide();
			mSwipeRefreshView.setRefreshing(false);
    		String errMsg;
			if (arg.insertpos == 0)
				errMsg = getString(R.string.no_more_post);
			else {
   				loaded = false;
				errMsg = getString(R.string.fail_to_load_posts) + Exceptions.getErrorMsg(e);
			}
			if (arg.insertpos == -1) {
				postslist.add(new PostItem(PostItem.ID_MORE));
				adapter.notifyDataSetChanged();
			}
			Utils.showToast(myAct, errMsg);
		}
    }

    /*
    public void updatePostItem(int position)
    {
    	new UpdatePostItemTask(new MyUpdatePostItemListener()).execute(new UpdatePostItemArg(basePath, token, board, position, postslist.get(position).id()));
    }
    */
    
    public void doReply(long id, boolean rmode) {
    	RequestArgs args = new RequestArgs(token);
    	args.add("id", postslist.get((int) id).id());
    	args.add("xid", postslist.get((int) id).xid());
    	args.add("board", pref.getString("board", ""));
    	if (rmode)
    		args.add("mode", "R");
    	else
    		args.add("mode", "S");
    	new QuotePostTask(new MyQuotePostListener(this, busy, ACTION_REPLY)).execute(new QuotePostArg(basePath, args));
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

    /*
    class MyUpdatePostItemListener extends UpdatePostItemListener {
    	@Override
    	protected void onPostExecute(UpdatePostItemArg arg, PostItem item)
    	{
   			postslist.set(arg.position, item);
    		adapter.notifyDataSetChanged();
    	}
    	@Override
    	protected void onException(UpdatePostItemArg arg, Exception e) {
    		String errMsg = getString(R.string.fail_to_load_posts) + Exceptions.getErrorMsg(e);
			Utils.showToast(myAct, errMsg);
    	}
    }
    */

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
    
    public void onPostReply() {
    	loadPosts(0, 20, 0, 0);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (requestCode == ACTION_POST || requestCode == ACTION_REPLY) {
    		if (resultCode == Activity.RESULT_OK) {
                loadPosts(0, 20, 0, 0);
    		}
    	} else if (requestCode == ACTION_VIEW) {
    		onPostsViewed(resultCode, data);
    	}
    }
    
    public void onPostsViewed(int result, Intent data) {
        assert data.getExtras() != null;
		int post_id = data.getExtras().getInt("id");
		int post_xid = data.getExtras().getInt("xid");
		boolean replied = data.getExtras().getBoolean("replied");
		if (replied) {
			loadPosts(0, 20, 0, post_id);
			return;
		}
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
					for (PostItem post : postslist) {
						if (post.xid() == v_xid) {
							post.setRead(true);
							break;
						}
					}
    			}
    			adapter.notifyDataSetChanged();
    		}
    		onPostView(post_id, post_xid);
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
    	busy.hide();
    }
}
