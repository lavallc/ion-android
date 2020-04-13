package io.lava.ion.logger;

import android.util.Log;

public class Logger {
	private static final boolean LOGGING_ENABLED = false;
	
	public static void d(String tag, String msg) {
		if (LOGGING_ENABLED)
			Log.d(tag, msg);
	}
	
	public static void e(String tag, String msg) {
		if (LOGGING_ENABLED)
			Log.e(tag, msg);
	}
	
	public static void e(String tag, String msg, Exception exp) {
		if (LOGGING_ENABLED)
			Log.e(tag, msg, exp);
	}
	
	public static void v(String tag, String msg) {
		if (LOGGING_ENABLED)
			Log.v(tag, msg);
	}
	
	public static void i(String tag, String msg) {
		if (LOGGING_ENABLED)
			Log.i(tag, msg);
	}
	
	public static void w(String tag, String msg) {
		if (LOGGING_ENABLED)
			Log.w(tag, msg);
	}
	
	public static boolean isLoggingEnabled() {
		return LOGGING_ENABLED;
	}
}
