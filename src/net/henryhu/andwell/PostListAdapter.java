package net.henryhu.andwell;

import java.util.Date;
import java.util.List;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class PostListAdapter extends ArrayAdapter<PostItem> {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public PostListAdapter(Context context, int textViewResourceId,
			List objects) {
		super(context, textViewResourceId, objects);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		PostItem item = getItem(position);
		View target;
		if (convertView != null) {
			target = convertView;
		} else {
			LayoutInflater vi = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			target = vi.inflate(R.layout.postlist_entry, null);
		}

        assert target != null;
        TextView idView = ((TextView)target.findViewById(R.id.postlist_entry_id));
		TextView readView = ((TextView)target.findViewById(R.id.postlist_entry_read));
		TextView authorView = ((TextView)target.findViewById(R.id.postlist_entry_author)); 
		TextView timeView = ((TextView)target.findViewById(R.id.postlist_entry_time));
		TextView titleView = ((TextView)target.findViewById(R.id.postlist_entry_title));
		
		if (item.id > 0) {
			//((TextView)target.findViewById(R.id.postlist_entry_id)).setText(Utils.fillTo(String.valueOf(item.id), 5) + "/" + String.valueOf(item.xid) + "/" + String.valueOf(item.reply_to));
			idView.setText(Utils.fillTo(String.valueOf(item.id), 5));
			readView.setText(item.read ? " " : "*");
			authorView.setText(item.author);
			timeView.setText(String.format("%1$tb %1$te", new Date(item.time * 1000)));

			if (item.title.startsWith("Re:") && item.reply_to != item.xid) {
				titleView.setText(item.title);
				titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
			} else {
				titleView.setText("● " + item.title);
				titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
			}
		} else {
			idView.setText("");
			readView.setText("");
			authorView.setText("");
			timeView.setText("");
			titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
			if (item.id == PostItem.ID_MORE) {
				titleView.setText(R.string.postlist_more);
			} else if (item.id == PostItem.ID_UPDATE) {
				titleView.setText(R.string.postlist_update);
			}
		}
			
		
		return target;
	}

}

