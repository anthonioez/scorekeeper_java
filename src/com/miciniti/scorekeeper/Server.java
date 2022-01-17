package com.miciniti.scorekeeper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Enumeration;
import java.util.Vector;

import com.miciniti.json.JSONArray;
import com.miciniti.json.JSONException;
import com.miciniti.json.JSONObject;
import com.miciniti.json.JSONTokener;
import com.miciniti.utils.Log;
import com.miciniti.utils.Utils;

public class Server implements Runnable
{	
	public static final String TAG				= "Server";
	public static final String ERROR_JSON		= "Internal json error";

	public static int				MAX					= 100;
	public static int				PORT				= 8124;
	public static int				TURNS				= 20;
	public static int				DIVISOR				= 2;
	public static String			PASS				= "pass1word2";
	
	public static final String		CMD_AUTH			= "auth";
	public static final String 		CMD_PASS 			= "pass";	
	public static final String 		CMD_QUIT 			= "quit";
	public static final String 		CMD_WIPE 			= "wipe";
	public static final String 		CMD_LIST 			= "list";
	public static final String 		CMD_PLAY 			= "play";
	public static final String 		CMD_LITE 			= "lite";
	public static final String 		CMD_OPTS 			= "opts";
	public static final String 		CMD_TEXT 			= "text";
	public static final String 		CMD_ERRO 			= "erro";

	private static Vector<Worker> 		workers;
	private static Vector<ScoreItem>	players;
	
	private static int				port			= PORT;
	private static int				max				= MAX;
	private static int				turns			= TURNS;
	private static int				divisor			= DIVISOR;
	private static String			password		= PASS;
	
	private static ServerSocket	 	serverSocket 	= null;


	//java -jar port max
	public static void main(String[] args)
	{
		if(args.length >= 1 && args[0] != null) 		
		{
			port = Integer.parseInt(args[0]);
		}
		
		if(args.length >= 2 && args[1] != null) 		
		{
			max = Integer.parseInt(args[1]);
		}
	
		new Server().run();
	}
	
	public void run()
	{
		workers = new Vector<Worker>();
		players = new Vector<ScoreItem>();
		
		serverSocket = null;
		try
		{
			Log.i(TAG, "Server starting on port: " + port + " for " + max + " conections");
			Log.i("", "");
			
			serverSocket = new ServerSocket(port);

			Log.i(TAG, "Server started...");
			for (;;)
			{
				Socket clientSocket = null;
				try
				{
					Log.i(TAG, "Waiting for connection..." + (workers.size() + "/" + max));
					
					// blocking call - waits for a connection
					clientSocket = serverSocket.accept(); 
					
					Log.i(TAG, "Connected to client: " + clientSocket.getRemoteSocketAddress() + " host: " + clientSocket.getInetAddress().getHostName() + " ip: " + clientSocket.getInetAddress().getHostAddress());
				}
				catch (IOException e)
				{
					Log.i(TAG, "IOException: " + e);
					break;
				}
	
				Worker worker = new Worker(clientSocket);
				
				if(addWorker(worker))
				{				
					// spin off a new thread to handle this socket - this way new socket connections can be served immediately
					worker.start();				
				}
			}
		}
		catch (IOException e)
		{
			Log.i(TAG, e + "- port:" + port);
		}
		
		System.exit(-1);
	}

	private synchronized static boolean addWorker(Worker worker) 
	{
		if(workers == null) return false;
		
		if(workers.size() >= max)
		{
			return false;
		}
		
		workers.addElement(worker);		
		return true;
	}
	
	private synchronized static boolean removeWorker(Worker worker) 
	{
		if(workers == null) return false;
		
		return workers.remove(worker);		
	}

	public synchronized void removePlayWorker(long id)
	{
		Log.i(TAG, "Looking for worker to remove!");
		Enumeration<Worker> enu = workers.elements(); 
		while(enu.hasMoreElements())
		{
			Worker item = enu.nextElement();
			if(item == null) continue;
						
			if(!item.isKeeper() && item.playerId == id)
			{
				Log.i(TAG, "found: " + id);
				item.interrupt();
				item.quit();
				Server.removeWorker(item);
				break;
			}
		}
	}
	
