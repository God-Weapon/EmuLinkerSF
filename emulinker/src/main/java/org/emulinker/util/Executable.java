package org.emulinker.util;

public interface Executable extends Runnable
{
	public boolean isRunning();

	public void stop();
}
