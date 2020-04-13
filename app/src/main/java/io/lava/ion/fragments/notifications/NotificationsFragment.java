package io.lava.ion.fragments.notifications;

import io.lava.ion.NotificationFactory;
import io.lava.ion.R;
import io.lava.ion.connectivity.ILampManagerListener;
import io.lava.ion.connectivity.LampManager;
import io.lava.ion.connectivity.incomingpackets.NotificationConfigPacket;
import io.lava.ion.connectivity.lamp.Lamp;
import io.lava.ion.connectivity.lamp.NotificationConfigCallback;
import io.lava.ion.connectivity.lamp.SuccessFailCallback;

import java.util.ArrayList;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class NotificationsFragment extends Fragment implements ILampManagerListener {
	private ArrayList<NotificationInfo> notifications;
	private TextView noNotificationsText;
	private ListView notificationList;
	private NotificationAdapter notificationAdapter;
	private Switch enableNotificationsSwitch;
	private RelativeLayout notificationsEnabledLayout;
	private boolean userRequestedNotificationsEnabled = false;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_notifications, container, false);
		
		notifications = new ArrayList<NotificationInfo>();
		
		// configure listview that holds notifications
		ListView notificationList = (ListView) v.findViewById(R.id.notification_listview);
		notificationList.setOverScrollMode(View.OVER_SCROLL_NEVER);
		//notificationList.setDividerHeight(0);
		notificationAdapter = new NotificationAdapter(getActivity(), notifications);
		notificationList.setAdapter(notificationAdapter);
		
		// allow user to edit notification
		notificationList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
				NotificationType notifyInfo = notifications.get(position).getNotificationType();
				NotificationConfigPacket notifyConfig = notifications.get(position).getNotificationConfig();
				
				Intent i = new Intent(getActivity(), ConfigureNotificationDialog.class);
				i.putExtra("notificationName", notifyInfo.getNotificationTitle());
				i.putExtra("notificationId", notifyConfig.getNotificationId());
				i.putExtra("notificationEnabled", notifyConfig.getEnabled());
				i.putExtra("notificationPattern", notifyConfig.getNotificationType());
				i.putExtra("notificationHue", notifyConfig.getNotificationHue());
				i.putExtra("notificationBrightness", notifyConfig.getNotificationBrightness());
				i.putExtra("notificationSaturation", notifyConfig.getNotificationSaturation());
				i.putExtra("notificationSpeed", notifyConfig.getNotificationSpeed());
				i.putExtra("notificationDuration", notifyConfig.getNotificationDuration());
				i.putExtra("notificationSticky", notifyConfig.getNotificationSticky());
				
				startActivity(i);
			}
		});
		
		return v;
	}
	
	private void multipleLamps() {
		noNotificationsText.setText("You must be connected to only one lamp to configure notifications.");
		noNotificationsText.setVisibility(View.VISIBLE);
		notificationList.setVisibility(View.GONE);
		notificationsEnabledLayout.setVisibility(View.GONE);
	}
	
	private void singleLamp() {
		notifications.clear();
		
		// check if we're actually bonded to this lamp and have android notification support
		if (LampManager.getInstanceIfReady().getSelectedLampIfExists().isBonded() && NotificationFactory.checkForNotificationPermission(getActivity()) && LampManager.getInstanceIfReady().getSelectedLampIfExists().areNotificationsEnabled()) {
			// we're good!
			enableNotificationsSwitch.setChecked(true);
		} else {
			noNotificationsText.setVisibility(View.GONE);
			notificationList.setVisibility(View.GONE);
			notificationsEnabledLayout.setVisibility(View.VISIBLE);
			enableNotificationsSwitch.setChecked(false);
			
			return;
		}
		
		// create NotificationInfo objects from notifications configured on lamp
		for (int i=0; i<NotificationTypes.getNotificationTypes().size(); i++) {
			final NotificationType notifyType = NotificationTypes.getNotificationTypes().get(i);
			final int notificationId = NotificationTypes.getNotificationTypes().get(i).getNotificationId();

			LampManager.getInstanceIfReady().getSelectedLampIfExists().getNotificationConfig(notificationId, new NotificationConfigCallback() {
				@Override
				public void onSuccess(NotificationConfigPacket notificationConfig) {
					notifications.add(new NotificationInfo(notifyType, notificationConfig));
					
					notificationAdapter.notifyDataSetChanged();
				}

				@Override
				public void onFail() {
					// notification doesn't exist, create w/ defaults
					NotificationConfigPacket notificationConfig = new NotificationConfigPacket(
							notificationId, false, NotificationAdapter.NOTIFICATION_HALO, 0, 255, 255, 30, 3, false);
					
					notifications.add(new NotificationInfo(notifyType, notificationConfig));
					
					notificationAdapter.notifyDataSetChanged();
				}
			});
		}
		
		noNotificationsText.setVisibility(View.GONE);
		notificationList.setVisibility(View.VISIBLE);
		notificationsEnabledLayout.setVisibility(View.VISIBLE);
	}
	
	private void noLamps() {
		noNotificationsText.setText("Connect to a lamp to configure notifications.");
		noNotificationsText.setVisibility(View.VISIBLE);
		notificationList.setVisibility(View.GONE);
		notificationsEnabledLayout.setVisibility(View.GONE);
	}
	
	private void notificationsEnableFailed() {
		enableNotificationsSwitch.setChecked(false);
		Toast.makeText(getActivity(), "failed to enable notifications", Toast.LENGTH_LONG).show();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		noNotificationsText = (TextView) getActivity().findViewById(R.id.no_notifications_text);
		notificationList = (ListView) getActivity().findViewById(R.id.notification_listview);
		enableNotificationsSwitch = (Switch) getActivity().findViewById(R.id.notificationsEnabledSwitch);
		notificationsEnabledLayout = (RelativeLayout) getActivity().findViewById(R.id.notificationsEnabledLayout);
		
		// default state
		enableNotificationsSwitch.setChecked(false);
		
		// listen for switch changes
		enableNotificationsSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (LampManager.getInstanceIfReady().getSelectedLampIfExists() == null)
					return;
				
				if (isChecked) {
					if (!NotificationFactory.checkForNotificationPermission(getActivity())) {
						userRequestedNotificationsEnabled = true;
						NotificationFactory.launchNotificationPermissionSettings(getActivity());
					} else if (!LampManager.getInstanceIfReady().getSelectedLampIfExists().isBonded()) {
						LampManager.getInstanceIfReady().getSelectedLampIfExists().beginBonding(new SuccessFailCallback() {
							@Override
							public void onSuccess() {
								if (!LampManager.getInstanceIfReady().getSelectedLampIfExists().areNotificationsEnabled()) {
									LampManager.getInstanceIfReady().getSelectedLampIfExists().setNotificationsEnabled(true, new SuccessFailCallback() {
										@Override
										public void onSuccess() {
											// we should be good, rerun this method to find out
											singleLamp();
										}

										@Override
										public void onFail() {
											notificationsEnableFailed();
										}
									});
									
									
								} else {
									// we're all set
									singleLamp();
								}
							}

							@Override
							public void onFail() {
								notificationsEnableFailed();
							}
						});
					} else if (!LampManager.getInstanceIfReady().getSelectedLampIfExists().areNotificationsEnabled()) {
						LampManager.getInstanceIfReady().getSelectedLampIfExists().setNotificationsEnabled(true, new SuccessFailCallback() {
							@Override
							public void onSuccess() {
								// we should be good, rerun this method to find out
								singleLamp();
							}

							@Override
							public void onFail() {
								notificationsEnableFailed();
							}
						});
					}
				} else {
					// the user wants us to disable notifications
					LampManager.getInstanceIfReady().getSelectedLampIfExists().setNotificationsEnabled(false, new SuccessFailCallback() {
						@Override
						public void onSuccess() {
							// clear out the list
							notifications.clear();
							notificationAdapter.notifyDataSetChanged();
						}

						@Override
						public void onFail() {
							// we weren't able to disable
							enableNotificationsSwitch.setChecked(true);
						}
					});
				}
			}
		});
		
		// setup listener for lamp selection so we can update the view on changes
		LampManager lampMgr = LampManager.getInstanceIfReady();
		lampMgr.addListener(this);
		
		if (lampMgr.areMultipleLampsSelected()) {
			multipleLamps();
		} else if (lampMgr.areNoLampsSelected()) {
			noLamps();
		} else {
			singleLamp();
		}
		
		// since the user already slid the switch, we should continue the notification enable process (bond + set global boolean)
		if (userRequestedNotificationsEnabled && NotificationFactory.checkForNotificationPermission(getActivity())) {
			userRequestedNotificationsEnabled = false;
			
			if (LampManager.getInstanceIfReady().getSelectedLampIfExists() == null)
				return;
			
			if (!LampManager.getInstanceIfReady().getSelectedLampIfExists().isBonded()) {
				LampManager.getInstanceIfReady().getSelectedLampIfExists().beginBonding(new SuccessFailCallback() {
					@Override
					public void onSuccess() {
						if (!LampManager.getInstanceIfReady().getSelectedLampIfExists().areNotificationsEnabled()) {
							LampManager.getInstanceIfReady().getSelectedLampIfExists().setNotificationsEnabled(true, new SuccessFailCallback() {
								@Override
								public void onSuccess() {
									// we should be good, rerun this method to find out
									singleLamp();
								}

								@Override
								public void onFail() {
									notificationsEnableFailed();
								}
							});
						} else {
							// we're all set
							singleLamp();
						}
					}

					@Override
					public void onFail() {
						notificationsEnableFailed();
					}
				});
			} else if (userRequestedNotificationsEnabled) {
				// the user declined our app access to system notifications, don't continue
				userRequestedNotificationsEnabled = false;
				enableNotificationsSwitch.setChecked(false);
			}
		}
	}
	
	@Override
	public void onPause() {
		enableNotificationsSwitch.setOnCheckedChangeListener(null);
		
		LampManager.getInstanceIfReady().removeListener(this);
		
		super.onPause();
	}

	@Override
	public void singleLampSelected(Lamp lamp) {
		singleLamp();
	}

	@Override
	public void multipleLampsSelected(ArrayList<Lamp> lamps) {
		multipleLamps();
	}

	@Override
	public void noLampsSelected() {
		noLamps();
	}

	@Override
	public void onLampListUpdate() {

	}

	@Override
	public void onMoodChange(int moodId) {

	}

	@Override
	public void onWeatherLocationUpdate() {

	}

	@Override
	public void onDfuLampListUpdate() {

	}
}

