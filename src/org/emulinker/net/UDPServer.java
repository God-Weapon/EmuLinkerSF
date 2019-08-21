package org.emulinker.net;

import java.util.*;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import org.apache.commons.logging.*;
import org.emulinker.util.*;
import org.picocontainer.Startable;

public abstract class UDPServer implements Executable, Startable
{
	private static Log		log			= LogFactory.getLog(UDPServer.class);
/*
	private static int		artificalPacketLossPercentage = 0;
	private static int		artificalDelay = 0;
	private static Random	random = new Random();

	static
	{
		try
		{
			artificalPacketLossPercentage = Integer.parseInt(System.getProperty("artificalPacketLossPercentage"));
			artificalDelay = Integer.parseInt(System.getProperty("artificalDelay"));
		}
		catch(Exception e) {}
		
		if(artificalPacketLossPercentage > 0)
			log.warn("Introducing " + artificalPacketLossPercentage + "% artifical packet loss!");
		
		if(artificalDelay > 0)
			log.warn("Introducing " + artificalDelay + "ms artifical delay!");
	}
*/
	private int				bindPort;
	private DatagramChannel	channel;
	private boolean			isRunning	= false;
	private boolean			stopFlag	= false;
	
	public UDPServer()
	{
		this(true);
	}

	public UDPServer(boolean shutdownOnExit)
	{
		if (shutdownOnExit)
			Runtime.getRuntime().addShutdownHook(new ShutdownThread());
	}

	public int getBindPort()
	{
		return bindPort;
	}

	public boolean isRunning()
	{
		return isRunning;
	}

	public synchronized boolean isBound()
	{
		if (channel == null)
			return false;
		if (channel.socket() == null)
			return false;
		return !channel.socket().isClosed();
	}

	public boolean isConnected()
	{
		return channel.isConnected();
	}

	public synchronized void start()
	{
		log.debug(toString() + " received start request!");
		if (isRunning)
		{
			log.debug(toString() + " start request ignored: already running!");
			return;
		}

		stopFlag = false;
	}

	protected boolean getStopFlag()
	{
		return stopFlag;
	}

	public synchronized void stop()
	{
		stopFlag = true;

		if (channel != null)
		{
			try
			{
				channel.close();
			}
			catch (IOException e)
			{
				log.error("Failed to close DatagramChannel: " + e.getMessage());
			}
		}
	}

	protected synchronized void bind() throws BindException
	{
		bind(-1);
	}

	protected synchronized void bind(int port) throws BindException
	{
		try
		{
			channel = DatagramChannel.open();

			if (port > 0)
				channel.socket().bind(new InetSocketAddress(port));
			else
				channel.socket().bind(null);

			bindPort = channel.socket().getLocalPort();

			ByteBuffer tempBuffer = getBuffer();
			int bufferSize = (tempBuffer.capacity() * 2);
			releaseBuffer(tempBuffer);

			channel.socket().setReceiveBufferSize(bufferSize);
			channel.socket().setSendBufferSize(bufferSize);
		}
		catch (IOException e)
		{
			throw new BindException("Failed to bind to port " + port, port, e);
		}

		this.start();
	}

	protected abstract ByteBuffer getBuffer();

	protected abstract void releaseBuffer(ByteBuffer buffer);

	protected abstract void handleReceived(ByteBuffer buffer, InetSocketAddress remoteSocketAddress);

	protected void send(ByteBuffer buffer, InetSocketAddress toSocketAddress)
	{
		if (!isBound())
		{
			log.warn("Failed to send to " + EmuUtil.formatSocketAddress(toSocketAddress) + ": UDPServer is not bound!");
			return;
		}
		/*
		if(artificalPacketLossPercentage > 0 && Math.abs(random.nextInt()%100) < artificalPacketLossPercentage)
		{
			return;
		}
		*/
		try
		{
//			log.debug("send("+EmuUtil.dumpBuffer(buffer, false)+")");
			channel.send(buffer, toSocketAddress);
		}
		catch (Exception e)
		{
			log.error("Failed to send on port " + getBindPort() + ": " + e.getMessage(), e);
		}
	}

	public void run()
	{
		isRunning = true;
		log.debug(toString() + ": thread running...");

		try
		{
			while (!stopFlag)
			{
				try
				{
					ByteBuffer buffer = getBuffer();
					InetSocketAddress fromSocketAddress = (InetSocketAddress) channel.receive(buffer);

					if (stopFlag)
						break;

					if (fromSocketAddress == null)
						throw new IOException("Failed to receive from DatagramChannel: fromSocketAddress == null");
					/*
					if(artificalPacketLossPercentage > 0 && Math.abs(random.nextInt()%100) < artificalPacketLossPercentage)
					{
						releaseBuffer(buffer);
						continue;
					}
					
					if(artificalDelay > 0)
					{
						try
						{
							Thread.sleep(artificalDelay);
						}
						catch(Exception e) {}
					}
					*/
					buffer.flip();
//					log.debug("receive("+EmuUtil.dumpBuffer(buffer, false)+")");					
					handleReceived(buffer, fromSocketAddress);
					releaseBuffer(buffer);
				}
				catch (SocketException e)
				{
					if (stopFlag)
						break;

					log.error("Failed to receive on port " + getBindPort() + ": " + e.getMessage());
				}
				catch (IOException e)
				{
					if (stopFlag)
						break;

					log.error("Failed to receive on port " + getBindPort() + ": " + e.getMessage());
				}
			}
		}
		catch (Throwable e)
		{
			log.fatal("UDPServer on port " + getBindPort() + " caught unexpected exception!", e);
			stop();
		}
		finally
		{
			isRunning = false;
			log.debug(toString() + ": thread exiting...");
		}
	}

	private class ShutdownThread extends Thread
	{
		private ShutdownThread()
		{
		}

		public void run()
		{
			UDPServer.this.stop();
		}
	}
}
