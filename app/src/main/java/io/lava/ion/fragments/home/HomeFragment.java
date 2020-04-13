package io.lava.ion.fragments.home;

import io.lava.ion.R;
import io.lava.ion.connectivity.LampManager;
import io.lava.ion.connectivity.lamp.Lamp;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;

public class HomeFragment extends Fragment {
	private ListView rotationList;
	private RotationListManager rotationManager;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		
		View v = inflater.inflate(R.layout.fragment_home, container, false);
		
		ImageView nextMoodBtn = (ImageView)v.findViewById(R.id.nextMoodImg);
		ImageView pwrBtn = (ImageView)v.findViewById(R.id.pwrImg);
		
		nextMoodBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				for (Lamp l : LampManager.getInstanceIfReady().getReadyLamps()) {
					l.nextMood();
				}
			}
		});
		
		pwrBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				for (Lamp l : LampManager.getInstanceIfReady().getReadyLamps()) {
					l.togglePower();
				}
			}
		});
		
		rotationList = (ListView)v.findViewById(R.id.rotationList);
		
		ViewGroup lampControls = (ViewGroup)v.findViewById(R.id.lampControls);
		ImageView rotationListShadow = (ImageView)v.findViewById(R.id.rotationListShadow);
		ViewGroup noConnectionLayout = (ViewGroup)v.findViewById(R.id.noConnectionLayout);
		
		rotationManager = new RotationListManager(getActivity(), rotationList, lampControls, rotationListShadow, noConnectionLayout);

		return v;
	}
	
	@Override
	public void onDestroyView() {
		rotationManager.onDestroy();
		
		super.onDestroyView();
	}
}