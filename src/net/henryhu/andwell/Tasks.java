package net.henryhu.andwell;

import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.util.Log;

public class Tasks {
	
}

class TaskListener<TArg, U, TRet> {
	protected void onPreExecute() { }
	protected void onProgressUpdate(U progress) { }
	protected void onPostExecute(TArg arg, TRet result) { }
	protected void onException(TArg arg, Exception e) {
		Log.e("TaskListener", "Exception: " + e.toString());
	}
}

abstract class BasicTask<TArg, U, TRet> extends AsyncTask<TArg, U, TRet> {
	class Listener extends TaskListener<TArg, U, TRet> {}
	TaskListener<TArg, U, TRet> listener;
	TRet result;
	TArg arg = null;
	Exception exception = null;
	BasicTask(TaskListener<TArg, U, TRet> _listener) {
		listener = _listener;
	}
	
	@Override
	protected void onPreExecute() {
		try {
			listener.onPreExecute();
		} catch (IllegalStateException e) {
			// Usually this is caused by detached fragments
			// But in onPreExecute()... This is really rare
			// XXX: Anyway, this is just a workaround
			Log.w(this.getClass().getName().toString() + "." + "onPreExecute", "IllegalStateException: ignore");
			Log.w(this.getClass().getName().toString() + "." + "onPreExecute", e.toString());
		}
	}
	
	@Override
	protected void onProgressUpdate(U... progress) {
		try {
			if (progress.length > 0)
				listener.onProgressUpdate(progress[0]);
			else
				listener.onProgressUpdate(null);
		} catch (IllegalStateException e) {
			// Usually this is caused by detached fragments
			// Since it's detached... We may just ignore the progress
			// XXX: Anyway, this is just a workaround
			Log.w(this.getClass().getName().toString() + "." + "onProgressUpdate", "IllegalStateException: ignore");
			Log.w(this.getClass().getName().toString() + "." + "onProgressUpdate", e.toString());
		}
	}
	
	@Override
	protected void onPostExecute(TRet result) {
		try {
			if (exception == null)
				listener.onPostExecute(arg, result);
			else
				listener.onException(arg, exception);
		} catch (IllegalStateException e) {
			// Usually this is caused by detached fragments
			// Since it's detached... We may just ignore the result
			// XXX: Anyway, this is just a workaround
			Log.w(this.getClass().getName().toString() + "." + "onPostExecute", "IllegalStateException: ignore");
			Log.w(this.getClass().getName().toString() + "." + "onPostExecute", e.toString());
		}
	}
	
	protected abstract TRet work(TArg arg) throws Exception;
	@Override
	protected TRet doInBackground(TArg... arg) {
		if (arg.length > 0)
			this.arg = arg[0];
		try {
			return work(this.arg);
		} catch (Exception e) {
			exception = e;
		}
		return null;
	}
}

class BasicArg {
	String basePath;
	String token;
	BasicArg(String basePath, String token) {
		this.basePath = basePath;
		this.token = token;
	}
}

class LoadPostResult {
	int new_post_xid;
	String content;
	LoadPostResult(String content, int new_post_xid) {
		this.new_post_xid = new_post_xid;
		this.content = content;
	}
}

class LoadPostArg extends BasicArg {
	String board;
	int new_post_id;
	LoadPostArg(String _basePath, String _token, String _board, int _new_post_id) {
		super(_basePath, _token);
		board = _board;
		new_post_id = _new_post_id;
	}
}

class LoadPostListener extends TaskListener<LoadPostArg, Integer, LoadPostResult> {}
class LoadPostTask extends BasicTask<LoadPostArg, Integer, LoadPostResult> {
	LoadPostTask(LoadPostListener listener) {
		super(listener);
	}

	@Override
	protected LoadPostResult work(LoadPostArg arg) throws Exception
	{
		RequestArgs args = new RequestArgs(arg.token);
		args.add("id", String.valueOf(arg.new_post_id));
		args.add("board", arg.board);

		HttpResponse resp = Utils.doGet(arg.basePath, "/post/view", args.getValue());
		Utils.checkResult(resp);
		JSONObject obj = null;
		obj = new JSONObject(Utils.readResp(resp));

		String content = obj.getString("content");
		int new_post_xid = obj.getInt("xid"); 
		return new LoadPostResult(content, new_post_xid);
	}
}

class LoadNextPostResult {
	int next_id;
	LoadNextPostResult(int next_id) {
		this.next_id = next_id;
	}
}

class LoadNextPostArg extends BasicArg {
	boolean forward, last_one, only_new;
	String board;
	int post_id;
	LoadNextPostArg(String token, String basePath, String board, int post_id, boolean forward, boolean last_one, boolean only_new) {
		super(basePath, token);
		this.board = board;
		this.post_id = post_id;
		this.forward = forward;
		this.last_one = last_one;
		this.only_new = only_new;
	}	
}

