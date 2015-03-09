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

package com.sherchen.realclock;

import java.util.TimeZone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
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
	
	/**
	 * 
	 */
	private static final int DEFAULT_BRAND_MARGIN_TOP = 5;
	private static final int DEFAULT_HOUR_CENTER_OFFSET = 8;
	private static final int DEFAULT_MINUTE_CENTER_OFFSET = 10;
	private static final int DEFAULT_SECOND_CENTER_OFFSET = 0;
	
	private static final int DRAW_TYPE_SECOND = 0;
	private static final int DRAW_TYPE_MINUTE = 1;
	private static final int DRAW_TYPE_HOUR = 2;

	private static final boolean DEBUG = true;
	private static final String TAG = "RealAnalogClock";
	private void debug(String msg){
		if(DEBUG) android.util.Log.v(TAG, msg);
	}
	
    private Time m_CurrentTime;

    private Drawable m_HourHandDraw;
    private Drawable m_MinuteHandDraw;
    private Drawable m_SecondHandDraw;
    private Drawable m_DialDraw;
    private Drawable m_BrandDraw;
    
    private int m_HourCenterOffset;
    private int m_MinuteCenterOffset;
    private int m_SecondCenterOffset;
    
    private int m_BrandMarginTop;

    private boolean m_HideHourHand = false;
    private boolean m_HideMinuteHand = false;
    private boolean m_HideSecondHand = false;
    private boolean m_HideDial = false;
    private boolean m_HideBrand = false;
    
    private int m_ClockWidth;
    private int m_ClockHeight;
    //it is used to move the clock to the center of larger axis
//    if the height is larger than the width,then move to the center of height
    private int m_ClockOffset;
