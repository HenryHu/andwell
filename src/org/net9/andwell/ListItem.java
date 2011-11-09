package org.net9.andwell;

public class ListItem<T> {
	String _name;
	T _id;
	
	ListItem(String name, T id)
	{
		_name = name;
		_id = id;
	}
	
	public String toString()
	{
		return _name;
	}
	
	public T id()
	{
		return _id;
	}
	
	public String name()
	{
		return _name;
	}
}
