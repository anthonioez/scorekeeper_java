package com.miciniti.scorekeeper;

import com.miciniti.json.JSONException;
import com.miciniti.json.JSONObject;

public class ScoreItem
{
	private long	id;
	private String	player;
	private int		score;
	private int		remaining;
	private double	mod;
	private int		state;
	private long	stamp;
	
	public long getId()
	{
		return id;
	}
	
	public String getPlayer()
	{
		return player;
	}
	
	public int getScore()
	{
		return score;
	}

	public int getRemaining()
	{
		return remaining;
	}
	
	public double getMod()
	{
		return mod;
	}
	
	public int getState()
	{
		return state;
	}
	
	public long getStamp()
	{
		return stamp;
	}
	
	public void setId(long id)
	{
		this.id = id;
	}

	public void setPlayer(String player)
	{
		this.player = player;
	}

	public void setScore(int score)
	{
		this.score = score;
	}

	public void setRemaining(int rem)
	{
		this.remaining = rem;
	}

	public void setMod(double mod)
	{
		this.mod = mod;
	}

	public void setState(int s)
	{
		this.state = s;
	}

	public void setStamp(long stamp)
	{
		this.stamp = stamp;
	}

	public JSONObject getJSON() throws JSONException
	{
		JSONObject json = new JSONObject();
		json.put("id", 		id);
		json.put("name", 	player);
		json.put("score", 	score);
		json.put("rem", 	remaining);
		json.put("mod", 	mod);
		json.put("state", 	state);
		json.put("stamp", 	stamp);
		
		return json;
	}

	public void reset(int turns)
	{
		score 		= 0;
		remaining 	= turns;
		mod 		= 0;		
		state 		= 0;
	}
}
