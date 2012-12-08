package net.henryhu.andwell;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class PostViewActivity extends FragmentActivity implements PostViewFragment.PostViewListener {
	private SharedPreferences pref = null;
	int last_post_id, last_post_xid;
	ArrayList<Integer> post_viewed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        pref = getSharedPreferences(Utils.PREFS_FILE, Context.MODE_PRIVATE);
        post_viewed = new ArrayList<Integer>();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.postview_act);
        if (savedInstanceState == null) {
            // During initial setup, plug in the details fragment.
            PostViewFragment postviewFrag = new PostViewFragment();
            postviewFrag.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().add(R.id.content, postviewFrag).commit();
        }
    }

	@Override
	public void onPostView(int post_id, int post_xid) {
    	setTitle(pref.getString("board", "") + " - " + post_id);
		last_post_id = post_id;
		last_post_xid = post_xid;
		post_viewed.add(post_xid);
		updateResult();
	}
	
    private void updateResult() {
    	Intent data = new Intent(this, PostViewActivity.class);
    	Bundle extras = new Bundle();
    	extras.putInt("id", last_post_id);
    	extras.putInt("xid", last_post_xid);
    	extras.putIntegerArrayList("post_viewed", post_viewed);

    	data.putExtras(extras);

    	setResult(Activity.RESULT_OK, data);
    }
}
