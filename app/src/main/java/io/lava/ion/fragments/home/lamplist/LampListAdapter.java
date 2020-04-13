package io.lava.ion.fragments.home.lamplist;

import io.lava.ion.R;
import io.lava.ion.connectivity.lamp.Lamp;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class LampListAdapter extends ArrayAdapter<LampListItem> {
	private final Context context;
	private final ArrayList<LampListItem> lampList;
	
	public final static int TYPE_HEADER = 0;
	public final static int TYPE_LAMP = 1;
 
	public LampListAdapter(Context context, ArrayList<LampListItem> lampList) {
		super(context, R.layout.lamp_row, lampList);
		this.context = context;
		this.lampList = lampList;
	}
	
	@Override
	public int getItemViewType(int position) {
		if (lampList.get(position) instanceof HeaderItem) {
			return TYPE_HEADER;
		} else {
			return TYPE_LAMP;
		}
	}
	
	@Override
	public int getViewTypeCount() {
		return 2;
	}
	
	private View buildHeaderRow(int position, View rowView) {
		HeaderItem headerItem = (HeaderItem)lampList.get(position);
		
		TextView lampHeaderTextView = (TextView)rowView.findViewById(R.id.lampHeaderTextView);
		lampHeaderTextView.setText(headerItem.getText());
		
		return rowView;
	}
	
	private View buildLampRow(int position, View rowView) {
		Lamp lamp = (Lamp)lampList.get(position);
		
		View connectionColor = (View)rowView.findViewById(R.id.connectionColor);
		TextView lampNameTextView = (TextView)rowView.findViewById(R.id.lampName);
		TextView lampHintTextView = (TextView)rowView.findViewById(R.id.lampHint);
		ImageView signalIndicator = (ImageView)rowView.findViewById(R.id.lampSignal);
		View lampDivider = (View)rowView.findViewById(R.id.lampDivider);
		
		if (lamp.getConnectivityState() == Lamp.LampState.READY) {
			connectionColor.setBackgroundColor(Color.GREEN);
			lampHintTextView.setText("Connected");
		} else if (lamp.getConnectivityState() != Lamp.LampState.DISCONNECTED) {
			connectionColor.setBackgroundColor(Color.YELLOW);
			lampHintTextView.setText("Connecting...");
		} else if (lamp.getConnectivityState() == Lamp.LampState.DISCONNECTED && lamp.seenRecently()) {
			connectionColor.setBackgroundColor(Color.RED);
			lampHintTextView.setText("Tap to connect");
		} else {
            connectionColor.setBackgroundColor(Color.RED);
            lampHintTextView.setText("Out of range");
        }
		
		double lampRssi = lamp.getRssi();
		
		if (!lamp.seenRecently()) {
			signalIndicator.setImageResource(R.drawable.signal_none);
		} else if (lampRssi > -70) {
			signalIndicator.setImageResource(R.drawable.signal_high);
		} else if (lampRssi > -85) {
			signalIndicator.setImageResource(R.drawable.signal_medium);
		} else {
			signalIndicator.setImageResource(R.drawable.signal_low);
		}
		
		lampNameTextView.setText(lamp.getName());
		
		// show the divider if a header isn't below us
		if ((position+1) < lampList.size()) {
			if (getItemViewType(position+1) == TYPE_LAMP) {
				lampDivider.setVisibility(View.VISIBLE);
			} else {
				lampDivider.setVisibility(View.GONE);
			}
		} else {
			lampDivider.setVisibility(View.GONE);
		}
		
		return rowView;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		int type = getItemViewType(position);
		
		View rowView = convertView;
        if (convertView == null) {
        	LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        	
        	if (type == TYPE_HEADER) {
        		// header
        		rowView = inflater.inflate(R.layout.lamp_header_row, parent, false);
        	} else {
        		// lamp
        		rowView = inflater.inflate(R.layout.lamp_row, parent, false);
        	}
        }
	    
        if (type == TYPE_HEADER) {
        	return buildHeaderRow(position, rowView);
        } else {
        	return buildLampRow(position, rowView);
        }
	}
}