	public synchronized void updateWorkers(Worker worker, String msg)
	{
		Log.i(TAG, "Updating workers!");
		Enumeration<Worker> enu = workers.elements(); 
		while(enu.hasMoreElements())
		{
			Worker item = enu.nextElement();
			if(item == null || item == worker) continue;
			
			if(item.isKeeper())
			{
				item.sendList(msg);
			}
		}
	}
	
	public synchronized void updatePlayWorkers(Worker worker, String msg)
	{
		Log.i(TAG, "Updating play workers!");
		Enumeration<Worker> enu = workers.elements(); 
		while(enu.hasMoreElements())
		{
			Worker item = enu.nextElement();
			if(item == null || item == worker) continue;
			
			if(!item.isKeeper())
			{
				ScoreItem score = playerById(item.playerId);
				if(score != null)
				{
					item.sendScore(score.getScore());
				}
			}
		}
	}
	
	public void test()
	{
	}

	public void resetGame()
	{		
		Enumeration<ScoreItem> enu = players.elements(); 
		while(enu.hasMoreElements())
		{
			ScoreItem item = enu.nextElement();
			if(item == null) continue;
			
			int index = players.indexOf(item);
			if(index >= 0)
			{
				item.reset(turns);				
				players.set(index, item);
			}
		}
	}

	public ScoreItem updateState(long id, int state)
	{
		ScoreItem score = null;
		
		Enumeration<ScoreItem> enu = players.elements(); 
		while(enu.hasMoreElements())
		{
			ScoreItem item = enu.nextElement();
			if(item == null) continue;
			
			if(item.getId() == id)
			{
				int index = players.indexOf(item);
				if(index >= 0)
				{
					item.setState(state);					
					players.set(index, item);
					score = item;
				}
			}
		}
		return score;
	}
	
	public ScoreItem updateScore(long id, int delta)
	{
		ScoreItem score = null;
		
		Enumeration<ScoreItem> enu = players.elements(); 
		while(enu.hasMoreElements())
		{
			ScoreItem item = enu.nextElement();
			if(item == null) continue;
			
			if(item.getId() == id)
			{
				int index = players.indexOf(item);
				if(index >= 0)
				{
					if(delta == 0)
					{
						item.setScore(0);
//						item.setRemaining(turns);
					}
					else
					{
						if(item.getRemaining() > 0)
						{
							item.setScore(item.getScore() + delta);
							item.setRemaining(item.getRemaining() - 1);
						}
					}
						
					double mod = 0;
					if(item.getRemaining() > 0)
						mod = ((double)item.getScore() * (double)divisor)  / (double)item.getRemaining();
					else
						mod = item.getScore();
					
					item.setMod(mod);
					item.setStamp(System.currentTimeMillis());
					
					players.set(index, item);
					score = item;
				}
			}
		}
		return score;
	}

	public boolean removeScore(long id)
	{		
		Enumeration<ScoreItem> enu = players.elements(); 
		while(enu.hasMoreElements())
		{
			ScoreItem item = enu.nextElement();
			if(item == null) continue;
			
			if(item.getId() == id)
			{
				players.remove(item);
				return true;
			}
		}
		return false;
	}

	public ScoreItem playerByName(String name)
	{		
		Enumeration<ScoreItem> enu = players.elements(); 
		while(enu.hasMoreElements())
		{
			ScoreItem item = enu.nextElement();
			if(item == null) continue;
			
			if(name.equalsIgnoreCase(item.getPlayer()))
			{				
				return item;
			}
		}
		return null;
	}

	public ScoreItem playerById(long id)
	{		
		Enumeration<ScoreItem> enu = players.elements(); 
		while(enu.hasMoreElements())
		{
			ScoreItem item = enu.nextElement();
			if(item == null) continue;
			
			if(id == item.getId())
			{				
				return item;
			}
		}
		return null;
	}

	/**
	 * <p>
	 * Worker is just a simple thread that handles each inbound socket connection.
	 * so that further socket connections can be accepted while another socket is
	 * handled!
	 */
	public class Worker extends Thread
	{
		private String TAG 			= "Worker";
	
		private static final int TIMEOUT = 600000;
	
		private Socket			_socket	= null;
		private InputStream 	_in 	= null;
		private OutputStream	_out 	= null;
		private byte[] 			_buffer = null;
	
		private long	playerId	= -1;
		private boolean keeper 		= false;
	
