package net.henryhu.andwell;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class PatchedTextView extends TextView {
	public PatchedTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	public PatchedTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	public PatchedTextView(Context context) {
		super(context);
	}
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		try{
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}catch (IndexOutOfBoundsException e){
            assert getText() != null;
			setText(getText().toString());
			super.onMeasure(widthMeasureSpec, heightMeasureSpec); 
		}
	}	
	@Override
	public void setGravity(int gravity){
		try{
			super.setGravity(gravity);
		}catch (IndexOutOfBoundsException e){
            assert getText() != null;
			setText(getText().toString());
			super.setGravity(gravity); 
		}
	}
	@Override
	public void setText(CharSequence text, BufferType type) {
		try{
			super.setText(text, type);
		}catch (IndexOutOfBoundsException e){
			setText(text.toString());
		}
	}
}