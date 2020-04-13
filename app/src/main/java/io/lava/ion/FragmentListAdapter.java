package io.lava.ion;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class FragmentListAdapter extends ArrayAdapter<String> {
	private final Context context;
	private final ArrayList<String> fragment_titles;
 
	public FragmentListAdapter(Context context, ArrayList<String> fragment_titles) {
		super(context, R.layout.left_drawer_list_item, fragment_titles);
		
		this.context = context;
		this.fragment_titles = fragment_titles;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View vi = convertView;
        if (convertView == null) {
        	LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        	vi = inflater.inflate(R.layout.left_drawer_list_item, parent, false);
        }
	    
        TextView title = (TextView)vi.findViewById(R.id.fragment_title);
        ImageView icon = (ImageView)vi.findViewById(R.id.fragment_icon);
        
        String fragmentTitle = fragment_titles.get(position);
        
        title.setText(fragmentTitle);
        
        if (fragmentTitle.equals(context.getString(R.string.fragment_home_title_header))) {
        	icon.setImageResource(R.drawable.menu_home);
        } else if (fragmentTitle.equals(context.getString(R.string.fragment_patterns_title_header))) {
        	icon.setImageResource(R.drawable.menu_moods);
        } else if (fragmentTitle.equals(context.getString(R.string.fragment_notifications_title_header))) {
        	icon.setImageResource(R.drawable.menu_notifications);
        } else if (fragmentTitle.equals(context.getString(R.string.fragment_settings_title_header))) {
        	icon.setImageResource(R.drawable.menu_settings);
        } else if (fragmentTitle.equals(context.getString(R.string.fragment_recovery_title_header))) {
        	icon.setImageResource(R.drawable.software_update);
        } else if (fragmentTitle.equals(context.getString(R.string.fragment_about_title_header))) {
        	icon.setImageResource(R.drawable.menu_about);
        }

        return vi;
	}
}
