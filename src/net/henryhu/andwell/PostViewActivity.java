package net.henryhu.andwell;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class PostViewActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.postview_act);
        if (savedInstanceState == null) {
            // During initial setup, plug in the details fragment.
            PostViewFragment postviewFrag = new PostViewFragment();
            postviewFrag.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().add(R.id.content, postviewFrag).commit();
        }
    }
}