//    private int mDialWidth;
//    private int mDialHeight;

    private boolean m_IsAttachedToWindow;

    private final ClockHandler mHandler = new ClockHandler();
    private float m_CurrentSeconds;//0-59
    private float m_CurrentMinutes;//0-59
    private float m_CurrentHour;//0-24
    
    private boolean m_NeedRedraw;

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

        m_HourCenterOffset = a.getDimensionPixelSize(R.styleable.RealAnalogClock_clock_hour_center_offset, DEFAULT_HOUR_CENTER_OFFSET);
        m_MinuteCenterOffset = a.getDimensionPixelSize(R.styleable.RealAnalogClock_clock_minute_center_offset, DEFAULT_MINUTE_CENTER_OFFSET);
        m_SecondCenterOffset = a.getDimensionPixelSize(R.styleable.RealAnalogClock_clock_second_center_offset, DEFAULT_SECOND_CENTER_OFFSET);
        
        m_DialDraw = a.getDrawable(R.styleable.RealAnalogClock_clock_dial);
        
        if(m_DialDraw == null){
        	m_HideDial = true;
        }

        m_BrandDraw = a.getDrawable(R.styleable.RealAnalogClock_clock_brand);
        if(m_BrandDraw == null){
        	m_HideBrand = true;
        }
        
        m_BrandMarginTop = a.getDimensionPixelSize(R.styleable.RealAnalogClock_clock_brand_margin_top, DEFAULT_BRAND_MARGIN_TOP);
        
        m_HourHandDraw = a.getDrawable(R.styleable.RealAnalogClock_clock_pointer_hour);
        if(m_HourHandDraw == null){
        	m_HideHourHand = true;
        }
        
        m_MinuteHandDraw = a.getDrawable(R.styleable.RealAnalogClock_clock_pointer_minute);
        if(m_MinuteHandDraw == null){
        	m_HideMinuteHand = true;
        }
        
		m_SecondHandDraw = a.getDrawable(R.styleable.RealAnalogClock_clock_pointer_second);
		if(m_SecondHandDraw == null){
			m_HideSecondHand = true;
		}
		
		a.recycle();
        m_CurrentTime = new Time();
    }
    
    //===================================================================
    public void setSecondHand(int id){
    	 m_SecondHandDraw = mResources.getDrawable(id);
    }
    
    public void setSecondHand(Drawable drawable){
    	m_SecondHandDraw = drawable;
    }
    
    public void setSecondHand(Bitmap bitmap){
    	m_SecondHandDraw = new BitmapDrawable(bitmap);
    }
    
    public void setMinuteHand(int id){
    	m_MinuteHandDraw = mResources.getDrawable(id);
    }
    
    public void setMinuteHand(Drawable drawable){
    	m_MinuteHandDraw = drawable;
    }
    
    public void setMinuteHand(Bitmap bitmap){
    	m_MinuteHandDraw = new BitmapDrawable(bitmap);
    }
    
    public void setHourHand(int id){
    	m_HourHandDraw = mResources.getDrawable(id);
    }
    
    public void setHourHand(Drawable drawable){
    	m_HourHandDraw = drawable;
    }
    
    public void setHourHand(Bitmap bitmap){
    	m_HourHandDraw = new BitmapDrawable(bitmap);
    }
    
    public void setDial(int id){
    	m_DialDraw = mResources.getDrawable(id);
    }
    
    public void setDial(Drawable drawable){
    	m_DialDraw = drawable;
    }
    
    public void setDial(Bitmap bitmap){
    	m_DialDraw = new BitmapDrawable(bitmap);
    }
    
    
    //--------------------------------------------------------
    public void hideHourHand(boolean hide){
    	m_HideHourHand = hide;
    }
    
    public void hideMinuteHand(boolean hide){
    	m_HideMinuteHand = hide;
    }
    
    public void hideSecondHand(boolean hide){
    	m_HideSecondHand = hide;
    }
    
    public void hideDial(boolean hide){
    	m_HideDial = hide;
    }
    
    public void hideBrand(boolean hide){
    	m_HideBrand = hide;
    }
    
    //--------------------------------------------------------
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!m_IsAttachedToWindow) {
            m_IsAttachedToWindow = true;
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
        m_CurrentTime = new Time();

        // Make sure we update to the current time
        onTimeChanged();
        mHandler.sendEmptyMessageDelayed(MSG_ONESECOND, ONESECOND);
    }
	
	private static final String ACTION_ATTACH_CONTAINER = "com.sherchen.clockui.attach_container";

    private void send(){
        Intent intent = new Intent(ACTION_ATTACH_CONTAINER);
        getContext().sendBroadcast(intent);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (m_IsAttachedToWindow) {
            getContext().unregisterReceiver(mIntentReceiver);
            m_IsAttachedToWindow = false;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

//        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize =  MeasureSpec.getSize(widthMeasureSpec);
//        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize =  MeasureSpec.getSize(heightMeasureSpec);
        
        m_ClockWidth = Math.min(widthSize, heightSize);
        m_ClockHeight = m_ClockWidth;
        m_ClockOffset =( widthSize - heightSize)/2;
        
        
        Log.d(TAG, "w:"+widthSize+" h:"+heightSize);
//        float hScale = 1.0f;
//        float vScale = 1.0f;
//
//        if (widthMode != MeasureSpec.UNSPECIFIED && widthSize < mDialWidth) {
//            hScale = (float) widthSize / (float) mDialWidth;
//        }
//
//        if (heightMode != MeasureSpec.UNSPECIFIED && heightSize < mDialHeight) {
//            vScale = (float )heightSize / (float) mDialHeight;
//        }
//
//        float scale = Math.min(hScale, vScale);
        
        setMeasuredDimension(widthSize, heightSize);
/*       setMeasuredDimension(resolveSizeAndState((int) (mDialWidth * scale), widthMeasureSpec, 0),
                resolveSizeAndState((int) (mDialHeight * scale), heightMeasureSpec, 0));*/
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        m_NeedRedraw = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);

        boolean changed = m_NeedRedraw;
        if (changed) {
            m_NeedRedraw = false;
        }

        int centerX = m_ClockWidth / 2;
        int centerY = m_ClockHeight / 2;

        if(m_ClockOffset > 0){ //w > H
        	canvas.translate(m_ClockOffset, 0);
        }else{
        	canvas.translate(0, - m_ClockOffset);
        }
        
        //---------------draw dial--------------------------------
        final Drawable dial = m_DialDraw;
        int w = dial.getIntrinsicWidth();//116
        int h = dial.getIntrinsicHeight();
        float scale = Math.min((float) m_ClockWidth / (float) w, (float) m_ClockHeight / (float) h);
        
        if(!m_HideDial && changed){//resize the dial to fit the clock sizes
        	canvas.save();
			w *= scale;
			h *= scale;
			dial.setBounds(0, 0, w, h);
			dial.draw(canvas);
			canvas.restore();
        }
        
        if(!m_HideBrand && changed){
        	canvas.save();
        	drawBrand(canvas, centerX, centerY, scale);
        	canvas.restore();
        }
        
        //---------------draw hour hand--------------------------------
        if(!m_HideHourHand && changed){
			canvas.save();
			//rotate the hour hand at the center of x,y
			canvas.rotate(m_CurrentHour / 12.0f * 360.0f, centerX, centerY);
			final Drawable hourHand = m_HourHandDraw;
			draw(canvas, centerX, centerY, scale, hourHand);
			canvas.restore();
		}
        //---------------draw minute hand--------------------------------
		if (!m_HideMinuteHand && changed) {
			canvas.save();
			canvas.rotate(m_CurrentMinutes / 60.0f * 360.0f, centerX, centerY);
			final Drawable minuteHand = m_MinuteHandDraw;
			draw(canvas, centerX, centerY, scale, minuteHand);
			canvas.restore();
		}
        //---------------draw second hand --------------------------------
		if (!m_HideSecondHand && changed) {
			canvas.save();
			canvas.rotate(m_CurrentSeconds / 60.0f * 360.0f, centerX, centerY);
			final Drawable secondHand = m_SecondHandDraw;
			draw(canvas, centerX, centerY, scale, secondHand);
			canvas.restore();
		}
    }

	/**
	 * @param canvas
	 * @param centerX
	 * @param centerY
	 * @param scale
	 * @param drawable
	 */
	private void draw(Canvas canvas, int centerX, int centerY, float scale,
			final Drawable drawable) {
		int w = (int) (drawable.getIntrinsicWidth() * scale);
		int h = (int) (drawable.getIntrinsicHeight() * scale);
		drawable.setBounds(
				centerX - (w / 2), centerY - (h / 2), 
				centerX + (w / 2), centerY + (h / 2));
		drawable.draw(canvas);
	}
	
	private void drawBrand(Canvas canvas, int centerX, int centerY, float scale){
		final Drawable drawable = m_BrandDraw;
		int w = (int) (drawable.getIntrinsicWidth() * scale);
		int h = (int) (drawable.getIntrinsicHeight() * scale);
		drawable.setBounds(centerX - (w / 2), m_BrandMarginTop, centerX + (w / 2), m_BrandMarginTop + h);
		drawable.draw(canvas);
	}

    private void onTimeChanged() {
        m_CurrentTime.setToNow();

        int hour = m_CurrentTime.hour;
        int minute = m_CurrentTime.minute;
        int second = m_CurrentTime.second;

        m_CurrentSeconds = second;
        m_CurrentMinutes = minute + m_CurrentSeconds / 60.0f;
        m_CurrentHour = hour + m_CurrentMinutes / 60.0f;
        m_CurrentHour %= 12;
        debug("m_CurrentHour is " + m_CurrentHour);
        m_NeedRedraw = true;

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
                m_CurrentTime = new Time(TimeZone.getTimeZone(tz).getID());
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
