package net.henryhu.andwell;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
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
	String token;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myAct = getActivity();
        pref = myAct.getSharedPreferences(Utils.PREFS_FILE, Context.MODE_PRIVATE);
        adapter = new BoardListAdapter(myAct, R.layout.boardlist_entry, boardslist);
        setListAdapter(adapter);
        
		basePath = pref.getString("server_api", "");
		token = pref.getString("token", "");
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
    		new LoadBoardsTask(new MyLoadBoardsListener()).execute(new BasicArg(basePath, token));
    	} else if (mode.equals("FAVBOARDS"))
    	{
    		new LoadFavBoardsTask(new MyLoadBoardsListener()).execute(new BasicArg(basePath, token));
    	}
    }

    class MyLoadBoardsListener extends LoadBoardsListener {
    	@Override
    	protected void onPreExecute()
    	{
    		Log.d("BoardListFragment", "LoadBoards: PreExec");
    		loadDialog = ProgressDialog.show(myAct, getString(R.string.please_wait), getString(R.string.loading_boards));
    	}
    	@Override
    	protected void onProgressUpdate(LoadBoardsProgress progress)
    	{
    		if (loadDialog != null)
    			loadDialog.setMessage("Loaded " + progress.count + " boards");
			boardslist.add(progress.board);
			adapter.notifyDataSetChanged();
    	}
    	@Override
    	protected void onPostExecute(BasicArg arg, String result)
    	{
    		Log.d("BoardListFragment", "LoadBoards: PostExec");
    		if (loadDialog != null)
    			loadDialog.dismiss();
   			adapter.notifyDataSetChanged();
    	}
    	@Override
    	protected void onException(BasicArg arg, Exception e) {
    		if (loadDialog != null)
    			loadDialog.dismiss();
			Utils.showToast(myAct, getString(R.string.fail_to_load_boards) + Exceptions.getErrorMsg(e));    		
    	}
    }
    	
    @Override
    public void onDestroy()
    {
    	super.onDestroy();
    	loadDialog = null;
    }
}
