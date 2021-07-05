package com.tutorialcodes;

import java.util.*;
import java.io.*;
import java.net.*;

public class MusicServer
{
	ArrayList<ObjectOutputStream> clientOutputStreams;
	class ClientHandler implements Runnable
	{
		ObjectInputStream in;
		Socket sock;
		public ClientHandler(Socket clientSocket)
		{
			try {
				sock = clientSocket;
				in = new ObjectInputStream(sock.getInputStream());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		public void run()
		{
			Object obj;
			try {
				while ((obj = in.readObject()) != null)
				{
					String message = (String)obj;
					boolean[] checkboxState = (boolean[]) in.readObject();
					tellEveryone(message, checkboxState);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	public static void main(String[] args)
	{
		new MusicServer().go();
	}
	public void go()
	{
		clientOutputStreams = new ArrayList<ObjectOutputStream>();
		try {
			ServerSocket serverSock = new ServerSocket(4242);
			while (true)
			{
				Socket clientSocket = serverSock.accept();
				ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
				clientOutputStreams.add(out);
				
				Thread t = new Thread(new ClientHandler(clientSocket));
				t.start();
				System.out.println("got a connection");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void tellEveryone(String message, boolean[] music)
	{
		Iterator it = clientOutputStreams.iterator();
		while (it.hasNext())
		{
			try {
				ObjectOutputStream out = (ObjectOutputStream)it.next();
				out.writeObject(message);
				out.writeObject(music);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}