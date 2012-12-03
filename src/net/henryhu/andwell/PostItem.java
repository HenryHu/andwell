package net.henryhu.andwell;

import org.json.JSONException;
import org.json.JSONObject;

public class PostItem {
	static final int ID_MORE = -1;
	static final int ID_UPDATE = -2;
	String title;
	String author;
	int id;
	int xid;
	int attach;
	int reply_to;
	long time;
	boolean read;
	public String toString()
	{
		if (id == ID_MORE)
			return "more";
		if (id == ID_UPDATE)
			return "update";
		String sunread = "  ";
		if (!read)
			sunread = " *";
		
		return id + sunread + "  " + author + "\n" + title;
	}
	
	
	public void setRead(boolean val)
	{
		read = val;
	}
	
	int id() { return id; }
	int xid() { return xid; }
	
	PostItem(JSONObject post) throws JSONException
	{
		id = post.getInt("id");
		title = post.getString("title");
		author = post.getString("owner");
		read = post.getBoolean("read");
		xid = post.getInt("xid");
		time = post.getLong("posttime");
		attach = post.getInt("attachment");
		reply_to = post.getInt("reply_to");
	}
	
	PostItem(int spec_id)
	{
		id = spec_id;
		title = "";
		author = "";
		read = true;
	}
}