class LoadNextPostListener extends TaskListener<LoadNextPostArg, Integer, LoadNextPostResult> {}
class LoadNextPostTask extends BasicTask<LoadNextPostArg, Integer, LoadNextPostResult> {
	LoadNextPostTask(LoadNextPostListener listener) {
		super(listener);
	}
	protected LoadNextPostResult work(LoadNextPostArg arg) throws Exception
	{
		RequestArgs args = new RequestArgs(arg.token);
		args.add("id", arg.post_id);
		args.add("board", arg.board);
		if (!arg.forward)
			args.add("direction", "backward");
		if (arg.last_one)
			args.add("last_one", 1);
		if (arg.only_new)
			args.add("only_new", 1);
		
		HttpResponse resp = Utils.doGet(arg.basePath, "/post/nextid", args.getValue());
		Utils.checkResult(resp);
		String reply = Utils.readResp(resp);
		JSONObject obj = null;
		obj = new JSONObject(reply);
		int nextid = obj.getInt("nextid");
		publishProgress();
		return new LoadNextPostResult(nextid);
	}
}

class QuotePostArg {
	String basePath;
	RequestArgs requestArgs;
	QuotePostArg(String basePath, RequestArgs requestArgs) {
		this.basePath = basePath;
		this.requestArgs = requestArgs;
	}
}

class QuotePostListener extends TaskListener<QuotePostArg, String, JSONObject> {}
class QuotePostTask extends BasicTask<QuotePostArg, String, JSONObject> {
	QuotePostTask(QuotePostListener listener) {
		super(listener);
	}
	
	@Override
	protected JSONObject work(QuotePostArg args) throws Exception {
		HttpResponse resp = Utils.doGet(args.basePath, "/post/quote", args.requestArgs.getValue());
		Utils.checkResult(resp);
		String res = Utils.readResp(resp);
		JSONObject obj = new JSONObject(res);
		return obj;
	}
}

interface BusyListener {
	void showBusy(String title, String msg);
	void hideBusy();
}

class MyQuotePostListener extends QuotePostListener {
	Fragment context;
	BusyListener busy;
	MyQuotePostListener(Fragment context, BusyListener busy) {
		this.context = context;
		this.busy = busy;
	}
	@Override
	protected void onPreExecute() {
		busy.showBusy(context.getString(R.string.please_wait), context.getString(R.string.quoting_post));
	}
	@Override
	protected void onPostExecute(QuotePostArg arg, JSONObject obj)
	{
		busy.hideBusy();
		try {
			Intent intent = new Intent(context.getActivity(), NewPostActivity.class);
			intent.putExtra("board", arg.requestArgs.getString("board"));
			intent.putExtra("re_id", arg.requestArgs.getInt("id"));
			intent.putExtra("re_xid", arg.requestArgs.getInt("xid"));
			intent.putExtra("title", obj.getString("title"));
			intent.putExtra("content", 
					((arg.requestArgs.getString("mode").equals("R") ? "" : 
							"\nSent from AndWell\n") + obj.getString("content")));
			context.startActivityForResult(intent, PostListFragment.ACTION_REPLY);
		} catch (JSONException e) {
			Utils.showToast(context.getActivity(), context.getString(R.string.illegal_reply));
		}    			
	}
	@Override
	protected void onException(QuotePostArg arg, Exception e) {
		busy.hideBusy();
		Utils.showToast(context.getActivity(), context.getString(R.string.error_with_msg) + Exceptions.getErrorMsg(e));
	}
}

class LoadPostsArg extends BasicArg {
	String board;
	int start;
	int count;
	int end;
	int insertpos;
	int selectid;
	LoadPostsArg(String basePath, String token, String board, int start, int count, int end) {
		this(basePath, token, board, start, count, end, -1);
	}
	LoadPostsArg(String basePath, String token, String board, int start, int count, int end, int insertpos) {
		this(basePath, token, board, start, count, end, insertpos, 0);
	}
	LoadPostsArg(String basePath, String token, String board, int start, int count, int end, int insertpos, int selectid) {
		super(basePath, token);
		this.board = board;
		this.start = start;
		this.count = count;
		this.end = end;
		this.insertpos = insertpos;
		this.selectid = selectid;
	}
}
class LoadPostsProgress {
	int count;
	int insertpos;
	PostItem item;
	LoadPostsProgress(int count, int insertpos, PostItem item) {
		this.count = count;
		this.insertpos = insertpos;
		this.item = item;
	}
}
class LoadPostsListener extends TaskListener<LoadPostsArg, LoadPostsProgress, String> {}
class LoadPostsTask extends BasicTask<LoadPostsArg, LoadPostsProgress, String> {
	LoadPostsTask(LoadPostsListener listener) {
		super(listener);
	}
	@Override
	protected String work(LoadPostsArg arg) throws Exception
	{
		RequestArgs args = new RequestArgs(arg.token);
		int insertpos = -1; // -1: tail 0:head
		args.add("name", arg.board);
		if (arg.start != 0)
			args.add("start", String.valueOf(arg.start));
		if (arg.count != 0)
			args.add("count", String.valueOf(arg.count));
		if (arg.end != 0)
				args.add("end", String.valueOf(arg.end));
		
		HttpResponse resp = Utils.doGet(arg.basePath, "/board/post_list", args.getValue());
		Utils.checkResult(resp);
		String ret = Utils.readResp(resp);

		JSONArray obj = null;
		obj = new JSONArray(ret);
		int cnt = 0;
		for (int i=obj.length() - 1; i>=0; i--) {
			JSONObject post = obj.getJSONObject(i);
			PostItem item = new PostItem(post);
			cnt++;
			if (insertpos == -1)
				publishProgress(new LoadPostsProgress(cnt, -1, item));
			else
				publishProgress(new LoadPostsProgress(cnt, obj.length() - i, item));
		}
		if (insertpos == -1)
			publishProgress(new LoadPostsProgress(cnt, -1, new PostItem(PostItem.ID_MORE)));
		return "OK";
	}
}