		private boolean running = true;
		
		public Worker(Socket soc)
		{
			_socket = soc;
			TAG += String.valueOf(soc.getPort());
		}
	
		public boolean isKeeper()
		{
			return keeper ;
		}

		public String getIp()
		{
			if(_socket != null)
			{
				InetAddress inet = _socket.getInetAddress();
				if(inet != null)
					return inet.getHostAddress();
			}
			return "Unknown Ip";
		}
		
		public String getHost()
		{
			if(_socket != null)
			{
				InetAddress inet = _socket.getInetAddress();
				if(inet != null)
					return inet.getHostName();
			}
			return "Unknown Host";
		}
		
		public String getAgent()
		{
			return "Unknown Useragent";
		}
		
		public void run()
		{
			try
			{
				running = true;
				_socket.setSoTimeout(TIMEOUT);
				_in		= _socket.getInputStream();
				_out 	= _socket.getOutputStream();
	
				int read = 0;
				while(read != -1)
				{
					read = read();
					if(read > 0)
					{
						String str = new String(_buffer); 
						
						Log.i(TAG, "Read from socket " + _socket.getPort() + " [" + str + "]...");
	
						processCommand(_buffer);
						
						if(!running) break;
					}
				}
				running = false;
				Log.i(TAG, "Socket " + _socket.getPort() + " closed");
			}
			catch (IOException e)
			{
				Log.i(TAG, "IOException: " + e);
			}
			finally
			{
				quit();
			}
			Server.removeWorker(this);
		}

		public void quit()
		{
			try { if(_in != null) _in.close(); _in = null;} 
			catch (IOException e) {}
			
			try { if(_out != null) _out.close(); _out = null;} 
			catch (IOException e) {}
			
			try { if(_socket != null) _socket.close(); _socket = null;} 
			catch (IOException e) {}			
		}
		
		private int read() 
		{
	        ByteArrayOutputStream bStrm = new ByteArrayOutputStream();
			Log.i(TAG, "Reading from socket " + _socket.getPort() + "...");
	
			int bytes = 2048;
			
			int ch = 0;
			int read = 0;
			int count = 0;
			while (count < bytes) 
			{
				try 
				{
					ch = _in.read();
					if(ch == -1)
					{
						read = -1;
						break;
					}
					else if(ch == 0)
					{
						break;
					}
					bStrm.write(ch);
					count++;
					read++;
//					Log.i(TAG, "Read: " + new String(bStrm.toString()));
				} 
				catch (SocketTimeoutException e) 
				{
					Log.i(TAG, "read SocketTimeoutException on Socket: " + _socket.getPort() + " e: " + e);
					break;
				}
				catch (SocketException e) 
				{
					Log.i(TAG, "read SocketException on Socket: " + _socket.getPort() + " e: " + e);
					read = -1;
					break;
				}
				catch (IOException e) 
				{
					Log.i(TAG, "read IOException on Socket: " + _socket.getPort() + " e: " + e);
					read = -1;
					break;
				}
			}
			
			_buffer = bStrm.toByteArray();
			return read;
		}
		
		public synchronized int write(String data) throws IOException
		{
			Log.i(TAG, "Writing [" + data + "]");
			
			byte[] buffer = data.getBytes();

			int wrote = 0;
	    	int length = buffer.length;    	
	    	
	        _out.write(buffer, 0, length);
	    	wrote += length;
	    	
	        _out.write(0);
	    	wrote++;

	    	_out.flush();
			
	    	return wrote;
		}

