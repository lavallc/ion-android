package io.lava.ion.connectivity.outgoingpackets;

import io.lava.ion.connectivity.PacketConstants.OpCode;

public class SetWeatherPacket extends BaseOutgoingPacket {
	private int reqId, currentTemp, currentConditions, futureTemp, futureConditions, sunrise24Hr, sunriseMin, sunset24Hr, sunsetMin;
	
	public SetWeatherPacket(int currentTemp, int currentConditions, int futureTemp, int futureConditions, int sunrise24Hr, int sunriseMin, int sunset24Hr, int sunsetMin) {
		this.currentTemp = currentTemp;
		this.currentConditions = currentConditions;
		this.futureTemp = futureTemp;
		this.futureConditions = futureConditions;
		this.sunrise24Hr = sunrise24Hr;
		this.sunriseMin = sunriseMin;
		this.sunset24Hr = sunset24Hr;
		this.sunsetMin = sunsetMin;
	}

	@Override
	public void setReqId(int reqId) {
		this.reqId = reqId;
	}

	@Override
	public byte[] toBytes() {
		return new byte[] {
			(byte)reqId,						// request ID
			(byte)OpCode.setWeather,			// op code
			(byte)currentTemp,					// current temp (-128 to 127)
			(byte)currentConditions,			// current weather conditions
			(byte)futureTemp,					// forecast temp (-128 to 127)
			(byte)futureConditions,				// forecast weather conditions
			(byte)sunrise24Hr,					// sunrise hour
			(byte)sunriseMin,					// sunrise minute
			(byte)sunset24Hr,					// sunset hour
			(byte)sunsetMin						// sunset minute
		};
	}

}
