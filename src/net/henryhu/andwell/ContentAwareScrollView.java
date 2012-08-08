package net.henryhu.andwell;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

public class ContentAwareScrollView extends ScrollView {
	private GestureDetector mGestureDetector;
    View.OnTouchListener mGestureListener;
    boolean intercept = true;

    public ContentAwareScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mGestureDetector = new GestureDetector(new YScrollDetector());
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
    	intercept = mGestureDetector.onTouchEvent(ev);
    	return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return super.onInterceptTouchEvent(ev) && intercept;
    }

    // Return false if we're scrolling in the x direction  
    class YScrollDetector extends SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        	float mX = e2.getX() - e1.getX();
        	float mY = e2.getY() - e1.getY();
            return Math.abs(mY) > Math.abs(mX);
        }
    }
}