		private void processCommand(byte[] buffer)
		{
			String response = new String(buffer);
			
			if(!Utils.isValidJSON(response)) return;
			
			try
			{
				JSONObject jo = new JSONObject(new JSONTokener(response));
				if ((jo != null) && jo.has("cmd")) 
				{
					String cmd = jo.optString("cmd");
	
					Log.i(TAG, "Command: " + cmd);
					
					if(cmd == null)
					{
						
					}
					else if(cmd.equalsIgnoreCase(Server.CMD_LIST))					
					{
						keeper 	= true;
						sendList("Scores updated!");
				        return;
					}
					else if(cmd.equalsIgnoreCase(Server.CMD_AUTH))	//params:	id, name	 
					{
						processAuth(jo);
				        return;
					}
					else if(cmd.equalsIgnoreCase(Server.CMD_PASS))	//params:	id, pass
					{
						processPass(jo);
				        return;
					}
					else if(cmd.equalsIgnoreCase(Server.CMD_QUIT))	//params:	id	 
					{
						processQuit(jo);
						running = false;
				        return;
					}
					else if(cmd.equalsIgnoreCase(Server.CMD_WIPE))	//params:	id	 
					{
						processWipe(jo);
				        return;
					}
					else if(cmd.equalsIgnoreCase(Server.CMD_PLAY))	//params:	id, delta	 
					{
						processPlay(jo);
				        return;
					}
					else if(cmd.equalsIgnoreCase(Server.CMD_LITE))	//params:	id, state	 
					{
						processHilite(jo);
				        return;
					}
					else if(cmd.equalsIgnoreCase(Server.CMD_OPTS))	//params:	id, delta	 
					{
						processOpts(jo);
				        return;
					}
					else
					{
						return;
					}
				}
			}
			catch (JSONException e)
			{
				Log.i(TAG, "processCommand JSONException: " + e);								
			} 
			catch (Exception e) 
			{
				Log.i(TAG, "processCommand IOException: " + e);								
			}		
		}
		
		public void processPass(JSONObject jo)
		{
			if(jo.has("data"))
			{
				try
				{
					String 	pass = jo.getString("data");
					if(pass != null)
					{
						if(pass.equals(password))
						{
							sendPass();
							return;
						}
						else
						{
							sendError("Invalid password!");						
							return;
						}
					}
				}
				catch(Exception e)
				{
					Log.i(TAG, "processPass Exception: " + e.toString());					
				}
				sendError("Unable to authenticate user!");
			}
		}	
		
		public void processAuth(JSONObject jo)
		{
			if(jo.has("data"))
			{
				try
				{
					String 	name = jo.getString("data");
					if(name != null)
					{
						long id = 0;
						ScoreItem item = null;
						if(jo.has("id") && (id = jo.getLong("id")) > 0)
						{
							item = playerById(id);															

							if(item != null)
							{
								int index = players.indexOf(item);								
								item.setPlayer(name);

								players.set(index, item);
								
								sendAuth(id, item.getScore());
								Log.i(TAG, "Player [" + name + "] logged in @" + id);
								
								updateWorkers(this, "An old player logged in!");
								
								playerId = id;
								return;
							}								
						}
						
						id = System.currentTimeMillis();							
						
						item = playerByName(name);
						if(item == null)
						{
							item = new ScoreItem();
							item.setId(id);
							item.setPlayer(name);
							item.setScore(0);
							item.setRemaining(turns);
							item.setMod(0);
							item.setState(0);
							players.addElement(item);
							
							sendAuth(id, 0);
							Log.i(TAG, "Player [" + name + "] logged in @" + id);
							
							updateWorkers(this, "A new player logged in!");
							
							playerId = id;
							return;
						}
						else
						{
							sendError("Player name [" + name + "] already taken!");
							return;
						}
					}
				}
				catch (JSONException e)
				{
					Log.i(TAG, "processAuth Exception: " + e.toString());
				}

				sendError("Unable to login player!");
			}
		}
		
		public void processQuit(JSONObject jo)
		{
			if(jo.has("id"))
			{
				long id;
				try
				{
					id = jo.getLong("id");

					if(removeScore(id))	
					{
						Log.i(TAG, "Player " + id + " removed!");
						updateWorkers(null, "A player has logged out!");
					}
					else
						Log.i(TAG, "Player " + id + " not found!");
					
				}
				catch (JSONException e)
				{
					Log.i(TAG, "processQuit Exception: " + e.toString());
				}
			}
		}
		
		public void processWipe(JSONObject jo)
		{
			if(jo.has("id"))
			{
				long id;
				try
				{
					id = jo.getLong("id");

					String msg = "";
					if(removeScore(id))		
					{
						removePlayWorker(id);
						
						msg = "A player has been removed!";
						updateWorkers(null, msg);
					}
					else
					{
						msg = "Player " + id + " not found!";
						sendError(msg);
					}
					
					Log.i(TAG, msg);
				}
				catch (JSONException e)
				{
					Log.i(TAG, "processWipe Exception: " + e.toString());
				}
			}
		}
		
