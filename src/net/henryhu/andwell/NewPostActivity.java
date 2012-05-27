package net.henryhu.andwell;

import java.io.IOException;

import org.apache.http.HttpResponse;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class NewPostActivity extends Activity {
	SharedPreferences pref;
	EditText title_in;
	EditText content_in;
	EditText qmd_in;
	CheckBox anony_in;
	Button post_btn;
	Button cancel_btn;
	
	String basePath;
	ProgressDialog busyDialog = null;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pref = getSharedPreferences(Utils.PREFS_FILE, MODE_PRIVATE);
        
        setContentView(R.layout.newpost);
        
        title_in = (EditText)findViewById(R.id.newpost_title);
        content_in = (EditText)findViewById(R.id.newpost_content);
        qmd_in = (EditText)findViewById(R.id.newpost_qmd_id);
        anony_in = (CheckBox)findViewById(R.id.newpost_anony);
        post_btn = (Button)findViewById(R.id.newpost_post);
        cancel_btn = (Button)findViewById(R.id.newpost_cancel);
        
        String title = getIntent().getExtras().getString("title");
        if (title != null)
        	title_in.setText(title);
        String content = getIntent().getExtras().getString("content");
        if (content != null)
        	content_in.setText(content);
        qmd_in.setText(pref.getString("qmd_id", ""));
        
        post_btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showConfirm("post");
			}
        });
        
        cancel_btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showConfirm("cancel");
			}
        });
        
        basePath = pref.getString("server_api", "");
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
		String title = title_in.getText().toString();
		String content = content_in.getText().toString();
		int anony = anony_in.isChecked() ? 1 : 0;
		int sig_id = 0;
		if (!qmd_in.getText().toString().equals(""))
			try {
				sig_id = Integer.valueOf(qmd_in.getText().toString());
			} catch (Exception e) {
				showMsg("invalid qmd number");
				return;
			}
		if (title.equals("")) {
			showMsg("Please input title");
			return;
		}
		pref.edit().putString("qmd_id", qmd_in.getText().toString()).commit();
		
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
		new NewPostTask().execute(args);
	}
	
	private class NewPostTask extends AsyncTask<RequestArgs, String, Pair<String, String>> {
		RequestArgs args;
		@Override
		protected Pair<String, String> doInBackground(RequestArgs... params) {
			args = params[0];
			try {
    			HttpResponse resp = Utils.doPost(basePath, "/post/new", args.getValue());
    			Pair<String, String> ret = Utils.parseResult(resp);
    			return ret;
    		}
    		catch (IOException e)
    		{
    			return new Pair<String, String>("IOException " + e.getMessage(), "");
    		}
		}
		
		protected void onPreExecute() {
			showBusy("Please wait...", "Posting...");
		}
		
		protected void onPostExecute(Pair<String, String> result)
    	{
			hideBusy();
    		if (result.first.equals("OK"))
    		{
    			showMsg("posted");
    			setResult(RESULT_OK);
    			finish();
    		} else {
    			showMsg("Error: " + result.first + ": " + result.second);
    		}
    	}	
	}
	
    void showBusy(String title, String msg) {
    	if (busyDialog != null)
    		busyDialog.dismiss();
    	
		busyDialog = ProgressDialog.show(this, title, msg);
    }
    
    void updateBusy(String msg) {
		if (busyDialog != null)
			busyDialog.setMessage(msg);
    }
    
    void hideBusy() {
		if (busyDialog != null) {
			busyDialog.dismiss();
			busyDialog = null;
		}
    }
    
    @Override
    public void onDestroy()
    {
    	super.onDestroy();
    	busyDialog = null;
    }
	
	void showMsg(String msg) {
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
	}
	
}
