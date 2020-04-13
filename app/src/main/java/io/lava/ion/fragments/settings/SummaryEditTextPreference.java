package io.lava.ion.fragments.settings;

import android.content.Context;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.util.AttributeSet;

public class SummaryEditTextPreference extends EditTextPreference {
	public SummaryEditTextPreference(Context context) {
		super(context);
		init();
	}
	
	public SummaryEditTextPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public SummaryEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	private void init() {

        setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference arg0, Object arg1) {
                arg0.setSummary(getText());
                return true;
            }
        });
    }

    @Override
    public CharSequence getSummary() {
        return super.getText();
    }
}
