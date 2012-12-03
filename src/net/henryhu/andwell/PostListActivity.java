package net.henryhu.andwell;

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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        pref = getSharedPreferences(Utils.PREFS_FILE, Context.MODE_PRIVATE);

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
			startActivityForResult(intent, PostListFragment.ACTION_VIEW);
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (postlistFrag != null)
			postlistFrag.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onPostView(int post_id, int post_xid) {
		if (postlistFrag != null)
			postlistFrag.onPostView(post_id, post_xid);
		setTitle(pref.getString("board", "") + " - " + post_id);
	}
	

}
