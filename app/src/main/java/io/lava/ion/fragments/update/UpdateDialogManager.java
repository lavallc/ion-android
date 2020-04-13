package io.lava.ion.fragments.update;

import io.lava.ion.connectivity.LampManager;
import io.lava.ion.connectivity.lamp.Lamp;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class UpdateDialogManager {
	private static UpdateDialogManager instance;
	
	private UpdateDialogManagerDelegate delegate;
	
	private boolean appVisible = false;
	private boolean lampUpdateDialogVisible = false;
	
	// should a lamp update start immediately?
	private boolean firmwareUpdateAcknowledgedByUser = false;
	
	private Context mContext;
	
	public static UpdateDialogManager getInstance(Context context) {
		if (instance == null) {
			instance = new UpdateDialogManager();
		}

        // update context
        instance.mContext = context;
		
		return instance;
	}
	
	private UpdateDialogManager() {
		// block public instantiation
	}
	
	public void appBecameVisible() {
		appVisible = true;
	}
	
	public void appBecameHidden() {
		appVisible = false;
	}
	
	public void setDelegate(UpdateDialogManagerDelegate dg) {
		delegate = dg;
	}
	
	public boolean hasFirmwareUpdateBeenAcceptedByUser() {
		return firmwareUpdateAcknowledgedByUser;
	}
	
	public void updateHasBegun() {
		firmwareUpdateAcknowledgedByUser = false;
	}
	
	public void createAppOutOfDateDialogIfAppVisible() {
		if (appVisible && !lampUpdateDialogVisible) {
			lampUpdateDialogVisible = true;
			
			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			
			builder.setTitle("App Out of Date");
			builder.setMessage("Sorry! Your ION is running newer software than this app supports. Please update from the Google Play Store.");
			
			builder.setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					lampUpdateDialogVisible = false;
					
	                dialog.dismiss();
				}
			});

			// Create the AlertDialog
			AlertDialog dialog = builder.create();
			dialog.show();
		}
	}
	
	public void createLampOutOfDateDialogIfAppVisible(final Lamp lamp, final boolean required) {
		if (appVisible && !lampUpdateDialogVisible && !firmwareUpdateAcknowledgedByUser) {
			lampUpdateDialogVisible = true;
			
			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			
			builder.setTitle("ION Firmware Update");
			if (required)
				builder.setMessage("A required firmware update has been detected for your ION. Would you like update now?");
			else
				builder.setMessage("An optional firmware update has been detected for your ION. Would you like update now?");
			
			builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					// the update should start as soon as the update fragment loads
					firmwareUpdateAcknowledgedByUser = true;
					
					lamp.enterDFUMode();
					
					// jump to update fragment
					LampManager.getInstanceIfReady().switchToUpdateFragment();
					
					lampUpdateDialogVisible = false;
					
					// let our delegate know that a lamp is ready to update
					if (delegate != null)
						delegate.lampWantsToUpdate();
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                	if (required) {
                		// force a disconnect if required
                		lamp.onInitFailed();
                	}
                		
                	lampUpdateDialogVisible = false;
                	
                	dialog.dismiss();
                }
            });

			// Create the AlertDialog
			AlertDialog dialog = builder.create();
			dialog.show();
		}
	}
}
