package com.miciniti.utils;

public class Utils 
{
	public static boolean isValidJSON(String response)
	{
		if(response == null) return false;
		response = response.trim();
		
		if (response.length() != 0 && (response.charAt(0) == '{') && (response.charAt(response.length() - 1) == '}')) 
			return true;
		else
			return false;
	}
	
	
	public void sleeper(long i)
	{
		try
		{
			Thread.sleep(i);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}

}
