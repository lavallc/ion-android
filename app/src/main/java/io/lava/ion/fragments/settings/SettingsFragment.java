package io.lava.ion.fragments.settings;

import io.lava.ion.R;
import io.lava.ion.connectivity.ILampManagerListener;
import io.lava.ion.connectivity.LampManager;
import io.lava.ion.connectivity.lamp.Lamp;
import io.lava.ion.connectivity.lamp.SuccessFailCallback;
import io.lava.ion.receivers.AlarmScheduler;
import io.lava.ion.services.weather.WeatherData;

import java.util.ArrayList;
import java.util.Iterator;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.text.InputFilter;
import android.widget.Toast;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener, ILampManagerListener {
	private ArrayList<PreferenceCategory> lampPreferences = new ArrayList<PreferenceCategory>();
	
	private PreferenceScreen rootScreen;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.app_settings);
        
        // Root preference screen
        rootScreen = getPreferenceScreen();
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // setup prefs
        reCreateLampSettings();
        
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        
        // setup listener for lamp selection so we can update the view on changes
        LampManager lampMgr = LampManager.getInstanceIfReady();
     	lampMgr.addListener(this);
     	
     	// update weather location
     	IconRightPreference geoPref = (IconRightPreference)findPreference("current_weather_location");
        geoPref.setSummary(WeatherData.getInstance().getLocationString());
    }

    @Override
    public void onPause() {
    	LampManager.getInstanceIfReady().removeListener(this);
    	
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        
        super.onPause();
    }

    @Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    	// enable or disable the background discovery alarm
		if (key.equals("background_discovery")) {
			// reconfigure alarms
			AlarmScheduler.setupAlarms(getActivity());
		}
	}
	
	private void addLampSettingsCategory(final Lamp l) {
    	final PreferenceCategory lampCat = new PreferenceCategory(getActivity());
        lampCat.setTitle(l.getName() + " settings");
        lampCat.setKey(l.getMacAddress());
        rootScreen.addPreference(lampCat); // Adding a category
        
        // lamp name
        final SummaryEditTextPreference lampNamePref = new SummaryEditTextPreference(getActivity());
        lampNamePref.setPersistent(false);
        lampNamePref.getEditText().setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(18)
        });
        lampNamePref.setTitle("Lamp Name");
        lampNamePref.setText(l.getName());
        lampNamePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				String newName = (String)newValue;
				
		        l.rename(newName, new SuccessFailCallback() {
		        	@Override
					public void onSuccess() {
		        		Toast.makeText(getActivity(), "lamp renamed", Toast.LENGTH_LONG).show();
		        		
		        		// update category name
		        		lampCat.setTitle(l.getName() + " settings");
					}

					@Override
					public void onFail() {
						// reset the name on fail
						lampNamePref.setText(l.getName());
					}
		        });

		        return true;
			}
        });
        lampCat.addPreference(lampNamePref);
        
        // quiet time enabled
        CheckBoxPreference quietTimePref = new CheckBoxPreference(getActivity());
        quietTimePref.setPersistent(false);
        quietTimePref.setTitle("Quiet Time Enabled");
        quietTimePref.setSummary("Ignore notifications for the times below");
        quietTimePref.setChecked(l.isQuietTimeEnabled());
        lampCat.addPreference(quietTimePref);
        
        // quiet time begin
        final TimePreference quietTimeBeginPref = new TimePreference(getActivity(), null);
        quietTimeBeginPref.setPersistent(false);
        quietTimeBeginPref.setTitle("Quiet Time Begin");
        quietTimeBeginPref.setDialogTitle("Quiet Time Begin");
        quietTimeBeginPref.setTime(l.getNotificationQuietHourStart(), l.getNotificationQuietMinuteStart());
        quietTimeBeginPref.setListener(new TimeListener() {
			@Override
			public void onTimeSet(int hour, int minute) {
				l.setQuietTimeStart(hour, minute, null);
			}	
        });
        lampCat.addPreference(quietTimeBeginPref);
        
        // quiet time end
        final TimePreference quietTimeEndPref = new TimePreference(getActivity(), null);
        quietTimeEndPref.setPersistent(false);
        quietTimeEndPref.setTitle("Quiet Time End");
        quietTimeEndPref.setDialogTitle("Quiet Time End");
        quietTimeEndPref.setTime(l.getNotificationQuietHourEnd(), l.getNotificationQuietMinuteEnd());
        quietTimeEndPref.setListener(new TimeListener() {
			@Override
			public void onTimeSet(int hour, int minute) {
				l.setQuietTimeEnd(hour, minute, null);
			}	
        });
        lampCat.addPreference(quietTimeEndPref);
        
        if (l.isQuietTimeEnabled()) {
        	quietTimeBeginPref.setEnabled(true);
        	quietTimeEndPref.setEnabled(true);
        } else {
        	quietTimeBeginPref.setEnabled(false);
        	quietTimeEndPref.setEnabled(false);
        }
        
        // show/hide quiet times based on quiet time being enabled
        quietTimePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean quietOn = (Boolean)newValue;
				
		        if (quietOn) {
		        	l.setQuietTimeEnabled(true, null);
		        	quietTimeBeginPref.setEnabled(true);
		        	quietTimeEndPref.setEnabled(true);
		        } else {
		        	l.setQuietTimeEnabled(false, null);
		        	quietTimeBeginPref.setEnabled(false);
		        	quietTimeEndPref.setEnabled(false);
		        }

		        return true;
			}
        });
        
        // knock
        CheckBoxPreference knockPref = new CheckBoxPreference(getActivity());
        knockPref.setPersistent(false);
        knockPref.setTitle("Knock Enabled");
        knockPref.setSummary("Knock to turn the lamp on/off");
        knockPref.setChecked(l.isKnockEnabled());
        knockPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean knockEnabled = (Boolean)newValue;
				
		        l.setKnockEnabled(knockEnabled, null);

		        return true;
			}
        });
        lampCat.addPreference(knockPref);
        
        // leash
        CheckBoxPreference leashPref = new CheckBoxPreference(getActivity());
        leashPref.setPersistent(false);
        leashPref.setTitle("Leash Mode");
        leashPref.setSummary("Lamp turns on/off based on proximity");
        leashPref.setChecked(l.isLeashEnabled());
        leashPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean leashEnabled = (Boolean)newValue;
				
		        l.setLeashEnabled(leashEnabled, null);

		        return true;
			}
        });
        
        lampCat.addPreference(leashPref);
        
        // shuffle
        CheckBoxPreference shufflePref = new CheckBoxPreference(getActivity());
        shufflePref.setPersistent(false);
        shufflePref.setTitle("Shuffle Mode");
        shufflePref.setSummary("Lamp continues to change to different moods");
        shufflePref.setChecked(l.isShuffleEnabled());
        lampCat.addPreference(shufflePref);
        
        // shuffle minutes
        final NumberPickerPreference shuffleMinsPref = new NumberPickerPreference(getActivity(), null);
        shuffleMinsPref.setDialogLayoutResource(R.layout.dialog_number_picker);
        shuffleMinsPref.setPersistent(false);
        shuffleMinsPref.setTitle("Shuffle Time");
        shuffleMinsPref.setDialogTitle("Shuffle Time (minutes)");
        shuffleMinsPref.setValue(l.getShuffleTime());
        shuffleMinsPref.setListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				int shuffleMins = (Integer)newValue;
				
		        l.setShuffleTime(shuffleMins, null);

		        return true;
			}
        });
        lampCat.addPreference(shuffleMinsPref);
        
        if (l.isShuffleEnabled())
        	shuffleMinsPref.setEnabled(true);
        else
        	shuffleMinsPref.setEnabled(false);
        
        // show/hide shuffle minutes based on shuffle being enabled
        shufflePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				boolean shuffleOn = (Boolean)newValue;
				
		        if (shuffleOn) {
		        	shuffleMinsPref.setValue(l.getShuffleTime());
		        	shuffleMinsPref.setEnabled(true);
		            l.setShuffleTime(1, null);
		        } else {
		        	shuffleMinsPref.setEnabled(false);
		        	l.setShuffleTime(0, null);
		        }

		        return true;
			}
        });
        
        lampPreferences.add(lampCat);
	}
	
	private void reCreateLampSettings() {
		Iterator<PreferenceCategory> itr = lampPreferences.iterator();
		while (itr.hasNext()) {
			PreferenceCategory lampCat = itr.next();
			rootScreen.removePreference(lampCat);
			lampPreferences.remove(lampCat);
		}
		
		// create prefs for each lamp
        for (Lamp l : LampManager.getInstanceIfReady().getReadyLamps()) {
        	addLampSettingsCategory(l);
        }
	}

	@Override
	public void singleLampSelected(Lamp lamp) {
		reCreateLampSettings();
	}

	@Override
	public void multipleLampsSelected(ArrayList<Lamp> lamps) {
		reCreateLampSettings();
	}

	@Override
	public void noLampsSelected() {
		reCreateLampSettings();
	}

	@Override
	public void onLampListUpdate() {

	}

	@Override
	public void onMoodChange(int moodId) {

	}

	@Override
	public void onWeatherLocationUpdate() {
		if (rootScreen != null) {
			// update weather location
	        IconRightPreference geoPref = (IconRightPreference)findPreference("current_weather_location");
	        geoPref.setSummary(WeatherData.getInstance().getLocationString());
		}
	}

	@Override
	public void onDfuLampListUpdate() {

	}
}