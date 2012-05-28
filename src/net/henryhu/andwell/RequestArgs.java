package net.henryhu.andwell;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

public class RequestArgs {
	List<NameValuePair> value;
	
	RequestArgs(String session)
	{
		value = new ArrayList<NameValuePair>();
//		value.add(new BasicNameValuePair("action", action));
		if (! session.equals(""))
			value.add(new BasicNameValuePair("session", session));
	}
	
	public void add(String item, String val)
	{
		value.add(new BasicNameValuePair(item, val));
	}
	
	public void add(String item, int val)
	{
		add(item, String.valueOf(val));
	}
	
	public int getInt(String item) {
		return Integer.parseInt(getString(item));
	}
	
	public String getString(String item) {
		for (NameValuePair pair : value) {
			if (pair.getName().equals(item)) {
				return pair.getValue();
			}
		}
		return "";
	}
	
	public List<NameValuePair> getValue()
	{
		return value;
	}
}
