package net.henryhu.andwell;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class BoardsActivity extends FragmentActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Utils.useDualPane(this))
        	setContentView(R.layout.boardlist_dual);
        else
        	setContentView(R.layout.boardlist);
	}
}
