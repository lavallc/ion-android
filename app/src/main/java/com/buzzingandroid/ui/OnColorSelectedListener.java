package com.buzzingandroid.ui;

public interface OnColorSelectedListener {
	/**
	 * @param color
	 *            The color code selected, or null if no color. No color is
	 *            only possible if
	 *            {@link HSVColorPickerDialog#setNoColorButton(int)
	 *            setNoColorButton()} has been called on the dialog before
	 *            showing it
	 */
	public void colorSelected(Integer color);
}
