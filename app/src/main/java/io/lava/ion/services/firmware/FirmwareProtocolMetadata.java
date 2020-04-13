package io.lava.ion.services.firmware;

public class FirmwareProtocolMetadata {
	int protocolVer, firmwareVer;
	String sha256, url;
	
	public FirmwareProtocolMetadata(int protocolVer, int firmwareVer, String sha256, String url) {
		this.protocolVer = protocolVer;
		this.firmwareVer = firmwareVer;
		this.sha256 = sha256;
		this.url = url;
	}
	
	public int getProtocolVersion() {
		return protocolVer;
	}
	
	public int getFirmwareVersion() {
		return firmwareVer;
	}
	
	public String getSha256() {
		return sha256;
	}
	
	public String getDownloadUrl() {
		return url;
	}
}
