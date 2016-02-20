package net.henryhu.andwell;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import android.util.Pair;

public class RequestArgs {
	List<Pair<String, String>> value;
	
	RequestArgs(String session)
	{
		value = new ArrayList<Pair<String, String>>();
//		value.add(new BasicNameValuePair("action", action));
		if (!session.equals("")) add("session", session);
	}
	
	public void add(String item, String val)
	{
		value.add(new Pair<String, String>(item, val));
	}
	
	public void add(String item, int val)
	{
		add(item, String.valueOf(val));
	}
	
	public int getInt(String item) {
		return Integer.parseInt(getString(item));
	}
	
	public String getString(String item) {
		for (Pair<String, String> pair : value) {
			if (pair.first.equals(item)) {
				return pair.second;
			}
		}
		return "";
	}

	public String getEncodedForm() {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Pair<String, String> pair : value) {
			if (!first) sb.append("&");
			try {
				sb.append(URLEncoder.encode(pair.first, "UTF-8"));
				sb.append("=");
				sb.append(URLEncoder.encode(pair.second, "UTF-8"));
				first = false;
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}
}
