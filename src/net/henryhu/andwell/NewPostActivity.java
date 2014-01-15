package net.henryhu.andwell;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class NewPostActivity extends Activity {
	SharedPreferences pref;
	EditText title_in;
	EditText content_in;
	Spinner qmd_in;
	CheckBox anony_in;
	Button post_btn;
	Button cancel_btn;
	
	String basePath;
	String token;
	BusyDialog busy;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pref = getSharedPreferences(Utils.PREFS_FILE, MODE_PRIVATE);
        busy = new BusyDialog(this);
        
        setContentView(R.layout.newpost);
        
        title_in = (EditText)findViewById(R.id.newpost_title);
        content_in = (EditText)findViewById(R.id.newpost_content);
        qmd_in = (Spinner)findViewById(R.id.newpost_qmd_id);
        anony_in = (CheckBox)findViewById(R.id.newpost_anony);
        post_btn = (Button)findViewById(R.id.newpost_post);
        cancel_btn = (Button)findViewById(R.id.newpost_cancel);

        assert getIntent().getExtras() != null;
        String title = getIntent().getExtras().getString("title");
        if (title != null)
        	title_in.setText(title);
        String content = getIntent().getExtras().getString("content");
        if (content != null)
        	content_in.setText(content);
        else
        	content_in.setText("\nSent from AndWell");
        int max_qmd_num = pref.getInt("signature_count", 100);
        List<QmdSelection> qmds = new ArrayList<QmdSelection>();
        qmds.add(QmdSelection.getRandomItem());
        qmds.add(QmdSelection.getNoneItem());
        for (int i=1; i<=max_qmd_num; i++)
        	qmds.add(new QmdSelection(i));
        QmdAdapter adapter = new QmdAdapter(this, 0, qmds);
        qmd_in.setAdapter(adapter);
        
        for (int i = 0; i < qmd_in.getCount(); i++) {
            QmdSelection qmd_i = (QmdSelection)qmd_in.getItemAtPosition(i);
            assert qmd_i != null;
            if (qmd_i.id().equals(
                    pref.getString("qmd_id", String.valueOf(QmdSelection.NONE_ID))))
                qmd_in.setSelection(i);
        }
        
        post_btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showConfirm("post");
			}
        });
        
        cancel_btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showConfirm("cancel");
			}
        });
        
        basePath = pref.getString("server_api", "");
        token = pref.getString("token", "");
    }
	
	void showConfirm(final String item) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Are you sure you want to " + item + "?")
		       .setCancelable(false)
		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   if (item.equals("cancel")) {
		        		   setResult(RESULT_CANCELED);
		        		   finish();
		        	   } else if (item.equals("post")) {
		        		   doPost();
		        	   }
		           }
		       })
		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		           }
		       });
		builder.create().show();
	}
	
	void doPost() {
        assert title_in.getText() != null;
        assert content_in.getText() != null;
		String title = title_in.getText().toString();
		String content = content_in.getText().toString();
		int anony = anony_in.isChecked() ? 1 : 0;
		int sig_id = 0;

        QmdSelection qmd_selected = ((QmdSelection)qmd_in.getSelectedItem());
        assert qmd_selected != null;
		if (!qmd_selected.isNoneItem())
			try {
				sig_id = Integer.parseInt(((QmdSelection)qmd_in.getSelectedItem()).id());
			} catch (Exception e) {
				sig_id = 0;
			}
		if (title.equals("")) {
			showMsg(getString(R.string.please_input_post_title));
			return;
		}
		pref.edit().putString("qmd_id", ((QmdSelection)qmd_in.getSelectedItem()).id()).commit();

		int re_id = this.getIntent().getExtras().getInt("re_id");
		int re_xid = this.getIntent().getExtras().getInt("re_xid");
		String board = this.getIntent().getExtras().getString("board");
		String session = pref.getString("token", "");
		
		RequestArgs args = new RequestArgs(session);
		args.add("title", title);
		args.add("content", content);
		args.add("board", board);
		if (anony == 1)
			args.add("anonymous", anony);
		if (sig_id != 0)
			args.add("signature_id", sig_id);
		if (re_id != 0)
			args.add("re_id", re_id);
		if (re_xid != 0)
			args.add("re_xid", re_xid);
		new NewPostTask(new NewPostListener() {
			@Override
			protected void onPreExecute() {
				busy.show(getString(R.string.please_wait), getString(R.string.newpost_posting));
			}
			@Override
			protected void onPostExecute(NewPostArg arg, Object result)
			{
				busy.hide();
				showMsg(getString(R.string.posted));
				setResult(RESULT_OK);
				finish();
			}
			@Override
			protected void onException(NewPostArg arg, Exception e) {
				busy.hide();
				showMsg(getString(R.string.error_with_msg) + Exceptions.getErrorMsg(e));
			}
		}).execute(new NewPostArg(basePath, token, args));
	}
    
    @Override
    public void onDestroy()
    {
    	super.onDestroy();
    	busy.hide();
    }
	
	void showMsg(String msg) {
        assert getApplicationContext() != null;
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
	}
	
	static class QmdSelection {
		int id;
		static final int RANDOM_ID = -1;
		static final int NONE_ID = 0;
		public QmdSelection(int _id) {
			id = _id;
		}
		public String id() { return String.valueOf(id); }
		public static QmdSelection getRandomItem() { return new QmdSelection(RANDOM_ID); }
		public static QmdSelection getNoneItem() { return new QmdSelection(NONE_ID); }
		public boolean isRandomItem() { return id == RANDOM_ID; }
		public boolean isNoneItem() { return id == NONE_ID; }
		@Override
		public String toString() {
			return String.valueOf(id);
		}
	}
	
	class QmdAdapter extends ArrayAdapter<QmdSelection> {

		public QmdAdapter(Context context, int textViewResourceId,
				List<QmdSelection> objects) {
			super(context, textViewResourceId, objects);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			QmdSelection item = this.getItem(position);
			View target;
			TextView idView;
			if (convertView != null) {
				target = convertView;
			} else {
				LayoutInflater vi = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				target = vi.inflate(R.layout.newpost_qmd_selection, null);
			}
            assert target != null;
			idView = (TextView)target.findViewById(R.id.newpost_qmd_num);
            assert post_btn.getTextColors() != null;
			if (convertView == null)
				idView.setTextColor(post_btn.getTextColors().getDefaultColor());
			if (item.isRandomItem()) {
				idView.setText(getString(R.string.qmd_random));
			} else if (item.isNoneItem()) {
				idView.setText(getString(R.string.qmd_none));
			} else {
				idView.setText(item.id());
			}
			return target;
		}
		
		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent) {
			return getView(position, convertView, parent);
		}
		
	}
	
}
