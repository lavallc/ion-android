package io.lava.ion.notifications;

import io.lava.ion.logger.Logger;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class SystemNotificationReceiver extends NotificationListenerService {
    private static final String TAG = "SystemNotificationReceiver";
    
    // 3rd party
    private static final String FACEBOOK_PACKAGE = "com.facebook.katana";
    private static final String FACEBOOK_MESSENGER_PACKAGE = "com.facebook.orca";
    private static final String TWITTER_PACKAGE = "com.twitter.android";
    private static final String WHATSAPP_PACKAGE = "com.whatsapp";
    private static final String SNAPCHAT_PACKAGE = "com.snapchat.android";
    private static final String MOTO_EMAIL_PACKAGE = "com.motorola.motoemail";
    private static final String SKYPE_PACKAGE = "com.skype.raider";
    private static final String KIK_PACKAGE = "kik.android";
    private static final String INSTAGRAM_PACKAGE = "com.instagram.android";
    private static final String HTC_ALARM_CLOCK_PACKAGE = "com.htc.android.worldclock";
    
    // google apps
    private static final String GMAIL_PACKAGE = "com.google.android.gm";
    private static final String HANGOUTS_PACKAGE = "com.google.android.talk";
    private static final String GVOICE_PACKAGE = "com.google.android.apps.googlevoice";
    private static final String GOOGLE_PLUS_PACKAGE = "com.google.android.apps.plus";
    private static final String GOOGLE_CALENDAR_PACKAGE = "com.google.android.calendar";
    private static final String CALENDAR_PACKAGE = "com.android.calendar";
    private static final String EMAIL_PACKAGE = "com.android.email";
    
    
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
    	String packageName = sbn.getPackageName();
    	Logger.d(TAG, "Got notification from: " + packageName);
    	
    	if (packageName.equals(FACEBOOK_PACKAGE)) {
    		CentralNotificationReceiver.getInstance().onFacebook();
    	} else if (packageName.equals(FACEBOOK_MESSENGER_PACKAGE)) {
    		CentralNotificationReceiver.getInstance().onFacebookMessage();
    	} else if (packageName.equals(TWITTER_PACKAGE)) {
    		CentralNotificationReceiver.getInstance().onTwitter();
    	} else if (packageName.equals(WHATSAPP_PACKAGE)) {
    		CentralNotificationReceiver.getInstance().onWhatsApp();
    	} else if (packageName.equals(SNAPCHAT_PACKAGE)) {
    		CentralNotificationReceiver.getInstance().onSnapchat();
    	} else if (packageName.equals(GMAIL_PACKAGE) || packageName.equals(MOTO_EMAIL_PACKAGE) || packageName.equals(EMAIL_PACKAGE)) {
    		CentralNotificationReceiver.getInstance().onEmail();
    	} else if (packageName.equals(SKYPE_PACKAGE)) {
    		CentralNotificationReceiver.getInstance().onSkype();
    	} else if (packageName.equals(KIK_PACKAGE)) {
    		CentralNotificationReceiver.getInstance().onKik();
    	} else if (packageName.equals(INSTAGRAM_PACKAGE)) {
    		CentralNotificationReceiver.getInstance().onInstagram();
    	} else if (packageName.equals(HANGOUTS_PACKAGE)) {
    		CentralNotificationReceiver.getInstance().onHangouts();
    	} else if (packageName.equals(GVOICE_PACKAGE)) {
    		CentralNotificationReceiver.getInstance().onGoogleVoice();
    	} else if (packageName.equals(GOOGLE_PLUS_PACKAGE)) {
    		CentralNotificationReceiver.getInstance().onGooglePlus();
    	} else if (packageName.equals(GOOGLE_CALENDAR_PACKAGE) || packageName.equals(CALENDAR_PACKAGE)) {
            CentralNotificationReceiver.getInstance().onCalendar();
        } else if (packageName.equals(HTC_ALARM_CLOCK_PACKAGE)) {
            CentralNotificationReceiver.getInstance().onAlarm();
    	} else {
    		CentralNotificationReceiver.getInstance().onCatchAllOthers();
    	}
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
    	// we don't care
    }
}