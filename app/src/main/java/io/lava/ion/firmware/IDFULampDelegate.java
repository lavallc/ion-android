package io.lava.ion.firmware;

public interface IDFULampDelegate {
	public void DFUTargetDidConnect();
	public void DFUTargetDidDisconnect();
	public void DFUTargetDidSendDataWithProgress(float progress);
	public void DFUTargetDidFailFlash();
	public void DFUTargetDidFinishFlash();
	public void DFUTargetNotFound();
}