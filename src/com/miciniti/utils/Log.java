package com.miciniti.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Log
{
	public static String getStamp()
	{
		SimpleDateFormat sdf = new SimpleDateFormat("[dd/MM/yy hh:mm:ss aa] ");	// 	
		String text = sdf.format(new Date());    	    	
		return text;
	}

	public static void i(String tag, String message)
	{
		System.err.println(getStamp() + "I [" + tag + "] " + message);
	}

	public static void d(String tag, String message)
	{
		System.err.println(getStamp() + "D [" + tag + "] " + message);
	}

	public static void e(String tag, String message)
	{
		System.err.println(getStamp() + "E [" + tag + "] " + message);
	}

	public static void v(String tag, String message)
	{
		System.err.println(getStamp() + "V [" + tag + "] " + message);
	}
}
