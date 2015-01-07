/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dirtyhub.realclock;

import java.util.TimeZone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

//import android.R;

/**
 * This widget display an analogic clock with three hands for hours,
 * minutes and seconds. 
 */
public class RealAnalogClock extends View {
	
	static final String TAG = "ClcokUi-MalataAnalogClock";
	
    private Time mCalendar;

    private Drawable mHourHand;
    private Drawable mMinuteHand;
    private Drawable mSecondHand;
    private Drawable mDial;

    private boolean mHideHourHand = false;
    private boolean mHideMinuteHand = false;
    private boolean mHideSecondHand = false;
    private boolean mHideDial = false;
    
    private int availableWidth;
    private int availableHeight;
    private int delta_WH;
    private int mDialWidth;
    private int mDialHeight;

    private boolean mAttached;

    private final ClockHandler mHandler = new ClockHandler();
    private float mSecond;
    private float mMinutes;
    private float mHour;
    private boolean mChanged;

    Context mContext;
    Resources mResources;
    
    public RealAnalogClock(Context context) {
        this(context, null);
    }

    public RealAnalogClock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RealAnalogClock(Context context, AttributeSet attrs,
                       int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        
        mResources = mContext.getResources();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RealAnalogClock, defStyle, 0);

        mDial = a.getDrawable(R.styleable.RealAnalogClock_clock_dial);
        if (mDial == null) {
            mDial = mResources.getDrawable(R.drawable.clock_bg);
        }

        mHourHand = a.getDrawable(R.styleable.RealAnalogClock_clock_pointer_hour);
        if (mHourHand == null) {
            mHourHand = mResources.getDrawable(R.drawable.clock_hour);
        }

        mMinuteHand = a.getDrawable(R.styleable.RealAnalogClock_clock_pointer_minute);
        if (mMinuteHand == null) {
            mMinuteHand = mResources.getDrawable(R.drawable.clock_minute);
        }

		mSecondHand = a.getDrawable(R.styleable.RealAnalogClock_clock_pointer_second);
		if (mSecondHand == null) {
	            mSecondHand = mResources.getDrawable(R.drawable.clock_second);
	    }

