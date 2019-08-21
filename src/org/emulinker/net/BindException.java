package org.emulinker.net;

public class BindException extends Exception
{
	private int	port;

	public BindException(String msg, int port, Exception e)
	{
		super(msg, e);
		this.port = port;
	}

	public int getPort()
	{
		return port;
	}
}
