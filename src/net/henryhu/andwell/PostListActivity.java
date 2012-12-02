package net.henryhu.andwell;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class PostListActivity extends FragmentActivity {
	@Override
		protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        if (Utils.useDualPane(this))
	        	setContentView(R.layout.postlist_dual);
	        else
	        	setContentView(R.layout.postlist);
		}

}
