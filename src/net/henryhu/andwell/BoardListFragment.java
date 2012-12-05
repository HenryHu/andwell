package net.henryhu.andwell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class BoardListFragment extends ListFragment {
	private Activity myAct = null;
	private SharedPreferences pref = null;
	List<BoardItem> boardslist = new ArrayList<BoardItem>();
	ProgressDialog loadDialog;
	ArrayAdapter<BoardItem> adapter;
	String basePath;
	BoardListener listener = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myAct = getActivity();
        pref = myAct.getSharedPreferences(Utils.PREFS_FILE, Context.MODE_PRIVATE);
        adapter = new BoardListAdapter(myAct, R.layout.boardlist_entry, boardslist);
        setListAdapter(adapter);
        
		basePath = pref.getString("server_api", "");
        loadBoards();
    }
    
    interface BoardListener {
    	void onBoardSelected(BoardItem board);
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (BoardListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement BoardListener");
        }
    }
    
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		BoardItem item = (BoardItem)l.getItemAtPosition(position);
		((BoardListener)getActivity()).onBoardSelected(item);
		
		item.setRead(true);
		adapter.notifyDataSetChanged();
    }
	
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
    	super.onActivityCreated(savedInstanceState);
    	ListView lv = getListView();
        lv.setTextFilterEnabled(true);
    }
    
    void loadBoards()
    {
    	String mode = getArguments().getString("mode");
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
    			Pair<String, String> result = Utils.parseResult(resp);
    			if (result.first.equals("OK"))
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
    				return result.second;
    			}
    		}
    		catch (IOException e)
    		{
    			return "IOException " + e.getMessage();
    		}
    	}
    	
    	protected void onPreExecute()
    	{
    		Log.d("BoardListFragment", "LoadBoards: PreExec");
    		loadDialog = ProgressDialog.show(myAct, getString(R.string.please_wait), getString(R.string.loading_boards));
    	}
    	
    	protected void onProgressUpdate(Integer... progress)
    	{
    		if (loadDialog != null)
    			loadDialog.setMessage("Loaded " + progress[0] + " boards");
    	}
    	
    	protected void onPostExecute(String result)
    	{
    		Log.d("BoardListFragment", "LoadBoards: PostExec");
    		if (loadDialog != null)
    			loadDialog.dismiss();
    		if (result.equals("OK"))
    		{
    			adapter.notifyDataSetChanged();
    		} else {
    			Utils.showToast(myAct, getString(R.string.fail_to_load_boards) + result);
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
    			Pair<String, String> result = Utils.parseResult(resp);
    			if (result.first.equals("OK"))
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
    				return result.second;
    			}
    		}
    		catch (IOException e)
    		{
    			return "IOException " + e.getMessage();
    		}
    	}
    	
    	protected void onPreExecute()
    	{
    		loadDialog = ProgressDialog.show(myAct, getString(R.string.please_wait), getString(R.string.loading_boards));
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
    			Utils.showToast(myAct, getString(R.string.fail_to_load_boards) + result);
    		}
    	}
    }
    
    
    @Override
    public void onDestroy()
    {
    	super.onDestroy();
    	loadDialog = null;
    }
}
