package net.henryhu.andwell;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class Main extends ListActivity {
	private Activity myAct = null;
	private SharedPreferences pref = null;
	private String basePath = null;
	
	static class MainMenuItem {
		interface MainMenuListener {
			public void onEnter(Context context);
		}
		
		MainMenuListener listener;
		int titleResId;
		public MainMenuItem(int _titleResId, MainMenuListener _listener) {
			titleResId = _titleResId;
			listener = _listener;
		}
		
		public int getTitleResId() { return titleResId; }
		public void enter(Context context) {
			listener.onEnter(context);
		}
	}
	
	class MainMenuAdapter extends ArrayAdapter<MainMenuItem> {

		public MainMenuAdapter(Context context, int textViewResourceId,
				MainMenuItem[] objects) {
			super(context, textViewResourceId, objects);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			MainMenuItem item = this.getItem(position);
			View target = null;
			if (convertView != null) {
				target = convertView;
			} else {
				LayoutInflater vi = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				target = vi.inflate(R.layout.mainmenu, null);
			}
			TextView title = (TextView)target.findViewById(R.id.mainmenu_title);
			title.setText(getString(item.getTitleResId()));
			return target;
		}
		
		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			return getView(position, convertView, parent);
		}
	}

	static final MainMenuItem[] MAINMENU = {
		new MainMenuItem(R.string.mainmenu_boards, new MainMenuItem.MainMenuListener() {
			@Override
			public void onEnter(Context context) {
           		Intent intent = new Intent(context, BoardsActivity.class);
           		intent.putExtra("boardlist_mode", "BOARDS");
           		context.startActivity(intent);
			}
		}),
		new MainMenuItem(R.string.mainmenu_favboards, new MainMenuItem.MainMenuListener() {
			@Override
			public void onEnter(Context context) {
        		Intent intent = new Intent(context, BoardsActivity.class);
           		intent.putExtra("boardlist_mode", "FAVBOARDS");
        		context.startActivity(intent);
			}
		}),
		new MainMenuItem(R.string.mainmenu_logout, new MainMenuItem.MainMenuListener() {
			@Override
			public void onEnter(Context context) {
				((Main)context).logout();
			}
		}),
	};
	
    /** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myAct = this;
        pref = getSharedPreferences(Utils.PREFS_FILE, MODE_PRIVATE);
        
        setListAdapter(new MainMenuAdapter(this, R.layout.mainmenu, MAINMENU));
        
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
        
        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                int position, long id) {
              // When clicked, show a toast with the TextView text
            	MainMenuItem item = MAINMENU[position];
            	item.enter(myAct);
            }
          });
        
		basePath = pref.getString("server_api", "");
		if (savedInstanceState == null)
			loadUserInfo();
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
    
    class LoadUserInfoTask extends AsyncTask<Object, Object, Object> {

		@Override
		protected Object doInBackground(Object... params) {
    		RequestArgs args = new RequestArgs(pref.getString("token", ""));
    		
    		try {
    			HttpResponse resp = Utils.doGet(basePath, "/user/detail", args.getValue());
    			Pair<String, String> result = Utils.parseResult(resp);

       			if (result.first.equals("OK"))
       			{
       				JSONObject obj = null;
       				try {
       					obj = new JSONObject(Utils.readResp(resp));

       					int sig_count = obj.getInt("signum");
       					pref.edit().putInt("signature_count", sig_count).commit();
       					return "OK";
       				} catch (JSONException e)
       				{
       					return new Pair<String, String>("JSON parse error: " + e.getMessage(), "");
        			}
        		} else {
        			return result;
        		}
        	}
        	catch (IOException e)
        	{
        		return new Pair<String, String>("IOException " + e.getMessage(), "");
        	}
		}
		
		@Override
		protected void onPostExecute(Object result) {
			if (result.equals("OK"))
				Utils.showToast(myAct, getString(R.string.userinfo_updated));
			else
				Utils.showToast(myAct, getString(R.string.userinfo_update_fail));
		}
    	
    }
    
    public void loadUserInfo() {
    	new LoadUserInfoTask().execute();
    }
}
