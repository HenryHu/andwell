package net.henryhu.andwell;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class InputDialogFragment extends DialogFragment {
	String prompt;
	String title;
	String initval;
	InputDialogListener listener;
	int type;
	TextView tValue;
	
	interface InputDialogListener {
		public void onInputOK(int type, String value);
		public void onInputCancel(int type);
	}
	
	static InputDialogFragment newInstance(String title, String prompt, String initval, int type, InputDialogListener listen) {
        InputDialogFragment f = new InputDialogFragment();

        Bundle args = new Bundle();
        args.putString("prompt", prompt);
        args.putString("title", title);
        args.putString("initval", initval);
        args.putInt("type", type);
        f.setArguments(args);
        f.listener = listen;

        return f;
    }
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prompt = getArguments().getString("prompt");
        title = getArguments().getString("title");
        initval = getArguments().getString("initval");
        type = getArguments().getInt("type");
    }
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.inputdialog, container, false);
        assert v != null;

        getDialog().setTitle(title);
    	TextView text = (TextView) v.findViewById(R.id.tPrompt_inputdialog);
    	text.setText(prompt);
    	tValue = (EditText) v.findViewById(R.id.tValue_inputdialog);
    	tValue.setText(initval);
    	Button bOK = (Button) v.findViewById(R.id.bOK_inputdialog);
    	Button bCancel = (Button) v.findViewById(R.id.bCancel_inputdialog);
    	bOK.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
//                ((InputDialogListener)getActivity()).onInputOK(tValue.getText().toString());
                assert tValue.getText() != null;
                listener.onInputOK(type, tValue.getText().toString());
                dismiss();
			}
    	});
    	bCancel.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				listener.onInputCancel(type);
				dismiss();
			}
    	});

        return v;
    }
}
