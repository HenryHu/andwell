package net.henryhu.andwell;

import org.json.JSONException;
import org.json.JSONObject;

class BoardItem {
	String title;
	String bms;
	String desc;
	int total;
	int currentusers;
	boolean read;
	public String toString()
	{
		String unread = "*";
		if (read)
			unread = " ";
		return Utils.fillTo(title, 16) + "  " + Utils.fillTo(String.valueOf(total), 5) + unread;
	}
	
	BoardItem(JSONObject board) throws JSONException
	{
		title = board.getString("name");
		total = board.getInt("total");
		currentusers = board.getInt("currentusers");
		read = board.getBoolean("read");
		bms = board.getString("BM");
		desc = board.getString("desc");
	}
	
	public void setRead(boolean val)
	{
		read = val;
	}
}