        mCalendar = new Time();
    }
    
    //===================================================================
    public void setSecondHand(int id){
    	 mSecondHand = mResources.getDrawable(id);
    }
    
    public void setMinuteHand(int id){
    	mMinuteHand = mResources.getDrawable(id);
    }
    
    public void setHourHand(int id){
    	mHourHand = mResources.getDrawable(id);
    }
    
    public void setDial(int id){
    	mDial = mResources.getDrawable(id);
    }
    //--------------------------------------------------------
    public void hideHourHand(boolean hide){
    	mHideHourHand = hide;
    }
    
    public void hideMinuteHand(boolean hide){
    	mHideMinuteHand = hide;
    }
    
    public void hideSecondHand(boolean hide){
    	mHideSecondHand = hide;
    }
    
    public void hideDial(boolean hide){
    	mHideDial = hide;
    }
    
    //--------------------------------------------------------
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();

            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);

            getContext().registerReceiver(mIntentReceiver, filter);
        }

		send();
		
        // NOTE: It's safe to do these after registering the receiver since the receiver always runs
        // in the main thread, therefore the receiver can't run before this method returns.

        // The time zone may have changed while the receiver wasn't registered, so update the Time
        mCalendar = new Time();

        // Make sure we update to the current time
        onTimeChanged();
        mHandler.sendEmptyMessageDelayed(MSG_ONESECOND, ONESECOND);
    }
	
	private static final String ACTION_ATTACH_CONTAINER = "com.malata.clockui.attach_container";

    private void send(){
        Intent intent = new Intent(ACTION_ATTACH_CONTAINER);
        getContext().sendBroadcast(intent);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            getContext().unregisterReceiver(mIntentReceiver);
            mAttached = false;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize =  MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize =  MeasureSpec.getSize(heightMeasureSpec);
        
        availableWidth = Math.min(widthSize, heightSize);
        availableHeight = availableWidth;
        delta_WH =( widthSize - heightSize)/2;
        
        
        Log.d(TAG, "w:"+widthSize+" h:"+heightSize);
        float hScale = 1.0f;
        float vScale = 1.0f;

        if (widthMode != MeasureSpec.UNSPECIFIED && widthSize < mDialWidth) {
            hScale = (float) widthSize / (float) mDialWidth;
        }

        if (heightMode != MeasureSpec.UNSPECIFIED && heightSize < mDialHeight) {
            vScale = (float )heightSize / (float) mDialHeight;
        }

        float scale = Math.min(hScale, vScale);
        
        setMeasuredDimension(widthSize, heightSize);
/*       setMeasuredDimension(resolveSizeAndState((int) (mDialWidth * scale), widthMeasureSpec, 0),
                resolveSizeAndState((int) (mDialHeight * scale), heightMeasureSpec, 0));*/
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mChanged = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        boolean changed = mChanged;
        if (changed) {
            mChanged = false;
        }

        int x = availableWidth / 2;
        int y = availableHeight / 2;

        if(delta_WH > 0){ //w > H
        	canvas.translate(delta_WH, 0);
        }else{
        	canvas.translate(0, -delta_WH);
        }
        canvas.save();
        
        //---------------draw dial--------------------------------
        final Drawable dial = mDial;
        int w = dial.getIntrinsicWidth();//116
        int h = dial.getIntrinsicHeight();
        float scale = Math.min((float) availableWidth / (float) w,
        		(float) availableHeight / (float) h);
        
        if(!mHideDial){
			if (changed) {
				w *= scale;
				h *= scale;
				dial.setBounds(0, 0, w, h);
			}
			dial.draw(canvas);
        }
        //---------------draw hour hand--------------------------------
        if(!mHideHourHand){
			canvas.save();
			canvas.rotate(mHour / 12.0f * 360.0f, x, y);
			final Drawable hourHand = mHourHand;
			if (changed) {
				w = (int) (hourHand.getIntrinsicWidth() * scale);
				h = (int) (hourHand.getIntrinsicHeight() * scale);
				hourHand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y
						+ (h / 2));
			}
			hourHand.draw(canvas);
			canvas.restore();
		}
        //---------------draw minute hand--------------------------------
		if (!mHideMinuteHand) {
			canvas.save();
			canvas.rotate(mMinutes / 60.0f * 360.0f, x, y);
			final Drawable minuteHand = mMinuteHand;
			if (changed) {
				w = (int) (minuteHand.getIntrinsicWidth() * scale);
				h = (int) (minuteHand.getIntrinsicHeight() * scale);
				minuteHand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y
						+ (h / 2));
			}
			minuteHand.draw(canvas);
			canvas.restore();
		}
        //---------------draw second hand --------------------------------
		if (!mHideSecondHand) {
			canvas.save();
			canvas.rotate(mSecond / 60.0f * 360.0f, x, y);
			final Drawable secondHand = mSecondHand;
			if (changed) {
				w = (int) (secondHand.getIntrinsicWidth() * scale);
				h = (int) (secondHand.getIntrinsicHeight() * scale);
				secondHand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y
						+ (h / 2));
			}
			secondHand.draw(canvas);
			canvas.restore();
		}
    }

    private void onTimeChanged() {
        mCalendar.setToNow();

        int hour = mCalendar.hour;
        int minute = mCalendar.minute;
        int second = mCalendar.second;

        mSecond = second;
        mMinutes = minute + mSecond / 60.0f;
        mHour = hour + mMinutes / 60.0f;
        mChanged = true;

        //updateContentDescription(mCalendar);
    }
    

	static final int MSG_BASE = 0;
    static final int MSG_ONESECOND = MSG_BASE + 1;
    
    static final int ONESECOND = 1000;
    class ClockHandler extends Handler{
    	@Override
    	public void handleMessage(Message msg) {
    		// TODO Auto-generated method stub
    		switch (msg.what) {
			case MSG_BASE:
				
				break;
				
			case MSG_ONESECOND:
	            onTimeChanged();
	            invalidate();
	            mHandler.sendEmptyMessageDelayed(MSG_ONESECOND, ONESECOND);
				break;

			default:
				break;
			}
    		super.handleMessage(msg);
    	}
    }
    
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                String tz = intent.getStringExtra("time-zone");
                mCalendar = new Time(TimeZone.getTimeZone(tz).getID());
            }
            onTimeChanged();
            invalidate();
        }
    };

    private void updateContentDescription(Time time) {
        final int flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_24HOUR;
        String contentDescription = DateUtils.formatDateTime(mContext,
                time.toMillis(false), flags);
        setContentDescription(contentDescription);
    }
}
