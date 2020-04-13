package io.lava.ion.fragments.settings;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.R;
import android.content.Context;
import android.preference.DialogPreference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

public class TimePreference extends DialogPreference {
    private int hour = 0;
    private int minute = 0;
    private TimePicker picker = null;
    private TimeListener listener;

    public TimePreference(Context ctxt) {
        this(ctxt, null);
    }

    public TimePreference(Context ctxt, AttributeSet attrs) {
        this(ctxt, attrs, android.R.attr.preferenceStyle);
    }

    public TimePreference(Context ctxt, AttributeSet attrs, int defStyle) {
        super(ctxt, attrs, defStyle);

        setPositiveButtonText("Set");
        setNegativeButtonText(R.string.cancel);
    }
    
    public void setListener(TimeListener listener) {
		this.listener = listener;
	}

    @Override
    protected View onCreateDialogView() {
        picker = new TimePicker(getContext());
        return (picker);
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        picker.setCurrentHour(hour);
        picker.setCurrentMinute(minute);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            hour = picker.getCurrentHour();
            minute = picker.getCurrentMinute();
            
            try {
				listener.onTimeSet(hour, minute);
			} catch (Exception e) {}

            setSummary(getSummary());
        }
    }
    
    public void setTime(int hour, int minute) {
    	this.hour = hour;
    	this.minute = minute;
    }

    @Override
    public CharSequence getSummary() {
    	Calendar myCal = new GregorianCalendar();
    	myCal.set(Calendar.HOUR_OF_DAY, hour);
        myCal.set(Calendar.MINUTE, minute);
        
        return DateFormat.getTimeFormat(getContext()).format(new Date(myCal.getTimeInMillis()));
    }
}
