package net.henryhu.andwell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import net.henryhu.andwell.R;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class BoardsActivity extends ListActivity {
	private Activity myAct = null;
	private SharedPreferences pref = null;
	List<BoardItem> boardslist = new ArrayList<BoardItem>();
	ProgressDialog loadDialog;
	ArrayAdapter<BoardItem> adapter;
	String basePath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myAct = this;
        pref = getSharedPreferences(Utils.PREFS_FILE, MODE_PRIVATE);
        adapter = new ArrayAdapter<BoardItem>(this, R.layout.boardlist, boardslist);
        setListAdapter(adapter);
        
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
		basePath = pref.getString("server_api", "");
        
        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                int position, long id) {
              // When clicked, show a toast with the TextView text
            	BoardItem item = boardslist.get(position);
            	Editor edit = pref.edit();
            	edit.putString("board", item.id());
            	edit.remove("post_id");
            	edit.commit();
            	Intent intent = new Intent(myAct, PostListActivity.class);
            	startActivity(intent);
            	boardslist.get(position).setRead(true);
            	adapter.notifyDataSetChanged();
            }
          });
        
        loadBoards();
    }
    
    void loadBoards()
    {
    	String mode = pref.getString("mainmenu_mode", "BOARDS");
    	if (mode.equals("BOARDS"))
    	{
    		new LoadBoardsTask().execute(0);
    	} else if (mode.equals("FAVBOARDS"))
    	{
    		new LoadFavBoardsTask().execute(0);
    	}
    }

    private class LoadBoardsTask extends AsyncTask<Integer, Integer, String> {
    	protected String doInBackground(Integer... dummy)
    	{
    		RequestArgs args = new RequestArgs(pref.getString("token", ""));
    		args.add("count", "100000");
    		
    		try {
    			HttpResponse resp = Utils.doGet(basePath, "/board/list", args.getValue());
    			if (resp.getStatusLine().getStatusCode() == 200)
    			{
    				String ret = Utils.readResp(resp);
    				JSONArray obj = null;
    				try {
    					obj = new JSONArray(ret);
    					for (int i=0; i<obj.length(); i++)
    					{
    						JSONObject board = obj.getJSONObject(i);
    						boardslist.add(new BoardItem(board));
    						publishProgress(i);
    					}
    					return "OK";
    				} catch (JSONException e)
    				{
    					return "JSON parse error: " + e.getMessage();
    				}
    			} else {
    				return resp.getStatusLine().getReasonPhrase();
    			}
    		}
    		catch (IOException e)
    		{
    			return "IOException " + e.getMessage();
    		}
    	}
    	
    	protected void onPreExecute()
    	{
    		loadDialog = ProgressDialog.show(myAct, "Please wait", "Loading boards...");
    	}
    	
    	protected void onProgressUpdate(Integer... progress)
    	{
    		if (loadDialog != null)
    			loadDialog.setMessage("Loaded " + progress[0] + " boards");
    	}
    	
    	protected void onPostExecute(String result)
    	{
    		if (loadDialog != null)
    			loadDialog.dismiss();
    		if (result.equals("OK"))
    		{
    			adapter.notifyDataSetChanged();
    		} else {
    			Toast toast = Toast.makeText(getApplicationContext(), 
    					"failed to load boards: " + result, Toast.LENGTH_SHORT);
    			toast.show();
    			finish();
    		}
    	}
    }

    private class LoadFavBoardsTask extends AsyncTask<Integer, Integer, String> {
    	protected String doInBackground(Integer... dummy)
    	{
    		RequestArgs args = new RequestArgs(pref.getString("token", ""));
    		args.add("count", "100000");
    		
    		try {
    			HttpResponse resp = Utils.doGet(basePath, "/favboard/list", args.getValue());
    			if (resp.getStatusLine().getStatusCode() == 200)
    			{
    				String ret = Utils.readResp(resp);
    				JSONArray obj = null;
    				try {
    					obj = new JSONArray(ret);
    					for (int i=0; i<obj.length(); i++)
    					{
    						JSONObject fboard = obj.getJSONObject(i);
    						String type = fboard.getString("type");
    						if (type.equals("board"))
    						{
    							JSONObject board = null;
    							try {
    								board = fboard.getJSONObject("binfo");
    								if (board == null)
    									throw new JSONException("no board info");
    								BoardItem item = new BoardItem(board);
    								boardslist.add(item);
    							} catch (JSONException e)
    							{
    								Log.w("AndWell", "failed to load favboard #" + i);
    							}
    						}
    						publishProgress(i);
    					}
    					return "OK";
    				} catch (JSONException e)
    				{
    					return "JSON parse error: " + e.getMessage();
    				}
    			} else {
    				return resp.getStatusLine().getReasonPhrase();
    			}
    		}
    		catch (IOException e)
    		{
    			return "IOException " + e.getMessage();
    		}
    	}
    	
    	protected void onPreExecute()
    	{
    		loadDialog = ProgressDialog.show(myAct, "Please wait", "Loading boards...");
    	}
    	
    	protected void onProgressUpdate(Integer... progress)
    	{
    		if (loadDialog != null)
    			loadDialog.setMessage("Loaded " + progress[0] + " boards");
    	}
    	
    	protected void onPostExecute(String result)
    	{
    		if (loadDialog != null)
    			loadDialog.dismiss();
    		if (result.equals("OK"))
    		{
    			adapter.notifyDataSetChanged();
    		} else {
    			Toast toast = Toast.makeText(getApplicationContext(), 
    					"failed to load boards: " + result, Toast.LENGTH_SHORT);
    			toast.show();
    			finish();
    		}
    	}
    }
    
    class BoardItem {
    	String _title;
    	int _total;
    	int _currentusers;
    	boolean _read;
    	public String toString()
    	{
    		String unread = "*";
    		if (_read)
    			unread = " ";
    		return Utils.fillTo(_title, 16) + "  " + Utils.fillTo(String.valueOf(_total), 5) + unread;
    	}
    	
    	BoardItem(JSONObject board) throws JSONException
    	{
			_title = board.getString("name");
			_total = board.getInt("total");
			_currentusers = board.getInt("currentusers");
			_read = board.getBoolean("read");
    	}
    	
    	public String id()
    	{
    		return _title;
    	}
    	
    	public void setRead(boolean read)
    	{
    		_read = read;
    	}
    }
    
    @Override
    public void onDestroy()
    {
    	super.onDestroy();
    	loadDialog = null;
    }

}
