package io.lava.ion.receivers;

import io.lava.ion.services.firmware.FirmwareUpdateChecker;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class IONFirmwareUpdateReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		// tell update checker to check
		if (FirmwareUpdateChecker.getInstanceIfExists() != null)
			FirmwareUpdateChecker.getInstanceIfExists().forceUpdate();
	}
}
