package io.lava.ion.utility;

public class Now {
	public static long get() {
		return System.currentTimeMillis() / 1000L;
	}
	
	public static boolean withinSeconds(long unixTime, int numSeconds) {
		return (get() - unixTime) <= numSeconds;
	}
}
