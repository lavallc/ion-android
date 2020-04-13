package io.lava.ion.fragments.settings;

import io.lava.ion.R;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

public class NumberPickerPreference extends DialogPreference {
	private NumberPicker picker;
	private Integer currentVal;
	private OnPreferenceChangeListener listener;
	private Preference instance;
	
	public NumberPickerPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		instance = this;
	}
	
	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		this.picker = (NumberPicker)view.findViewById(R.id.num_picker);
		this.picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
		
		// defaults
		this.picker.setMinValue(1);
		this.picker.setMaxValue(60);
		
		if ( this.currentVal != null ) picker.setValue(this.currentVal);
		
		setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference arg0, Object arg1) {
                arg0.setSummary(getSummary());
                return true;
            }
        });
	}
	
	public void setListener(OnPreferenceChangeListener listener) {
		this.listener = listener;
	}
	
	@Override
	public void onClick(DialogInterface dialog, int which) {
		super.onClick(dialog, which);
		
		if ( which == DialogInterface.BUTTON_POSITIVE ) {
			this.currentVal = picker.getValue();
			callChangeListener( currentVal );
			try {
				listener.onPreferenceChange(instance, currentVal);
			} catch (Exception e) {}
		}
	}
	
	public void setValue(int val) {
		if (this.picker != null)
			this.picker.setValue(val);
		
		currentVal = val;
	}
	
	@Override
    public CharSequence getSummary() {
        return Integer.toString(currentVal) + " minute" + (currentVal > 1 ? "s" : "");
    }
		
	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getInt(index, 1);
	}
}