package io.lava.ion.fragments.update;

import io.lava.ion.R;
import io.lava.ion.connectivity.ILampManagerListener;
import io.lava.ion.connectivity.LampManager;
import io.lava.ion.connectivity.lamp.Lamp;
import io.lava.ion.firmware.DFULamp;
import io.lava.ion.firmware.IDFULampDelegate;
import io.lava.ion.logger.Logger;
import io.lava.ion.services.firmware.FirmwareManager;
import io.lava.ion.services.firmware.FirmwareProtocolMetadata;
import io.lava.ion.services.firmware.IFirmwareManagerListener;

import java.io.File;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class UpdateFragment extends Fragment implements IDFULampDelegate, ILampManagerListener, IFirmwareManagerListener, UpdateDialogManagerDelegate {
	// should open the recovery dialog when firmware data is available
	private Button recoverBtn;
	
	// checks for updates w/ server
	private Button updateBtn;
	
	// displays to user the status of connected lamps or update info
	private TextView updateText;
	
	// if we are waiting on a response from the firmware manager to give us new metadata
	private boolean isReloadingMetadata = false;
	
	// for when we need to run things on the UI thread
	private Handler uiHandler;
	
	// should be postDelayed when we need a timeout for trying to connect to an ION
	private Runnable cancelDialogRunnable;
	
	// if recovery is also performing a factory settings restore
	private boolean isPerformingFactoryErase = false;
	
	// our current lamp (if there is one) that we are updating
	private DFULamp dfuLamp;
	
	// our progress dialog for firmware update % (if currently updating), null if not visible
	private ProgressDialog firmwareUpdateProgressDialog = null;
	
	// are we currently updating a lamp?
	private boolean isUpdatingFirmware = false;
	
	// are we trying to update a lamp right now?
	private boolean pendingUpdate = false;
	
	// are we trying to update a lamp in recovery (debug mode)?
	private boolean pendingDebugUpdate = false;
	

	private Context getCtx() {
		return getActivity();
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        uiHandler = new Handler(Looper.getMainLooper());

        cancelDialogRunnable = new Runnable() {
            @Override
            public void run() {
                // reset state
                isUpdatingFirmware = false;
                isPerformingFactoryErase = false;

                // clear update dialog manager state
                UpdateDialogManager.getInstance(getCtx()).updateHasBegun();

                // stop trying to find an ION in DFU
                LampManager.getInstanceIfReady().setLampDiscoveryContinuous(false);

                // clear out our progress dialog
                if (firmwareUpdateProgressDialog != null) {
                    firmwareUpdateProgressDialog.dismiss();
                    firmwareUpdateProgressDialog = null;
                }

                // update status text
                updateFirmwareTextLabelStatus();

                // notify the user
                Toast.makeText(getCtx(), "could not find ION", Toast.LENGTH_LONG).show();
            }
        };
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_recovery, container, false);
		
		recoverBtn = (Button)v.findViewById(R.id.recoverBtn);
		recoverBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				// always update metadata before flashing if in debug mode
				if (Logger.isLoggingEnabled()) {
					pendingDebugUpdate = true;
					Toast.makeText(getCtx(), "contacting server...", Toast.LENGTH_SHORT).show();
					updateMetadata();
					return;
				}
				
				// confirmation from user to explain recovery further
				launchRecoveryDialog();
			}
		});
		
		updateBtn = (Button)v.findViewById(R.id.updateBtn);
		updateBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				updateMetadata();
				
				updateFirmwareTextLabelStatus();
			}
		});
		
		updateText = (TextView)v.findViewById(R.id.update_text);

		return v;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		// setup listeners / delegates
		UpdateDialogManager.getInstance(getCtx()).setDelegate(this);
		LampManager.getInstanceIfReady().addListener(this);
		FirmwareManager.getInstance(getActivity()).addListener(this);
		
		// if an update is pending, let's begin
		if (UpdateDialogManager.getInstance(getCtx()).hasFirmwareUpdateBeenAcceptedByUser()) {
			launchUpdateDialog();
		}
		
		if (FirmwareManager.getInstance(getActivity()).getLastUpdated() > 0) {
			recoverBtn.setEnabled(true);
			updateBtn.setEnabled(true);
		} else {
			recoverBtn.setEnabled(false);
		}
		
		updateFirmwareTextLabelStatus();
	}
	
	@Override
	public void onPause() {
		pendingUpdate = false;
		
		// remove listeners / delegates
		UpdateDialogManager.getInstance(getCtx()).setDelegate(this);
		LampManager.getInstanceIfReady().removeListener(this);
		FirmwareManager.getInstance(getActivity()).removeListener(this);
		
		super.onPause();
	}
	
	private void updateFirmwareTextLabelStatus() {
		// we do not have metadata information
		if (FirmwareManager.getInstance(getActivity()).getLastUpdated() == 0 || isReloadingMetadata) {
			updateText.setText(FirmwareManager.getInstance(getActivity()).getStatusText());
			return;
		}
		
		// check for connected lamps to see if they are out of date (we already know the protocol versions match)
		for (Lamp l : LampManager.getInstanceIfReady().getReadyLamps()) {
			if (l.getFirmwareVersion() < FirmwareManager.getInstance(getActivity()).getLatestMetadataForProtocolVersion(Lamp.EXPECTED_PROTOCOL_VERSION).getFirmwareVersion()) {
				updateText.setText("Firmware update detected!");
				// ask the user if they'd like to update
				if (!isUpdatingFirmware && !pendingUpdate && !pendingDebugUpdate) {
                    UpdateDialogManager.getInstance(getCtx()).createLampOutOfDateDialogIfAppVisible(l, false);
                }
				
				return;
			}
		}
		
		if (firmwareUpdateProgressDialog != null) {
			updateText.setText("Updating ION...");
		} else if (LampManager.getInstanceIfReady().getReadyLamps().size() == 0) {
			updateText.setText("Connect to a lamp to check for updates.");
		} else {
			updateText.setText("All connected IONs are up to date!");
		}
	}

	@Override
	public void DFUTargetDidConnect() {

	}

	@Override
	public void DFUTargetDidDisconnect() {

	}

	@Override
	public void DFUTargetDidSendDataWithProgress(float progressPercent) {
		if (firmwareUpdateProgressDialog != null) {
			firmwareUpdateProgressDialog.setProgress(Math.round(progressPercent*90)+10);
		}
	}

	@Override
	public void DFUTargetDidFailFlash() {
		onFirmwareUpdateFail();
	}

	@Override
	public void DFUTargetDidFinishFlash() {
		onFirmwareUpdateComplete();
	}

	@Override
	public void DFUTargetNotFound() {
		onFirmwareUpdateFail();
	}
	
	private void launchRecoveryDialog() {
		View checkBoxView = View.inflate(getActivity(), R.layout.checkbox, null);
		CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);
		checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

		    @Override
		    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		    	if (isChecked)
		    		isPerformingFactoryErase = true;
		    	else
		    		isPerformingFactoryErase = false;
		    }
		});
		checkBox.setText("Restore Factory Settings");

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		    builder.setTitle("ION Recovery");
		    builder.setMessage("If your ION is still behaving erratically after recovery, try restoring to factory settings.")
		           .setView(checkBoxView)
		           .setCancelable(false)
		           .setPositiveButton("Recover", new DialogInterface.OnClickListener() {
		               public void onClick(DialogInterface dialog, int id) {
		            	   dialog.dismiss();
		            	   launchUpdateDialog();
		               }
		           })
		           .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		               public void onClick(DialogInterface dialog, int id) {
		                    dialog.cancel();
		               }
		           })
		           .show();
	}
	
	private void updateMetadata() {
		updateBtn.setEnabled(false);
		isReloadingMetadata = true;
		FirmwareManager.getInstance(getActivity()).forceUpdate();
	}
	
	private void launchUpdateDialog() {
		if (!isUpdatingFirmware || pendingUpdate) {
			isUpdatingFirmware = true;
			
			// check if firmware metadata is up to date
			if (FirmwareManager.getInstance(getActivity()).getLastUpdated() == 0 && !isReloadingMetadata) {
				// start a metadata update
				updateMetadata();
				pendingUpdate = true;
				return;
			} else if (FirmwareManager.getInstance(getActivity()).getLastUpdated() == 0 && isReloadingMetadata) {
				// metadata is currently updating right now
				return;
			}
			
			if (firmwareUpdateProgressDialog == null) {
				// show lamp is updating text
				updateFirmwareTextLabelStatus();
				
				firmwareUpdateProgressDialog = new ProgressDialog(getActivity());
				firmwareUpdateProgressDialog.setMax(100);
				firmwareUpdateProgressDialog.setCancelable(false);
				firmwareUpdateProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				firmwareUpdateProgressDialog.setIndeterminate(false);
				firmwareUpdateProgressDialog.setTitle("Updating ION");
				firmwareUpdateProgressDialog.setProgressNumberFormat(null);
				
				if (LampManager.getInstanceIfReady().getAllDfuLamps().size() > 0) {
					firmwareUpdateProgressDialog.setMessage("Downloading firmware...");
					firmwareUpdateProgressDialog.show();
					dfuLamp = LampManager.getInstanceIfReady().getAllDfuLamps().get(0);
					
					beginFirmwareUpdate(dfuLamp);
				} else {
					firmwareUpdateProgressDialog.setMessage("Searching for ION...");
					firmwareUpdateProgressDialog.show();
					LampManager.getInstanceIfReady().setLampDiscoveryContinuous(true);
					
					// set 5 second timeout for dialog
					uiHandler.postDelayed(cancelDialogRunnable, 5000);
				}
			}
		}
	}
	
	private void beginFirmwareUpdate(final DFULamp lampToUpdate) {
        UpdateDialogManager.getInstance(getCtx()).updateHasBegun();

		dfuLamp = lampToUpdate;
		
		if (isPerformingFactoryErase)
			dfuLamp.setRestoreToFactorySettings(true);
		else
			dfuLamp.setRestoreToFactorySettings(false);
		
		// keep BLE quiet for updating
		LampManager.getInstanceIfReady().suspendDiscovery();
		LampManager.getInstanceIfReady().setLampDiscoveryContinuous(false);
		
		// request the firmware image file
		FirmwareProtocolMetadata fwMeta = FirmwareManager.getInstance(getActivity()).getLatestMetadataForProtocolVersion(Lamp.EXPECTED_PROTOCOL_VERSION);
		FirmwareManager.getInstance(getActivity()).requestFirmware(fwMeta);
	}
	
	private void onFirmwareUpdateComplete() {
		onUpdatingEnded();
		Toast.makeText(getCtx(), "firmware update successful", Toast.LENGTH_LONG).show();
	}
	
	private void onFirmwareUpdateFail() {
		onUpdatingEnded();
		Toast.makeText(getCtx(), "firmware update failed", Toast.LENGTH_LONG).show();
	}
	
	private void onUpdatingEnded() {
		dfuLamp = null;
		isUpdatingFirmware = false;
		isPerformingFactoryErase = false;
		LampManager.getInstanceIfReady().resumeDiscovery();
		
		if (firmwareUpdateProgressDialog != null) {
			firmwareUpdateProgressDialog.dismiss();
			firmwareUpdateProgressDialog = null;
		}
		
		updateFirmwareTextLabelStatus();
		
		// discover any lamps that were just updated
		LampManager.getInstanceIfReady().discoverLamps();
	}

	@Override
	public void onLampListUpdate() {

	}

	@Override
	public void onDfuLampListUpdate() {
		if (dfuLamp == null && LampManager.getInstanceIfReady().getAllDfuLamps().size() > 0 && isUpdatingFirmware) {
			// no need to kill the dialog now
			if (cancelDialogRunnable != null) {
				uiHandler.removeCallbacks(cancelDialogRunnable);
			}
			
			firmwareUpdateProgressDialog.setMessage("Downloading firmware...");
			firmwareUpdateProgressDialog.show();
			dfuLamp = LampManager.getInstanceIfReady().getAllDfuLamps().get(0);
			
			beginFirmwareUpdate(dfuLamp);
		}
	}

	@Override
	public void singleLampSelected(Lamp lamp) {
		updateFirmwareTextLabelStatus();
	}

	@Override
	public void multipleLampsSelected(ArrayList<Lamp> lamps) {
		updateFirmwareTextLabelStatus();
	}

	@Override
	public void noLampsSelected() {
		updateFirmwareTextLabelStatus();
	}

	@Override
	public void onMoodChange(int moodId) {

	}

	@Override
	public void onWeatherLocationUpdate() {

	}

	@Override
	public void lampWantsToUpdate() {
		if (firmwareUpdateProgressDialog == null)
			launchUpdateDialog();
	}

	
	
	
	@Override
	public void onFirmwareDataUpdate() {
		if (isReloadingMetadata) {
			isReloadingMetadata = false;
			
			// continue with updates if already started
			if (pendingUpdate) {
				launchUpdateDialog();
				pendingUpdate = false;
			} else if (pendingDebugUpdate) {
				launchRecoveryDialog();
				pendingDebugUpdate = false;
			}
		}
		
		if (FirmwareManager.getInstance(getActivity()).getLastUpdated() > 0) {
			recoverBtn.setEnabled(true);
		} else {
			recoverBtn.setEnabled(false);
		}
		
		updateBtn.setEnabled(true);
		updateFirmwareTextLabelStatus();
	}

	@Override
	public void firmwareIsReady(FirmwareProtocolMetadata fwMeta, File firmware) {
		if (isUpdatingFirmware) {
			firmwareUpdateProgressDialog.setProgress(10);
			firmwareUpdateProgressDialog.setMessage("Updating Firmware...");
			dfuLamp.setFirmware(firmware);
			dfuLamp.setDelegate(this);
			dfuLamp.connectAndFlash();
		}
	}

	@Override
	public void failedToRetrieveFirmware() {
		onFirmwareUpdateFail();
	}

	@Override
	public void onFirmwareDownloadProgress(float progressPercent) {
		if (firmwareUpdateProgressDialog != null) {
			firmwareUpdateProgressDialog.setMessage("Downloading firmware...");
			firmwareUpdateProgressDialog.setProgress(Math.round(progressPercent*10));
		}
	}
}

