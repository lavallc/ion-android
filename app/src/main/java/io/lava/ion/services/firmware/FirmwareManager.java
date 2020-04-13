package io.lava.ion.services.firmware;

import io.lava.ion.logger.Logger;
import io.lava.ion.utility.Now;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public class FirmwareManager {
	// singleton instance
	private static FirmwareManager instance;
	
	private Context mContext;
		
	private HashMap<Integer, FirmwareProtocolMetadata> latestMetadataMap;
	
	private int latestProtocol = -1;
		
	private long lastUpdated = 0;
	
	private Handler mCallbackHandler;
	
	private String lastUpdateText = "Tap the button below to check.";
	
	// anyone interested in firmware data updates will be in this list
	private ArrayList<IFirmwareManagerListener> listeners;
		
	public static FirmwareManager getInstance(Context context) {
		if (instance == null) {
			instance = new FirmwareManager();
			instance.mContext = context;
			instance.listeners = new ArrayList<IFirmwareManagerListener>();
			instance.latestMetadataMap = new HashMap<Integer, FirmwareProtocolMetadata>();
			instance.mCallbackHandler = new Handler(Looper.getMainLooper());
		}

		return instance;
	}

	// This is simply to block other classes from instantiating
	private FirmwareManager() {
	}
	
	public void addListener(IFirmwareManagerListener listener) {
		if (listener != null)
			listeners.add(listener);
	}
	
	public void removeListener(IFirmwareManagerListener removeListener) {
		for (IFirmwareManagerListener listener : listeners) {
			if (removeListener == listener) {
				listeners.remove(removeListener);
				return;
			}
		}
	}
	
	public int getLatesetProtocolVersion() {
		return latestProtocol;
	}
	
	public FirmwareProtocolMetadata getLatestMetadataForProtocolVersion(int protocol) {
		return latestMetadataMap.get(protocol);
	}
	
	public boolean forceUpdate() {
		if (FirmwareUpdateChecker.getInstanceIfExists() != null) {
			return FirmwareUpdateChecker.getInstanceIfExists().forceUpdate();
		}
		return false;
	}
	
	public long getLastUpdated() {
		return lastUpdated;
	}
	
	public String getStatusText() {
		return lastUpdateText;
	}
	
	public void updateStatusText(final String status) {
		mCallbackHandler.post(new Runnable() {
	        public void run() {
				lastUpdateText = status;
				
				// call delegate that new firmware data is available
				for (IFirmwareManagerListener listener : listeners) {
					// let all our listeners know
					try {
						listener.onFirmwareDataUpdate();
					} catch (Exception e) {}
				}
	        }
		});
	}
	
	public void receivedNewJSON(final JSONObject json) {
		mCallbackHandler.post(new Runnable() {
	        public void run() {
				try {
					// parse update JSON
					String base_firmware_url = json.getString("base_firmware_url");
					JSONArray protocolsArray = json.getJSONArray("latest_firmware_by_protocol");
					
					for (int i=0; i<protocolsArray.length(); i++) {
						JSONObject protocolObject = protocolsArray.getJSONObject(i);
						
						int protocolVer = protocolObject.getInt("firmware_protocol");
						int firmwareVer = protocolObject.getInt("firmware_version");
						String sha256 = protocolObject.getString("firmware_sha256");
						String url = base_firmware_url + Integer.toString(firmwareVer) + ".bin";
						
						FirmwareProtocolMetadata fwMeta = new FirmwareProtocolMetadata(protocolVer, firmwareVer, sha256, url);

                        FirmwareProtocolMetadata lastMeta = latestMetadataMap.get(protocolVer);

                        // only update the protocol map if the current value is null, or this firmware version number is higher (but the same protocol)
                        if (lastMeta == null || (lastMeta.getProtocolVersion() == protocolVer && lastMeta.getFirmwareVersion() < firmwareVer)) {
                            latestMetadataMap.put(protocolVer, fwMeta);
                        }
						
						// keep track of greatest protocol version
						if (protocolVer > latestProtocol)
							latestProtocol = protocolVer;
					}
					
					lastUpdated = Now.get();
					
					Calendar c = Calendar.getInstance();
					SimpleDateFormat timestamp = new SimpleDateFormat("MM/dd/yyyy hh:mma");
					timestamp.setCalendar(c);
					
					//updateStatusText("Last Updated at " + timestamp.format(c.getTime()));
					
					// call delegate that new firmware data is available
					for (IFirmwareManagerListener listener : listeners) {
						// let all our listeners know
						try {
							listener.onFirmwareDataUpdate();
						} catch (Exception e) {}
					}
					
					Logger.i(this.getClass().getSimpleName(), "RECEIVED FIRMWARE JSON SUCCESSFULLY @ " + timestamp.format(c.getTime()));
				} catch (Exception e) {
					Logger.i(this.getClass().getSimpleName(), e.toString());
					updateStatusText("Failed to receive updates");
				}
	        }
		});
	}
	
	public void requestFirmware(final FirmwareProtocolMetadata fwMeta) {
		new Thread() {
            @Override
            public void run() {
            	// in debug mode, always redownload firmware
            	if (Logger.isLoggingEnabled()) {
            		File firmwareImage = new File(mContext.getFilesDir() + "/fw", "ion_" + Integer.toString(fwMeta.getFirmwareVersion()) + ".bin");
            		firmwareImage.delete();
            	}
            	
            	// force re-download always in DEBUG
				if (isFirmwareDownloaded(fwMeta)) {
					sendFirmwareFileToListeners(fwMeta);
				} else {
					downloadFirmware(fwMeta);
				}
            }
		}.start();
	}
	
	// returns true if the latest firmware is downloaded and VALID
	// ONLY call this from a background thread
	private boolean isFirmwareDownloaded(FirmwareProtocolMetadata fwMeta) {
		// get reference to file (spoiler alert, it might not exist)
		File firmwareImage = new File(mContext.getFilesDir() + "/fw", "ion_" + Integer.toString(fwMeta.getFirmwareVersion()) + ".bin");
		
		try {
    		// do we have the latest firmware locally downloaded?
    		if (!firmwareImage.exists()) {
    			// file does not even exist
    			return false;
    		}
    		
    		// calculate checksum
    		String calculatedSha256 = computeHash(FileUtils.readFileToByteArray(firmwareImage));
    		
    		if (calculatedSha256.equalsIgnoreCase(fwMeta.getSha256())) {
    			// VALID sha256
    			return true;
    		} else {
    			Logger.i(this.getClass().getSimpleName(), "FAILED TO VALIDATE FIRMWARE");
    			
    			// remove the corrupt file
    			firmwareImage.delete();
    			
    			// invalid sha256
    			return false;
    		}
        } catch (Exception e) {
        	Logger.i(this.getClass().getSimpleName(), "isFirmwareDownloaded Exception: " + e.toString());
        	
        	// something isn't right, let's clear this mess out
        	firmwareImage.delete();
        	
			return false;
        }
	}
	
	// this must be called AFTER isFirmwareDownloaded()
	private void downloadFirmware(final FirmwareProtocolMetadata fwMeta) {
    	// create file and directories
		File firmwareImg = new File(mContext.getFilesDir() + "/fw", "ion_" + Integer.toString(fwMeta.getFirmwareVersion()) + ".bin");
		firmwareImg.mkdirs();
		
		// clear out anything that already exists
		firmwareImg.delete();
		
		InputStream input = null;
		FileOutputStream output = null;

		try {
			URL url = new URL(fwMeta.getDownloadUrl());
			URLConnection connection = url.openConnection();
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);
			connection.connect();
			final int fileSize = connection.getContentLength();
			
			// download the file
			input = new BufferedInputStream(url.openStream());
			output = new FileOutputStream(firmwareImg);

			byte data[] = new byte[1024];
			int count;
			int bytesRetrieved = 0;
			
			while ((count = input.read(data)) != -1) {
				output.write(data, 0, count);
				bytesRetrieved += count;
				
				// progress callbacks
				final int currentBytesRetrieved = bytesRetrieved;
				
				// notify listeners of firmware download progress
				mCallbackHandler.post(new Runnable() {
			        public void run() {
						for (IFirmwareManagerListener listener : listeners) {
							// let all our listeners know
							try {
					    		listener.onFirmwareDownloadProgress((float)currentBytesRetrieved/(float)fileSize);
							} catch (Exception e) {}
						}
			        }
			    });
			}
			
			output.close();
			
			// validate and callback now that we've downloaded something
			if (isFirmwareDownloaded(fwMeta)) {
				sendFirmwareFileToListeners(fwMeta);
			} else {
				notifyListenersOfFailure();
			}
		} catch (Exception e) {
			try {
				output.close();
			} catch (IOException ioExcpt) {}
			
			// delete partial files
			firmwareImg.delete();
    		
    		notifyListenersOfFailure();
		}
	}
	
	private void notifyListenersOfFailure() {
		mCallbackHandler.post(new Runnable() {
	        public void run() {
				for (IFirmwareManagerListener listener : listeners) {
					// let all our listeners know
					try {
			    		listener.failedToRetrieveFirmware();
					} catch (Exception e) {}
				}
	        }
		});
	}
	
	private void sendFirmwareFileToListeners(final FirmwareProtocolMetadata fwMeta) {
		final File currentFirmware = new File(mContext.getFilesDir() + "/fw", "ion_" + Integer.toString(fwMeta.getFirmwareVersion()) + ".bin");
		
		mCallbackHandler.post(new Runnable() {
	        public void run() {
	        	// call delegate that new firmware data is available
	    		for (IFirmwareManagerListener listener : listeners) {
	    			// let all our listeners know
	    			try {
	    				if (currentFirmware.exists())
	    	    			listener.firmwareIsReady(fwMeta, currentFirmware);
	    	    		else
	    	    			listener.failedToRetrieveFirmware();
	    			} catch (Exception e) {}
	    		}
	        }
	    });
	}
	
	private static String computeHash(byte[] bytes) throws NoSuchAlgorithmException, UnsupportedEncodingException{
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
	    digest.reset();

	    byte[] byteData = digest.digest(bytes);
	    StringBuffer sb = new StringBuffer();

	    for (int i = 0; i < byteData.length; i++){
	      sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
	    }
	    return sb.toString();
	}
}
