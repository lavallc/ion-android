package io.lava.ion.fragments.home;

import io.lava.ion.connectivity.ILampManagerListener;
import io.lava.ion.connectivity.LampManager;
import io.lava.ion.connectivity.lamp.Lamp;
import io.lava.ion.moods.MoodManager;

import java.util.ArrayList;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;


public class RotationListManager implements ILampManagerListener {
	private ArrayList<Integer> currentMoods;
	private ListView rotationListView;
	private RotationAdapter rotationAdapter;
	private ViewGroup lampControls, noConnectionLayout;
	private ImageView rotationShadow;
	private Activity activity;
	
	private Lamp selectedLamp;
	
	public RotationListManager(final Activity activity, ListView rotationListView, ViewGroup lampControls, ImageView rotationShadow, ViewGroup noConnectionLayout) {
		this.activity = activity;
		this.currentMoods = new ArrayList<Integer>();
		this.rotationListView = rotationListView;
		this.lampControls = lampControls;
		this.rotationAdapter = new RotationAdapter(activity, this.currentMoods);
		this.rotationShadow = rotationShadow;
		this.noConnectionLayout = noConnectionLayout;
		
		rotationListView.setAdapter(rotationAdapter);
		rotationListView.setOverScrollMode(View.OVER_SCROLL_NEVER);
		 
        // Click event for single list row
		rotationListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            	// set the lamp's mood
            	if (selectedLamp != null) {
            		selectedLamp.setMood(selectedLamp.getRotation().get(position), null);
            	} else if (LampManager.getInstanceIfReady().areMultipleLampsSelected()) {
            		// multiple lamps selected, the user chose from the entire rotation list
            		for (Lamp l: LampManager.getInstanceIfReady().getReadyLamps()) {
						l.setMood(MoodManager.getInstance(activity).getAllMoodIds().get(position+1), null);
					}
            	}
            }
        });
		
		LampManager lampMgr = LampManager.getInstanceIfReady();
		lampMgr.addListener(this);
		
		if (lampMgr.areMultipleLampsSelected()) {
			// display all moods in listview
			displayAllMoods();
		} else if (lampMgr.areNoLampsSelected()) {
			// hide the listview
			hideRotation();
		} else {
			selectedLamp = lampMgr.getSelectedLampIfExists();
			
			displayRotation(selectedLamp.getRotation());
		}
	}
	
	
	
	private void displayAllMoods() {
		// load all moods into ListView
		currentMoods.clear();
		currentMoods.addAll(MoodManager.getInstance(activity).getAllMoodIds());
		
		// remove OFF since we have a control for that already
		currentMoods.remove(0);
		
		// update listview
		rotationAdapter.notifyDataSetChanged();
		
		showRotation();
	}
	
	private void displayRotation(ArrayList<Integer> moodIds) {
		// load rotation into ListView
		currentMoods.clear();
		currentMoods.addAll(moodIds);
		
		// update listview
		rotationAdapter.notifyDataSetChanged();
		
		showRotation();
	}
	
	private void showRotation() {
		noConnectionLayout.setVisibility(View.GONE);
		rotationListView.setVisibility(View.VISIBLE);
		lampControls.setVisibility(View.VISIBLE);
		rotationShadow.setVisibility(View.VISIBLE);
	}
	
	private void hideRotation() {
		rotationListView.setVisibility(View.GONE);
		lampControls.setVisibility(View.GONE);
		rotationShadow.setVisibility(View.GONE);
		noConnectionLayout.setVisibility(View.VISIBLE);
	}
	
	
	

	@Override
	public void singleLampSelected(Lamp lamp) {
		if (lamp != selectedLamp) {
			selectedLamp = lamp;
			
			displayRotation(selectedLamp.getRotation());
		}
	}

	@Override
	public void multipleLampsSelected(ArrayList<Lamp> lamps) {
		if (selectedLamp != null) {
			selectedLamp = null;
		}
		
		displayAllMoods();
	}

	@Override
	public void noLampsSelected() {
		if (selectedLamp != null) {
			selectedLamp = null;
		}
		
		hideRotation();
	}
	
	public void onDestroy() {
		if (selectedLamp != null) {
			selectedLamp = null;
		}
		
		LampManager.getInstanceIfReady().removeListener(this);
	}



	@Override
	public void onLampListUpdate() {

	}

	@Override
	public void onMoodChange(int moodId) {
		// update listview
		if (LampManager.getInstanceIfReady().isSingleLampSelected())
			rotationAdapter.notifyDataSetChanged();
	}

	@Override
	public void onWeatherLocationUpdate() {

	}

	@Override
	public void onDfuLampListUpdate() {

	}

}