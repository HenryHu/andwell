package net.henryhu.andwell;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class BoardListAdapter extends ArrayAdapter<BoardItem> {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public BoardListAdapter(Context context, int textViewResourceId,
			List objects) {
		super(context, textViewResourceId, objects);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		BoardItem item = getItem(position);
		View target = null;
		if (convertView != null) {
			target = convertView;
		} else {
			LayoutInflater vi = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			target = vi.inflate(R.layout.boardlist_entry, null);
		}
		
		TextView name_view = (TextView)target.findViewById(R.id.boardlist_entry_name);
		name_view.setText(item.title);
		TextView postinfo_view = (TextView)target.findViewById(R.id.boardlist_entry_postinfo);
		postinfo_view.setText(Utils.fillTo(String.valueOf(item.total), 5));
		TextView read_view = (TextView)target.findViewById(R.id.boardlist_entry_read);
		read_view.setText(item.read ? " " : "*");
//		((TextView)target.findViewById(R.id.boardlist_entry_bms)).setText(item.bms);
		((TextView)target.findViewById(R.id.boardlist_entry_desc)).setText(item.desc);
		
		return target;
	}

}

