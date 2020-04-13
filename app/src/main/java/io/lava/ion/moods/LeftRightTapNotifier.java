package io.lava.ion.moods;

import java.util.ArrayList;

public class LeftRightTapNotifier {
	// anyone interested in left/right arrow taps
	private ArrayList<OnLeftRightTapListener> listeners;
	
	public LeftRightTapNotifier() {
		listeners = new ArrayList<OnLeftRightTapListener>();
	}
	
	public void addListener(OnLeftRightTapListener listener) {
		if (listener != null)
			listeners.add(listener);
	}
	
	public void removeListener(OnLeftRightTapListener removeListener) {
		for (OnLeftRightTapListener listener : listeners) {
			if (removeListener == listener) {
				listeners.remove(removeListener);
				return;
			}
		}
	}
	
	public void onLeftTap() {
		for (OnLeftRightTapListener listener : listeners) {
			// let all our listeners that a tap occurred
			try {
				listener.onLeftTap();
			} catch (Exception e) {}
		}
	}
	
	public void onRightTap() {
		for (OnLeftRightTapListener listener : listeners) {
			// let all our listeners that a tap occurred
			try {
				listener.onRightTap();
			} catch (Exception e) {}
		}
	}
}