class LoadUserInfoListener extends TaskListener<BasicArg, Object, Integer> {}
class LoadUserInfoTask extends BasicTask<BasicArg, Object, Integer> {
	LoadUserInfoTask(LoadUserInfoListener listener) {
		super(listener);
	}
	@Override
	protected Integer work(BasicArg arg) throws Exception {
		RequestArgs args = new RequestArgs(arg.token);
		
		HttpResponse resp = Utils.doGet(arg.basePath, "/user/detail", args.getValue());
		Utils.checkResult(resp);

		JSONObject obj = null;
		obj = new JSONObject(Utils.readResp(resp));

		int sig_count = obj.getInt("signum");
		return sig_count;
	}
}

class NewPostArg extends BasicArg {
	RequestArgs args;
	NewPostArg(String basePath, String token, RequestArgs args) {
		super(basePath, token);
		this.args = args;
	}
}

class NewPostListener extends TaskListener<NewPostArg, String, Object> {}
class NewPostTask extends BasicTask<NewPostArg, String, Object> {
	NewPostTask(NewPostListener listener) {
		super(listener);
	}
	@Override
	protected Object work(NewPostArg arg) throws Exception {
		HttpResponse resp = Utils.doPost(arg.basePath, "/post/new", arg.args.getValue());
		Utils.checkResult(resp);
		Utils.readResp(resp);

		return null;
	}
}

class LoadBoardsProgress {
	int count;
	BoardItem board;
	LoadBoardsProgress(int count, BoardItem board) {
		this.count = count;
		this.board = board;
	}
}

class LoadBoardsListener extends TaskListener<BasicArg, LoadBoardsProgress, String> {}
class LoadBoardsTask extends BasicTask<BasicArg, LoadBoardsProgress, String> {
	LoadBoardsTask(LoadBoardsListener listener) {
		super(listener);
	}
	@Override
	protected String work(BasicArg arg) throws Exception
	{
		RequestArgs args = new RequestArgs(arg.token);
		args.add("count", "100000");
		
		HttpResponse resp = Utils.doGet(arg.basePath, "/board/list", args.getValue());
		Utils.checkResult(resp);
		String ret = Utils.readResp(resp);
		JSONArray obj = null;
		obj = new JSONArray(ret);
		for (int i=0; i<obj.length(); i++)
		{
			JSONObject board = obj.getJSONObject(i);
			publishProgress(new LoadBoardsProgress(i, new BoardItem(board)));
		}
		return "OK";
	}
}

class LoadFavBoardsTask extends BasicTask<BasicArg, LoadBoardsProgress, String> {
	LoadFavBoardsTask(LoadBoardsListener listener) {
		super(listener);
	}
	@Override
	protected String work(BasicArg arg) throws Exception
	{
		RequestArgs args = new RequestArgs(arg.token);
		args.add("count", "100000");

		HttpResponse resp = Utils.doGet(arg.basePath, "/favboard/list", args.getValue());
		Utils.checkResult(resp);
		String ret = Utils.readResp(resp);
		JSONArray obj = null;
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
					publishProgress(new LoadBoardsProgress(i, item));
				} catch (JSONException e)
				{
					Log.w("AndWell", "failed to load favboard #" + i);
				}
			}
		}
		return "OK";
	}
}

class UpdatePostItemArg extends BasicArg {
	String board;
	int position;
	int postid;
	UpdatePostItemArg(String basePath, String token, String board, int position, int postid) {
		super(basePath, token);
		this.board = board;
		this.position = position;
		this.postid = postid;
	}
}

class UpdatePostItemListener extends TaskListener<UpdatePostItemArg, Integer, PostItem> {}
class UpdatePostItemTask extends BasicTask<UpdatePostItemArg, Integer, PostItem> {
	UpdatePostItemTask(UpdatePostItemListener listener) {
		super(listener);
	}
	@Override
	protected PostItem work(UpdatePostItemArg arg) throws Exception
	{
		RequestArgs args = new RequestArgs(arg.token);
		args.add("name", arg.board);
		args.add("start", String.valueOf(arg.postid));
		args.add("count", "1");

		HttpResponse resp = Utils.doGet(arg.basePath, "/board/post_list", args.getValue());
		Utils.checkResult(resp);
		String ret = Utils.readResp(resp);

		JSONArray obj = null;
		obj = new JSONArray(ret);
		JSONObject post = obj.getJSONObject(0);

		return new PostItem(post);
	}
}