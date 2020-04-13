package io.lava.ion.fragments.notifications;

import io.lava.ion.R;
import io.lava.ion.connectivity.LampManager;
import io.lava.ion.connectivity.incomingpackets.NotificationConfigPacket;
import io.lava.ion.connectivity.lamp.NotificationConfigCallback;
import io.lava.ion.logger.Logger;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

public class NotificationAdapter extends ArrayAdapter<NotificationInfo> {
	private final Context context;
	private final ArrayList<NotificationInfo> notifications;
	
	public final static int NOTIFICATION_HALO = 0x05;
	public final static int NOTIFICATION_SPIN = 0x09;
	public final static int NOTIFICATION_GLOW = 0x16;
 
	public NotificationAdapter(Context context, ArrayList<NotificationInfo> notifications) {
		super(context, R.layout.notification_row, notifications);
		this.context = context;
		this.notifications = notifications;
	}
	
	private static void saveNotification(NotificationConfigPacket config, boolean enabled) {
		if (LampManager.getInstanceIfReady() != null) {
			if (LampManager.getInstanceIfReady().getSelectedLampIfExists() != null) {
				LampManager.getInstanceIfReady().getSelectedLampIfExists().setNotificationConfig(
					config.getNotificationId(), 
					enabled,
					config.getNotificationType(),
					config.getNotificationHue(),
					config.getNotificationBrightness(),
					config.getNotificationSaturation(),
					config.getNotificationSpeed(),
					config.getNotificationDuration(),
					config.getNotificationSticky(),
					new NotificationConfigCallback() {
						@Override
						public void onSuccess(NotificationConfigPacket notificationConfig) {
							Logger.i(this.getClass().getSimpleName(), "successfully saved config boolean");
						}

						@Override
						public void onFail() {
							// fail
						}
					}
				);
			} else {
				// fail
			}
		} else {
			// fail
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView = convertView;
        if (convertView == null) {
        	LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        	rowView = inflater.inflate(R.layout.notification_row, parent, false);
        }
	    
        // get references to all views
	    View colorBox = (View) rowView.findViewById(R.id.notificationColor);
	    TextView title = (TextView) rowView.findViewById(R.id.notificationTitle);
	    Switch enableSwitch = (Switch) rowView.findViewById(R.id.notificationEnableSwitch);
	    enableSwitch.setOnCheckedChangeListener(null);
	    ImageView notificationIcon = (ImageView) rowView.findViewById(R.id.notificationIcon);
	    
	    // grab our notification config and type data
	    final NotificationConfigPacket notificationConfig = notifications.get(position).getNotificationConfig();
	    final NotificationType notificationType = notifications.get(position).getNotificationType();
	    
	    float[] defColor = {(float)notificationConfig.getNotificationHue(), ((float)notificationConfig.getNotificationSaturation())/255f, 1.0f};
	    boolean isEnabled = notificationConfig.getEnabled();
	    int notificationPattern = notificationConfig.getNotificationType();
	    
	    if (notificationPattern == NOTIFICATION_HALO) {
	    	notificationIcon.setImageResource(R.drawable.notification_halo);
	    } else if (notificationPattern == NOTIFICATION_SPIN) {
	    	notificationIcon.setImageResource(R.drawable.notification_spin);
	    } else if (notificationPattern == NOTIFICATION_GLOW) {
	    	notificationIcon.setImageResource(R.drawable.notification_glow);
	    }
	    
	    if (isEnabled) {
	    	enableSwitch.setChecked(true);
	    } else {
	    	enableSwitch.setChecked(false);
	    }
	    
	    enableSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				notificationConfig.setEnabled(isChecked);
				
				saveNotification(notificationConfig, isChecked);
			}
	    });
	    
	    colorBox.setBackgroundColor(Color.HSVToColor(defColor));
	    
	    title.setText(notificationType.getNotificationTitle());

	    return rowView;
	}
}