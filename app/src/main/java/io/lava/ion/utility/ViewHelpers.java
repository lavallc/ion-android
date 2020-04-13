package io.lava.ion.utility;

import android.content.Context;

public class ViewHelpers {
	public static float getDensityScalar(Context context) {
		return context.getResources().getDisplayMetrics().density;
	}
	
	public static int pxToDp(Context context, int px) {
	    return Math.round(px / getDensityScalar(context));
	}
	
	public static int dpToPx(Context context, int dp) {
	    return Math.round(dp * getDensityScalar(context));
	}
}
