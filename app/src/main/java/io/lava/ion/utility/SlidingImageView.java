package io.lava.ion.utility;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class SlidingImageView extends ImageView {
	private float hOffset = 0.0f;
	private float vOffset = 0.0f;
	
	public SlidingImageView(Context context) {
		super(context);
		
		configMatrix();
	}

	public SlidingImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		configMatrix();
	}

	public SlidingImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		configMatrix();
	}
	
	// we'll be manually controlling the imageview's matrix
	private void configMatrix() {
		this.setScaleType(ScaleType.MATRIX);
	}
	
	public void setHorizontalOffset(float offset) {
		// save for later
		hOffset = offset;
		
		// force redraw
		this.invalidate();
	}
	
	@Override
	protected void onDraw (Canvas canvas) {
		// calculate and set offsets
		updateOffsetAndScale();
		
		// allow regular draw to happen
		super.onDraw(canvas);
	}
	
	private void updateOffsetAndScale() {
		Drawable imgDrawable = this.getDrawable();
		
		// only try to scale/offset once the image has been set
		if (imgDrawable != null) {
			// get imageview size
			int imageWidth = this.getWidth();
			int imageHeight = this.getHeight();
			
			// get actual image drawable size
			int drawableWidth = imgDrawable.getIntrinsicWidth();
			int drawableHeight = imgDrawable.getIntrinsicHeight();
	        
			// for calculations
	        float scale;
	        float dx = 0, dy = 0;
	
	        // check if we need to scale in the X or Y direction (both dimensions must fill the container)
	        if (drawableWidth * (float)imageHeight > (float)imageWidth * drawableHeight) {
	        	scale = (float)imageHeight / drawableHeight;
	        } else {
	        	scale = (float)imageWidth / drawableWidth;	
	        }
	        
	        // calculate the center point of the image (in pixels) based off
	        // the scaling amount, container size, and image size
	        dx = ((float)imageWidth - drawableWidth * scale) * 0.5f;
	        dy = ((float)imageHeight - drawableHeight * scale) * 0.5f;
	        
	        // calculate offsets
	        float imgOffsetPxWidth = (int)Math.floor(((float)imageWidth) * hOffset);
	        float imgOffsetPxHeight = (int)Math.floor(((float)imageHeight) * vOffset);
	        
	        // create and manipulate image matrix
	        Matrix layerImageMatrix = new Matrix();
	        layerImageMatrix.setScale(scale, scale);
	        layerImageMatrix.postTranslate((int) (dx + 0.5f + imgOffsetPxWidth), (int) (dy + 0.5f + imgOffsetPxHeight));
	        
	        // update imageview matrix
	        this.setImageMatrix(layerImageMatrix);
		}
	}
}
