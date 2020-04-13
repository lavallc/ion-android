package io.lava.ion.fragments.home.lamplist;

public class HeaderItem implements LampListItem {
	private String headerText;
	
	public HeaderItem(String headerText) {
		this.headerText = headerText;
	}
	
	public String getText() {
		return headerText;
	}
}
