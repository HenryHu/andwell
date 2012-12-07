package net.henryhu.andwell;

import android.app.ProgressDialog;
import android.content.Context;

public class BusyDialog {
	ProgressDialog busy;
	Context context;
	BusyDialog(Context context) {
		busy = null;
		this.context = context;
	}
	
	public void show(String title, String msg) {
    	hide();
		busy = ProgressDialog.show(context, title, msg);
    }
    
    public void update(String msg) {
		if (busy != null)
			busy.setMessage(msg);
    }
    
    public void hide() {
		if (busy != null) {
			busy.dismiss();
			busy = null;
		}
    }
}
