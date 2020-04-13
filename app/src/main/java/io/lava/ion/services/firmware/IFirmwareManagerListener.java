package io.lava.ion.services.firmware;

import java.io.File;


public interface IFirmwareManagerListener {
	public abstract void onFirmwareDataUpdate();
	public abstract void firmwareIsReady(FirmwareProtocolMetadata fwMeta, File firmware);
	public abstract void failedToRetrieveFirmware();
	public abstract void onFirmwareDownloadProgress(float progress);
}