package net.henryhu.andwell;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class BoardsActivity extends FragmentActivity implements BoardListFragment.BoardListener, PostListFragment.PostListener {
	private SharedPreferences pref = null;
	PostListFragment postlistFrag = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        pref = getSharedPreferences(Utils.PREFS_FILE, Context.MODE_PRIVATE);
        super.onCreate(savedInstanceState);
        if (useDualPane())
        	setContentView(R.layout.boardlist_dual);
        else
        	setContentView(R.layout.boardlist);
	}
	
	public void onBoardSelected(BoardItem board) {
		Editor edit = pref.edit();
		edit.putString("board", board.title);
		edit.commit();
		if (useDualPane()) {
            postlistFrag = new PostListFragment();
            Bundle args = new Bundle();
            args.putString("board", board.title);
            postlistFrag.setArguments(args);
            getSupportFragmentManager().beginTransaction().replace(R.id.boardlist_postlist, postlistFrag).commit();
		} else {
			Intent intent = new Intent(this, PostListActivity.class);
			intent.putExtra("board", board.title);
			startActivity(intent);
		}
	}
	
	public boolean useDualPane() {
		return Utils.useDualPane(this, 2, 2);
	}

	@Override
	public void onPostSelected(PostItem post) {
		if (PostListActivity.useDualPane(this)) {
			Intent intent = new Intent(this, PostListActivity.class);
			intent.putExtra("post_id", post.id());
			intent.putExtra("post_xid", post.xid());
			if (postlistFrag != null) {
				intent.putExtra("first_post", postlistFrag.getFirstPost());
				intent.putExtra("last_post", postlistFrag.getLastPost());
			}
			intent.putExtra("board", pref.getString("board", ""));
			startActivityForResult(intent, PostListFragment.ACTION_VIEW);
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
}
