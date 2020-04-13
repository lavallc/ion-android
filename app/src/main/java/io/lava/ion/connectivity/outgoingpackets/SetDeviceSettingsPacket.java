package io.lava.ion.connectivity.outgoingpackets;

import io.lava.ion.connectivity.PacketConstants.OpCode;

public class SetDeviceSettingsPacket extends BaseOutgoingPacket {
	private int reqId, shuffleTime, quietHourStart, quietMinuteStart, quietHourEnd, quietMinuteEnd;
	private boolean knockEnabled, leashEnabled, notificationsEnabled, quietEnabled;
	
	public SetDeviceSettingsPacket(boolean knockEnabled, int shuffleTime, boolean leashEnabled, boolean notificationsEnabled, boolean quietEnabled, int quietHourStart, int quietMinuteStart, int quietHourEnd, int quietMinuteEnd) {
		this.knockEnabled = knockEnabled;
		this.quietEnabled = quietEnabled;
		this.shuffleTime = shuffleTime;
		this.leashEnabled = leashEnabled;
		this.notificationsEnabled = notificationsEnabled;
		
		this.quietHourStart = quietHourStart;
		this.quietMinuteStart = quietMinuteStart;
		this.quietHourEnd = quietHourEnd;
		this.quietMinuteEnd = quietMinuteEnd;
	}

	@Override
	public void setReqId(int reqId) {
		this.reqId = reqId;
	}

	@Override
	public byte[] toBytes() {
		return new byte[] {
			(byte)reqId,										// request ID
			(byte)OpCode.setDeviceSettings,						// op code
			(byte)(knockEnabled ? 0x01 : 0x00), 				// knock
			(byte)(quietEnabled ? 0x01 : 0x00), 				// quiet enabled
			(byte)quietHourStart,								// quiet hour start
			(byte)quietMinuteStart,								// quiet minute start
			(byte)quietHourEnd,									// quiet hour end
			(byte)quietMinuteEnd,								// quiet minute end
			(byte)shuffleTime, 									// shuffle
			(byte)(notificationsEnabled ? 0x01 : 0x00), 		// notifications
			(byte)(leashEnabled ? 0x01 : 0x00) 					// leash
		};
	}

}
