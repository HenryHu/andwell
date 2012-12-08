package net.henryhu.andwell;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

public class PostListActivity extends FragmentActivity implements PostListFragment.PostListener, PostViewFragment.PostViewListener {
	private SharedPreferences pref = null;
	private PostListFragment postlistFrag = null;
	private PostViewFragment postviewFrag = null;
	int last_post_id, last_post_xid;
	ArrayList<Integer> post_viewed;
	static final int ACTION_VIEW_POST = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        pref = getSharedPreferences(Utils.PREFS_FILE, Context.MODE_PRIVATE);
        post_viewed = new ArrayList<Integer>();

        super.onCreate(savedInstanceState);
		if (useDualPane())
			setContentView(R.layout.postlist_dual);
		else
			setContentView(R.layout.postlist);
		
		if (savedInstanceState == null) {
			postlistFrag = new PostListFragment();
			postlistFrag.setArguments(getIntent().getExtras());
			getSupportFragmentManager().beginTransaction().replace(R.id.postlist, postlistFrag).commit();
		} else {
			postlistFrag = (PostListFragment)getSupportFragmentManager().findFragmentById(R.id.postlist);
			postviewFrag = (PostViewFragment)getSupportFragmentManager().findFragmentById(R.id.postlist_right);
		}
        
        if (useDualPane()) {
			showPost(getIntent().getExtras().getInt("post_id"), getIntent().getExtras().getInt("post_xid"));
        }
	}
	
	public static boolean useDualPane(Context context) {
		return Utils.useDualPane(context, 2, 3);
	}
	
	public boolean useDualPane() {
		return useDualPane(this);
	}
	
	void showPost(int post_id, int post_xid) {
		if (postviewFrag != null)
			if (postviewFrag.getPostId() == post_id && postviewFrag.getPostXid() == post_xid)
				return;
				
		postviewFrag = new PostViewFragment();
		Bundle args = new Bundle();
		args.putInt("id", post_id);
		args.putInt("xid", post_xid);
		args.putString("board", pref.getString("board", ""));
		postviewFrag.setArguments(args);
		
		FragmentManager fm = getSupportFragmentManager();
		fm.beginTransaction().replace(R.id.postlist_right, postviewFrag).commit();
	}

	@Override
	public void onPostSelected(PostItem post) {
		if (useDualPane()) {
			showPost(post.id(), post.xid());
		} else {
			Intent intent = new Intent(this, PostViewActivity.class);
			intent.putExtra("id", post.id());
			intent.putExtra("xid", post.xid());
			intent.putExtra("board", pref.getString("board", ""));
			startActivityForResult(intent, ACTION_VIEW_POST);
		}
	}
	
	@Override
	public void onActivityResult(int request, int result, Intent intent) {
		if (request == ACTION_VIEW_POST) {
			postlistFrag.onPostsViewed(result, intent);
		}
		super.onActivityResult(request, result, intent);
	}
	
	@Override
	public void onPostView(int post_id, int post_xid) {
		if (postlistFrag != null)
			postlistFrag.onPostView(post_id, post_xid);
		setTitle(pref.getString("board", "") + " - " + post_id);
		last_post_id = post_id;
		last_post_xid = post_xid;
		post_viewed.add(post_xid);
		updateResult();
	}
	
	@Override
	public void onPostReply() {
		if (postlistFrag != null)
			postlistFrag.onPostReply();
	}
	
    private void updateResult() {
    	Intent data = new Intent(this, PostListActivity.class);
    	Bundle extras = new Bundle();
    	extras.putInt("id", last_post_id);
    	extras.putInt("xid", last_post_xid);
    	extras.putIntegerArrayList("post_viewed", post_viewed);

    	data.putExtras(extras);

    	setResult(Activity.RESULT_OK, data);
    }
}
