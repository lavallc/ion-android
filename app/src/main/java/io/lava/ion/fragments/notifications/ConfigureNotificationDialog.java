package io.lava.ion.fragments.notifications;

import io.lava.ion.R;
import io.lava.ion.connectivity.LampManager;
import io.lava.ion.connectivity.incomingpackets.NotificationConfigPacket;
import io.lava.ion.connectivity.lamp.NotificationConfigCallback;
import io.lava.ion.connectivity.lamp.SuccessFailCallback;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.buzzingandroid.ui.HSVColorWheel;
import com.buzzingandroid.ui.OnColorSelectedListener;

public class ConfigureNotificationDialog extends Activity {
	private String notificationName;
	private boolean notificationEnabled, notificationSticky;
	private int notificationId, notificationPattern, notificationSpeed, notificationDuration;
	private int notificationHue, notificationBrightness, notificationSaturation;
	private Activity activity;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		activity = this;
		
		setContentView(R.layout.configure_notification_dialog);
		
		// pull this notification's data from the intent
		Intent i = getIntent();
		notificationName = i.getStringExtra("notificationName");
		notificationId = i.getIntExtra("notificationId", 0);
		// always enable a notification when it is enabled
		notificationEnabled = true;//i.getBooleanExtra("notificationEnabled", false);
		notificationPattern = i.getIntExtra("notificationPattern", 0);
		notificationHue = i.getIntExtra("notificationHue", 0);
		notificationBrightness = i.getIntExtra("notificationBrightness", 0);
		notificationSaturation = i.getIntExtra("notificationSaturation", 0);
		notificationSpeed = i.getIntExtra("notificationSpeed", 0);
		notificationDuration = i.getIntExtra("notificationDuration", 0);
		notificationSticky = i.getBooleanExtra("notificationSticky", false);
		
		this.setTitle(notificationName + " Notification");

		
		
		
		// duration slider
		final TextView durationLabel = (TextView)findViewById(R.id.notif_duration_text);
		durationLabel.setText(notificationDuration + "s");
		
		final SeekBar durationSlider = (SeekBar)findViewById(R.id.notif_duration);
		durationSlider.setMax(9);
		durationSlider.setProgress(notificationDuration);
		durationSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				// scales from 1-10
				notificationDuration = progress + 1;
				durationLabel.setText(notificationDuration + "s");
			}
		});

		
		// sticky switch
		Switch stickySwitch = (Switch)findViewById(R.id.stickySwitch);
		stickySwitch.setChecked(notificationSticky);
		durationSlider.setEnabled(!notificationSticky);
		stickySwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				notificationSticky = isChecked;
				durationSlider.setEnabled(!notificationSticky);
			}
		});
		
		
		// notification type (radio group)
		RadioGroup radioGroup = (RadioGroup)findViewById(R.id.typeRadioGroup);
		RadioButton haloRadio = (RadioButton)findViewById(R.id.haloRadio);
		RadioButton spinRadio = (RadioButton)findViewById(R.id.spinRadio);
		RadioButton glowRadio = (RadioButton)findViewById(R.id.glowRadio);
		
		if (notificationPattern == 0x05) {
			haloRadio.setChecked(true);
		} else if (notificationPattern == 0x09) {
			spinRadio.setChecked(true);
		} else if (notificationPattern == 0x16) {
			glowRadio.setChecked(true);
		}
		
		radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
		      public void onCheckedChanged(RadioGroup arg0, int id) {
		          switch (id) {
		          case R.id.haloRadio:
		        	notificationPattern = 0x05;
		            break;
		          case R.id.spinRadio:
		        	notificationPattern = 0x09;
		            break;
		          case R.id.glowRadio:
		        	notificationPattern = 0x16;
		            break;
		          default:
		            break;
		          }
		      }
		});
		
		

		// hue picker
		HSVColorWheel colorWheel = (HSVColorWheel)findViewById(R.id.notif_color_wheel);
		colorWheel.setListener( new OnColorSelectedListener() {
			public void colorSelected(Integer color) {						
				float[] f = new float[3];
				Color.colorToHSV(color.intValue(), f);
				notificationHue = (int)f[0];
				notificationSaturation = (int)(f[1] * 255f);
			}
		} );
		
		// set color on wheel
		float hue = (float)notificationHue;
		float sat = ((float)notificationSaturation/255f);
		
		float[] defColor = {hue, sat, 1.0f};
		colorWheel.setColor(Color.HSVToColor(defColor));
        
		
        
        // notification speed slider
		SeekBar speedSlider = (SeekBar)findViewById(R.id.notif_speed);
		speedSlider.setMax(254);
		speedSlider.setProgress(notificationSpeed);
		speedSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				// scales from 1-255
				notificationSpeed = progress + 1;
			}
		});
		
		
		
		
		// preview, save, cancel buttons
		Button previewBtn = (Button)findViewById(R.id.notif_preview_btn);
		previewBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				previewNotification();
			}
		});
		
		Button saveBtn = (Button)findViewById(R.id.notif_save_btn);
		saveBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				saveNotification();
			}
		});
		
		Button cancelBtn = (Button)findViewById(R.id.notif_cancel_btn);
		cancelBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				finish();
			}
		});
	}
	
	private void failedSave() {
		Toast.makeText(activity, "save failed", Toast.LENGTH_LONG).show();
		finish();
	}
	
	private void failedPreview() {
		Toast.makeText(activity, "could not preview", Toast.LENGTH_LONG).show();
	}
	
	private void previewNotification() {
		if (LampManager.getInstanceIfReady() != null) {
			if (LampManager.getInstanceIfReady().getSelectedLampIfExists() != null) {
				LampManager.getInstanceIfReady().getSelectedLampIfExists().showNotification(
					notificationPattern,
					notificationHue,
					notificationBrightness,
					notificationSaturation,
					notificationSpeed,
					notificationDuration,
					notificationSticky,
					new SuccessFailCallback() {
						@Override
						public void onSuccess() {
							// ignore
						}

						@Override
						public void onFail() {
							failedPreview();
						}
					}
				);
			} else {
				failedPreview();
			}
		} else {
			failedPreview();
		}
	}
	
	private void saveNotification() {
		if (LampManager.getInstanceIfReady() != null) {
			if (LampManager.getInstanceIfReady().getSelectedLampIfExists() != null) {
				LampManager.getInstanceIfReady().getSelectedLampIfExists().setNotificationConfig(
					notificationId, 
					notificationEnabled,
					notificationPattern,
					notificationHue,
					notificationBrightness,
					notificationSaturation,
					notificationSpeed,
					notificationDuration,
					notificationSticky,
					new NotificationConfigCallback() {
						@Override
						public void onSuccess(NotificationConfigPacket notificationConfig) {
							Toast.makeText(activity, "notification saved", Toast.LENGTH_LONG).show();
							finish();
						}

						@Override
						public void onFail() {
							failedSave();
						}
					}
				);
			} else {
				failedSave();
			}
		} else {
			failedSave();
		}
	}
}
