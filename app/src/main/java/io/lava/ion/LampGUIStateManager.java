package io.lava.ion;

import io.lava.ion.connectivity.ILampManagerListener;
import io.lava.ion.connectivity.LampManager;
import io.lava.ion.connectivity.lamp.Lamp;
import io.lava.ion.fragments.home.lamplist.HeaderItem;
import io.lava.ion.fragments.home.lamplist.LampListAdapter;
import io.lava.ion.fragments.home.lamplist.LampListItem;

import java.util.ArrayList;

import android.content.Context;
import android.widget.ListView;

public class LampGUIStateManager implements ILampManagerListener {
	private ListView lampList;
	private ArrayList<LampListItem> rightDrawerList;
	private LampManager lampManager;
	private static LampGUIStateManager instance;
	private HeaderItem nearbyHeader, savedHeader;
	
	public LampGUIStateManager(Context context, ListView lampList, LampManager lampManager) {
		this.lampList = lampList;
		this.lampManager = lampManager;
		
		nearbyHeader = new HeaderItem("NEARBY");
		savedHeader = new HeaderItem("OFFLINE");
		
		rightDrawerList = new ArrayList<LampListItem>();
		
		lampList.setAdapter(new LampListAdapter(context, rightDrawerList));
		lampList.setDividerHeight(0);
		
		// register for updates (didUpdateLampList)
		lampManager.addListener(this);
		
		// initial state check
		onLampListUpdate();
		
		instance = this;
	}
	
	private void rebuildLampList() {
		//Logger.i(this.getClass().getSimpleName(), "REBUILDING LAMP LIST");
		
		rightDrawerList.clear();
		
		ArrayList<Lamp> seenRecently = new ArrayList<Lamp>();
		ArrayList<Lamp> saved = new ArrayList<Lamp>();
		
		for (Lamp l : lampManager.getAllLamps()) {
			if (l.seenRecently()) {
				seenRecently.add(l);
			} else if (l.isRemembered()) {
				saved.add(l);
			}
		}
		
		// always display the nearby header if no lamps are found
		if (seenRecently.size() == 0 && saved.size() == 0) {
			rightDrawerList.add(nearbyHeader);
		}
		
		// display nearby lamps
		if (seenRecently.size() > 0) {
			rightDrawerList.add(nearbyHeader);
			rightDrawerList.addAll(seenRecently);
		}
		
		// display saved lamps
		if (saved.size() > 0) {
			rightDrawerList.add(savedHeader);
			rightDrawerList.addAll(saved);
		}
		
		// updates list in the right drawer
		((LampListAdapter)lampList.getAdapter()).notifyDataSetChanged();
	}
	
	public static LampGUIStateManager getStateManagerIfExists() {
		return instance;
	}
	
	public void onDestroy() {
		// remove ourself as a listener
		lampManager.removeListener(this);
	}

	@Override
	public void onLampListUpdate() {
		// updates list of lamps in right drawer
		rebuildLampList();
	}

	@Override
	public void singleLampSelected(Lamp lamp) {

	}

	@Override
	public void multipleLampsSelected(ArrayList<Lamp> lamps) {

	}

	@Override
	public void noLampsSelected() {

	}

	@Override
	public void onMoodChange(int moodId) {

	}

	@Override
	public void onWeatherLocationUpdate() {

	}

	@Override
	public void onDfuLampListUpdate() {

	}
}
