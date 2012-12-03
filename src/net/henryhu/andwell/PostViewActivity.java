package net.henryhu.andwell;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class PostViewActivity extends FragmentActivity implements PostViewFragment.PostViewListener {
	private SharedPreferences pref = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        pref = getSharedPreferences(Utils.PREFS_FILE, Context.MODE_PRIVATE);

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
	}
}
