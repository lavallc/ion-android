package io.lava.ion.moods;

import io.lava.ion.connectivity.LampManager;
import io.lava.ion.connectivity.lamp.Lamp;
import io.lava.ion.connectivity.lamp.SuccessFailCallback;
import io.lava.ion.utility.SlidingImageView;
import io.lava.ion.widgets.VerticalViewPager;
import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.Toast;

public class MoodScroller extends HorizontalScrollView implements GestureDetector.OnGestureListener, OnLeftRightTapListener {
	private MoodScroller instance;
	private GestureDetectorCompat mDetector;
	private boolean inFling = false;
	private SlidingImageView siv;
	private VerticalViewPager verticalPager;
	private ImageView upArrow, rightArrow, downArrow, leftArrow;
	private boolean first, last;
	private boolean isRight = false;
	private Context context;
	private boolean pendingSave = false;
	private int pageIndex = -1;
	private int numConfigs = -1;
	private LeftRightTapNotifier leftRightArrowNotifier;
	
	// useful for locking the MoodScroller if there are no configs
	private class OnTouch implements OnTouchListener {
	    @SuppressLint("ClickableViewAccessibility")
		@Override
	    public boolean onTouch(View v, MotionEvent event) {
	    	return true;
	    }
	}

	public MoodScroller(Context context) {
		super(context);
		init(context);
	}

	public MoodScroller(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public MoodScroller(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context) {
		this.context = context;
		
		// save instance
		this.instance = this;

		// create gesture detector
		mDetector = new GestureDetectorCompat(getContext(), this);
		
		// kill overscroll
		this.setOverScrollMode(View.OVER_SCROLL_NEVER);
	}

	public void setSlidingImage(SlidingImageView siv) {
		this.siv = siv;
	}
	
	public void setPendingSave() {
		pendingSave = true;
	}
	
	public void setVerticalPagerToLock(VerticalViewPager verticalPager) {
		this.verticalPager = verticalPager;
	}
	
	public void setFirstAndLast(boolean first, boolean last) {
		this.first = first;
		this.last = last;
	}
	
	public void setPageIndex(int pageIndex) {
		this.pageIndex = pageIndex;
	}
	
	public void setNumConfigs(int numConfigs) {
		this.numConfigs = numConfigs;
	}
	
	@SuppressLint("ClickableViewAccessibility")
	public void setLocked() {
		this.setOnTouchListener(new OnTouch());
	}
	
	public void setArrows(ImageView upArrow, ImageView rightArrow, ImageView downArrow, ImageView leftArrow) {
		this.upArrow = upArrow;
		this.rightArrow = rightArrow;
		this.downArrow = downArrow;
		this.leftArrow = leftArrow;
	}
	
	public void setLeftRightArrowNotifier(LeftRightTapNotifier leftRightArrowNotifier) {
		this.leftRightArrowNotifier = leftRightArrowNotifier;
		leftRightArrowNotifier.addListener(this);
	}
	
	public void clearListeners() {
		if (leftRightArrowNotifier != null) {
			leftRightArrowNotifier.removeListener(this);
		}
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	private void snapToPosition() {
		if (inFling) {
			inFling = false;
			return;
		}
		if (this.getScrollX() >= this.getWidth() / 2)
			sendToRight();
		else
			sendToLeft();
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		mDetector.onTouchEvent(ev);

		if (ev.getAction() == MotionEvent.ACTION_UP)
			snapToPosition();

		super.onTouchEvent(ev);
		return true;
	}
	
	public boolean isRight() {
		return isRight;
	}
	
	public void savePendingMoodConfigChanges() {
		// only save if something changed
		if (pendingSave) {
			// save all configs
			for (Lamp l : LampManager.getInstanceIfReady().getReadyLamps()) {
				l.saveMoodConfigs(new SuccessFailCallback() {
					@Override
					public void onSuccess() {

					}

					@Override
					public void onFail() {
						Toast.makeText(context, "mood save failed", Toast.LENGTH_LONG).show();
					}
				});
			}
			
			pendingSave = false;
		}
	}

	public void sendToLeft() {
		instance.post(new Runnable() {
			@Override
			public void run() {
				if (isRight) {
					isRight = false;
					
					savePendingMoodConfigChanges();
				}
				
				instance.fullScroll(View.FOCUS_LEFT);
				
				if (verticalPager != null) {
					verticalPager.setEnabled();
				}
				
				if (verticalPager.getCurrentItem() == pageIndex) {
					if (!first)
						upArrow.setVisibility(View.VISIBLE);
					if (!last)
						downArrow.setVisibility(View.VISIBLE);
				
					// only show right arrow if there are configs
					if (numConfigs > 0)
						rightArrow.setVisibility(View.VISIBLE);
					else
						rightArrow.setVisibility(View.GONE);
					
					leftArrow.setVisibility(View.GONE);
				}
			}
		});
	}

	public void sendToRight() {
		instance.post(new Runnable() {
			@Override
			public void run() {
				isRight = true;
				
				instance.fullScroll(View.FOCUS_RIGHT);
				
				if (verticalPager != null) {
					verticalPager.setDisabled();
				}
				
				if (verticalPager.getCurrentItem() == pageIndex) {
					upArrow.setVisibility(View.GONE);
					downArrow.setVisibility(View.GONE);
					rightArrow.setVisibility(View.GONE);
					leftArrow.setVisibility(View.VISIBLE);
				}
			}
		});
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		inFling = true;
		if (velocityX < 0) {
			sendToRight();
		} else {
			sendToLeft();
		}

		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	@Override
	protected void onScrollChanged(int x, int y, int oldX, int oldY) {
		super.onScrollChanged(x, y, oldX, oldY);

		int scrollViewWidth = this.getWidth();
		int scrolledTo = this.getScrollX();

		// parallax imageview behind us
		float scrolledPercent = (float) scrolledTo / ((float) scrollViewWidth * 2);
		if (siv != null)
			siv.setHorizontalOffset((scrolledPercent / 3) * -1); // divide by 3 as to not move far
	}

	@Override
	public void onLeftTap() {
		if (verticalPager.getCurrentItem() == pageIndex) {
			sendToLeft();
		}
	}

	@Override
	public void onRightTap() {
		if (verticalPager.getCurrentItem() == pageIndex) {
			sendToRight();
		}
	}
}
