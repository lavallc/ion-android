package io.lava.ion.fragments.settings;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class IconRightPreference extends Preference {
    public IconRightPreference(Context ctxt) {
        super(ctxt);
    }

    public IconRightPreference(Context ctxt, AttributeSet attrs) {
        super(ctxt, attrs);
    }

    public IconRightPreference(Context ctxt, AttributeSet attrs, int defStyle) {
        super(ctxt, attrs, defStyle);
    }
    
    @Override
    protected View onCreateView(ViewGroup parent) {
        ViewGroup parentView = (ViewGroup)super.onCreateView(parent);
        
        // kill any left/right margins on imageview
        ImageView icon = (ImageView)parentView.findViewById(android.R.id.icon);
        LinearLayout.LayoutParams iconLp = (LinearLayout.LayoutParams)icon.getLayoutParams();
        iconLp.setMargins(0, iconLp.topMargin, 0, iconLp.bottomMargin);
        
        // grab all 3 views in preference layout
        LinearLayout firstView = (LinearLayout)parentView.getChildAt(0);
        RelativeLayout middleView = (RelativeLayout)parentView.getChildAt(1);
        LinearLayout lastView = (LinearLayout)parentView.getChildAt(2);
        
        // remove views
        parentView.removeAllViews();
        
        // reorder so imageview is on the right
        parentView.addView(middleView);
        parentView.addView(lastView);
        parentView.addView(firstView);

        return parentView;
    }
}