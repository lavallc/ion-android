package io.lava.ion.utility;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;

public class ImageLoaderTask extends AsyncTask<Void, Void, Drawable> {
	private Context context;
	private ImageView iv;
	private int drawableId;
	private Runnable cb;
	
	public ImageLoaderTask(Context context, int drawableId, ImageView iv, Runnable cb) {
		this.context = context;
		this.iv = iv;
		this.drawableId = drawableId;
		this.cb = cb;
		this.execute();
	}
	
	protected Drawable doInBackground(Void... input) {
		Drawable draw = context.getResources().getDrawable(drawableId);
		return draw;
	}
	
	protected void fadeInDrawable(final ImageView imgView, final Drawable img) {
		Animation fadeIn = new AlphaAnimation(0f, 1f);
		fadeIn.setInterpolator(new AccelerateInterpolator());
		fadeIn.setDuration(300);
		imgView.startAnimation(fadeIn);
		imgView.setImageDrawable(img);
	}

	protected void onProgressUpdate(Void... progress) {}

	protected void onPostExecute(Drawable img) {
		// fade in the drawable
		fadeInDrawable(iv, img);
    	 
		// fire callback if it exists
		if (cb != null)
			cb.run();
	}
}