		public void processHilite(JSONObject jo)	//id, delta
		{
			if(jo.has("id") && jo.has("data"))
			{
				try
				{
					long 	id 		= jo.getLong("id");
					long	state 	= jo.getLong("data");

					ScoreItem item = updateState(id, (int)state);
					if(item != null)
					{						
						updateWorkers(null, "A player has been " + (state == 1 ? "" : "un") + "hilighted!");
						return;
					}
				}
				catch(Exception e)
				{
					
				}
			} 
			sendError("Unable to hilight player!");
			return;
		}

		public void processPlay(JSONObject jo)	//id, delta
		{
			if(jo.has("id") && jo.has("data"))
			{
				try
				{
					long 	id 		= jo.getLong("id");
					int 	delta 	= jo.getInt("data");

					ScoreItem item = updateScore(id, delta);
					if(item != null)
					{						

						sendScore(item.getScore());
						
						if(item.getRemaining() == 0)
							sendError("You have used up your turns!");

						updateWorkers(this, "A player changed scores");
						return;
					}
				}
				catch(Exception e)
				{
					
				}
			} 
			sendError("Unable to update your score!");
			return;
		}

		//TODO
		public void processOpts(JSONObject jo)	//id, delta
		{
			if(jo.has("id") && jo.has("data"))
			{
				try
				{
					int 	oturns 	= jo.getInt("id");
					int 	odivisor = jo.getInt("data");

					turns 	= oturns;
					divisor = odivisor;

					resetGame();
					
					String msg = "Game has been reset!";
					
					updateWorkers(this, msg);
					
					updatePlayWorkers(this, msg);
					
					Log.i(TAG, "Game reset: turns: " + turns + " divisor: " + divisor);
					sendOpts(msg);
					return;
				}
				catch(Exception e)
				{
					
				}
			} 
			sendError("Unable to reset game!");
			return;
		}

		private void sendList(String msg) 
		{
			JSONObject json = new JSONObject();
			try
			{
				JSONArray arr = new JSONArray();
				for(int i = 0; i < players.size(); i++)
				{
					try
					{
						ScoreItem item = players.get(i);
						JSONObject obj = item.getJSON();					
						arr.put(i, obj);						
					}
					catch(Exception e)
					{
						
					}
				}
				
				json.put("res", CMD_LIST);
				json.put("msg", msg);					
				json.put("data", arr);	
				
				sendJson(json);
			}
			catch (JSONException e)
			{
			}
		}

		private void sendAuth(long id, int score)
		{
			JSONObject json = new JSONObject();
			try
			{
				json.put("res", CMD_AUTH);
				json.put("msg", String.valueOf(score));	
				json.put("data", id);	
				
				sendJson(json);
			}
			catch (JSONException e)
			{
			}
		}
		
		private void sendPass()
		{
			JSONObject json = new JSONObject();
			try
			{
				json.put("res", CMD_PASS);
				json.put("msg", String.valueOf(divisor));	
				json.put("data", turns);	
				
				sendJson(json);
			}
			catch (JSONException e)
			{
			}
		}

		private void sendScore(int score)
		{
			JSONObject json = new JSONObject();
			try
			{
				json.put("res", CMD_PLAY);
				json.put("msg", "Score updated!");	
				json.put("data", score);	
				
				sendJson(json);
			}
			catch (JSONException e)
			{
			}
		}

		private void sendOpts(String msg)
		{
			Log.i(TAG, "sendOpts: " + msg);

			JSONObject json = new JSONObject();
			try
			{
				json.put("res", CMD_OPTS);
				json.put("msg", msg);
				json.put("data", 0);	
				
				sendJson(json);
			}
			catch (JSONException e)
			{
			}
		}

		private void sendError(String msg)
		{
			Log.i(TAG, "sendError: " + msg);

			JSONObject json = new JSONObject();
			try
			{
				json.put("res", CMD_ERRO);
				json.put("msg", msg);
				json.put("data", 0);	
				
				sendJson(json);
			}
			catch (JSONException e)
			{
			}
		}

		private void sendJson(JSONObject json)
		{
			if(json == null) return;
			try
			{
				String data = json.toString();
				write(data);
			}
			catch (IOException e)
			{
			}
		}

	}

}
