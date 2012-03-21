package net.henryhu.andwell;

import net.henryhu.andwell.R;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

public class Main extends ListActivity {
	private Activity myAct = null;
	private SharedPreferences pref = null;
	
	@SuppressWarnings("rawtypes")
	static final ListItem[] MAINMENU = {
		new ListItem<String>("Boards", "BOARDS"), 
		new ListItem<String>("Favourite Boards", "FAVBOARDS"),
	};
    /** Called when the activity is first created. */
    @SuppressWarnings("unchecked")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myAct = this;
        pref = getSharedPreferences(Utils.PREFS_FILE, MODE_PRIVATE);
        
        setListAdapter(new ArrayAdapter<ListItem<String>>(this, R.layout.mainmenu, MAINMENU));
        
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
        
        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                int position, long id) {
              // When clicked, show a toast with the TextView text
            	ListItem<String> item = (ListItem<String>)MAINMENU[position];
            	pref.edit().putString("mainmenu_mode", item.id()).commit();
            	if (item.id().equals("BOARDS"))
            	{
            		Intent intent = new Intent(myAct, BoardsActivity.class);
            		startActivity(intent);
            	} else if (item.id().equals("FAVBOARDS"))
            	{
            		Intent intent = new Intent(myAct, BoardsActivity.class);
            		startActivity(intent);
            	}
            }
          });

    }
    
    void logout()
    {
    	Editor edit = pref.edit();
    	edit.remove("token");
    	edit.commit();
		Intent intent = new Intent(myAct, AndWell.class);
		startActivity(intent);

    	this.finish();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.main_optmenu, menu);
    	return true;
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.main_optmenu_logout:
            logout();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
