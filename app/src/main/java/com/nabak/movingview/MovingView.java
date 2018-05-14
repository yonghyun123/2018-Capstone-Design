package com.nabak.movingview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ImageView;

public class MovingView extends ImageView {
	Context mContext;
	View targetView;
	Rect targetRect;
	boolean mVibrate;
	
	boolean mDragMode;
	int mDragOffsetY;
	int mDragOffsetX;
	int mTargetOffsetY;
	int mTargetOffsetX;
	
	ImageView mDragView;
	
	MovingViewListener mMovingViewListener;

	/////////////////////////////////////////////////////////////////////////////
	public MovingView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}
	
	public void setTarget(View v, MovingViewListener mvl, boolean vib) {
		targetView = v;
		mMovingViewListener = mvl;
		mVibrate = vib;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		final int action = ev.getAction();
		final int x = (int)ev.getX();
		final int y = (int)ev.getY();	
		
		if (action == MotionEvent.ACTION_DOWN) {
			mDragMode = true;
		}

		if (!mDragMode) 
			return super.onTouchEvent(ev);

		switch (action) {
			case MotionEvent.ACTION_DOWN:
                startDrag(x, y);
				drag(x,y);// replace 0 with x if desired
				break;
			case MotionEvent.ACTION_MOVE:
				drag(x,y);// replace 0 with x if desired
				break;
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
			default:
				mDragMode = false;
				stopDrag();
				break;
		}
		return true;
	}	
	
	// move the drag view
	private void drag(int x, int y) {
		if (mDragView != null) {
			WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) mDragView.getLayoutParams();

			x = x + mDragOffsetX;
			y = y + mDragOffsetY;
			layoutParams.x = x;
			layoutParams.y = y;
			
			WindowManager mWindowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
			mWindowManager.updateViewLayout(mDragView, layoutParams);
			
			//
			if (isInTarget(targetRect, x+mTargetOffsetX, y+mTargetOffsetY)) {
				vibrate();
				mMovingViewListener.onMatchTarget();
				stopDrag();
			}
		}
	}

	// enable the drag view for dragging
	private void startDrag(int x, int y) {
		stopDrag();

		this.setDrawingCacheEnabled(true);
		
        // Create a copy of the drawing cache so that it does not get recycled
        // by the framework when the list tries to clean up memory
        Bitmap bitmap = Bitmap.createBitmap(this.getDrawingCache());
        
        WindowManager.LayoutParams mWindowParams = new WindowManager.LayoutParams();
        mWindowParams.gravity = Gravity.TOP | Gravity.LEFT;
        // image의 현재 위치 보정
        mDragOffsetY = this.getTop() + this.getHeight()/2;
        mDragOffsetX = this.getLeft() - this.getWidth()/2;
        //
        mWindowParams.x = x + mDragOffsetX;
        mWindowParams.y = y + mDragOffsetY;

        mWindowParams.height = LayoutParams.WRAP_CONTENT;
        mWindowParams.width = LayoutParams.WRAP_CONTENT;
        mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        mWindowParams.format = PixelFormat.TRANSLUCENT;
        mWindowParams.windowAnimations = 0;
        
        Context context = getContext();
        ImageView v = new ImageView(context);
        v.setImageBitmap(bitmap);      
        //
        WindowManager mWindowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        mWindowManager.addView(v, mWindowParams);
        mDragView = v;
        this.setVisibility(INVISIBLE);
        // 
		targetRect = new Rect();
		targetView.getGlobalVisibleRect(targetRect);
		//
		mTargetOffsetX = this.getWidth()/2;
		mTargetOffsetY = this.getHeight()/2;
		//
		vibrate();
	}

	// destroy drag view
	private void stopDrag() {
		if (mDragView != null) {
            mDragView.setVisibility(GONE);
            this.setVisibility(VISIBLE);
            WindowManager wm = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);
            wm.removeView(mDragView);
            mDragView.setImageDrawable(null);
            mDragView = null;
        }
	}
	//
	private boolean isInTarget(Rect r, int x, int y) {
		if (r.contains(x, y)) return true;
		else return false;
	}
	//
	private void vibrate() {
		if (mVibrate) {
			Vibrator vib = (Vibrator)mContext.getSystemService(Context.VIBRATOR_SERVICE);
			vib.vibrate(50);
		}
	}
////////////////////////////////////////////////////////////////////////////////
//Interface
////////////////////////////////////////////////////////////////////////////////
	public interface MovingViewListener {
		void onMatchTarget();
	}
}
