package io.lava.ion.fragments.home;

import io.lava.ion.R;
import io.lava.ion.connectivity.LampManager;
import io.lava.ion.connectivity.lamp.Lamp;
import io.lava.ion.moods.MoodFromJSON;
import io.lava.ion.moods.MoodManager;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;


public class RotationAdapter extends ArrayAdapter<Integer> {
	private final Context context;
	private final ArrayList<Integer> moods;
 
	public RotationAdapter(Context context, ArrayList<Integer> moods) {
		super(context, R.layout.rotation_list_item, moods);
		
		this.context = context;
		this.moods = moods;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View vi = convertView;
        if (convertView == null) {
        	LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        	vi = inflater.inflate(R.layout.rotation_list_item, parent, false);
        }
	    
        TextView title = (TextView)vi.findViewById(R.id.moodTitle);
        ImageView moodImg = (ImageView)vi.findViewById(R.id.moodImg);
        
        MoodFromJSON moodJson = MoodManager.getInstance(context).getMoodById(moods.get(position));
        
        // darken moods that aren't active when a single lamp is connected
        if (LampManager.getInstanceIfReady() != null) {
        	Lamp selectedLamp = LampManager.getInstanceIfReady().getSelectedLampIfExists();
        	
        	if (selectedLamp != null) {
        		// single lamp
        		if (selectedLamp.getCurrentMood() == moodJson.getId()) {
        			// lighten it up!
        			moodImg.setColorFilter(Color.argb(0, 0, 0, 0));
        		} else {
        			// darken it down!
        			moodImg.setColorFilter(Color.argb(150, 0, 0, 0));
        		}
        	} else {
        		// no darkening
        		moodImg.setColorFilter(Color.argb(0, 0, 0, 0));
        	}
        } else {
        	// no darkening
        	moodImg.setColorFilter(Color.argb(0, 0, 0, 0));
        }
 
        // Setting all values in listview
        title.setText(moodJson.getName());
        int imageID = context.getResources().getIdentifier("io.lava.ion:drawable/" + moodJson.getImageName().replace(".jpg", "") + "_wide", null, null);
        moodImg.setImageResource(imageID);

        return vi;
	}
}