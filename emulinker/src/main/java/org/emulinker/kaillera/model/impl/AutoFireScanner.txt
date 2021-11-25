package org.emulinker.kaillera.model.impl;
/*
package org.emulinker.kaillera.model.impl;

import java.util.*;
import java.util.regex.*;
import java.util.concurrent.*;

import java.io.*;

import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.FluentLogger;
import org.emulinker.kaillera.model.KailleraGame;
import org.emulinker.kaillera.model.KailleraUser;
import org.emulinker.kaillera.model.*;
import org.emulinker.util.*;

public class AutoFireScanner implements AutoFireDetector
{
	private static final FluentLogger logger = FluentLogger.forEnclosingClass();

	private static int MIN_CHAR = 65;
	private static int MAX_CHAR = 90;

	private HashMap<Integer, ScanningJob>	jobs;
	private ExecutorService	 				executor;

	public AutoFireScanner()
	{
		jobs = new HashMap<Integer, ScanningJob>();
		executor = Executors.newCachedThreadPool();
	}

	public void start(KailleraGame game, KailleraUser user, int maxDelay, int sensitivity)
	{
		ScanningJob job = new ScanningJob(game, user, maxDelay, sensitivity);
		jobs.put(user.getID(), job);
	}

	public void stop(KailleraUser user)
	{
		ScanningJob job = jobs.remove(user.getID());
		if(job == null)
			logger.atSevere().log("AutoFireScanner stop failed: User not found: " + user);
	}

	public void addData(KailleraUser user, byte[] data, int bytesPerAction)
	{
		ScanningJob job = jobs.get(user.getID());
		if(job == null)
		{
			logger.atSevere().log("AutoFireScanner addData failed: User not found: " + user);
			return;
		}

		synchronized(job)
		{
			job.addData(data, bytesPerAction);

			if(job.isFull())
			{
				ScanningJob oldJob = job;
				job = new ScanningJob(oldJob.getGame(), user, oldJob.getMaxDelay(), oldJob.getSensitivity());
				jobs.put(user.getID(), job);
				executor.submit(oldJob);
			}
		}
	}

	protected class ScanningJob implements Runnable
	{
		private KailleraGame game;
		private KailleraUser user;
		private int maxDelay;
		private int sensitivity;
		private int bytesPerAction;

		private int sizeLimit;
		private int size = 0;

		private ArrayList<byte[]> buffer = new ArrayList<byte[]>();

		protected ScanningJob(KailleraGame game, KailleraUser user, int maxDelay, int sensitivity)
		{
			this.game = game;
			this.user = user;
			this.maxDelay = maxDelay;
			this.sensitivity = sensitivity;

			sizeLimit = ((maxDelay+1)*sensitivity*3);
		}

		protected KailleraGame getGame()
		{
			return game;
		}

		protected KailleraUser getUser()
		{
			return user;
		}

		protected int getMaxDelay()
		{
			return maxDelay;
		}

		protected int getSensitivity()
		{
			return sensitivity;
		}

		protected void addData(byte[] data, int bytesPerAction)
		{
			buffer.add(data);
			this.bytesPerAction = bytesPerAction;
			size += (data.length/bytesPerAction);
		}

		protected boolean isFull()
		{
			return (size >= sizeLimit);
		}

		public void run()
		{
			// create a map to translate actions into keys
			HashMap<String, Integer> keyMap = new HashMap<String, Integer>();
			// create a comprehensive array to hold all the assembled translated actions for each byte array in the buffer
			int[] keys = new int[size];
			int keyCounter = 0;
			// we want to translate actions into characters for the regex scanner, using only readable characters
			int charIndex = MIN_CHAR;

			// iterate through the buffer, it contains byte arrays
			for(byte[] data : buffer)
			{
				// determine the number of actions in this array
				int actionCount = (data.length/bytesPerAction);

				for(int i=0; i<actionCount; i++)
				{
					// convert the individual action to a string representation
					String s = EmuUtil.bytesToHex(data, (i*bytesPerAction), bytesPerAction);

					// get a mapping for this action to a key
					int key = -1;
					Integer keyInteger = keyMap.get(s);
					if(keyInteger == null)
					{
						// this is a new action, create a new key
						key = charIndex++;
						keyInteger = new Integer(key);
						keyMap.put(s, key);

						// this is a kludge... if we run out of keys rollover to the first
						if(charIndex > MAX_CHAR)
							charIndex = MIN_CHAR;
					}
					else
					{
						// this is a previously seen action
						key = keyInteger.intValue();
					}

					// add the key to the comprehensive array
					keys[keyCounter] = key;
					keyCounter++;
				}
			}

			// declare our counters, etc to use while we looks for repetitive segments in the array
			int k1 = -1;
			int k2 = -1;
			int k1Count = 0;
			int k2Count = 0;
			int startPos = 0;

			// iterator through the entire array looking for sections containing 2 characters only
			for(int thisPos = 0; thisPos<keys.length; thisPos++)
			{
				int key = keys[thisPos];

				if(k1 < 0)
				{
					// k1 is not initialized
					k1 = key;
					k1Count = 1;
					startPos = thisPos;
				}
				else if(k1 == key)
				{
					k1Count++;
				}
				else if(k2 < 0)
				{
					// k2 is not initialized
					k2 = key;
					k2Count = 1;
				}
				else if(k2 == key)
				{
					k2Count++;
				}
				else
				{
					// our pattern has ended
					if(k1Count > sensitivity && k2Count > sensitivity)
					{
						if(checkRegex(keys, startPos, keys.length, k1, k2))
						{
							// pattern was detected... nothing more to do here
							return;
						}
					}

					k2 = k1;
					k2Count = 1;
					k1 = key;
					k1Count = 1;
					startPos = (thisPos-1);
				}
			}

			// check for a match at the end of the array also
			if(k1Count > sensitivity && k2Count > sensitivity)
			{
				checkRegex(keys, startPos, keys.length, k1, k2);
			}
		}

		private boolean checkRegex(int[] keys, int startPos, int endPos, int k1, int k2)
		{
			StringBuilder sb = new StringBuilder();
			for(int i=startPos; i<endPos; i++)
			{
				sb.append((char)keys[i]);
			}

			// now scan the string using a regular expression that is made to check for repetition
			StringBuilder regex = new StringBuilder();
			sb.append(".*(");
			sb.append((char)k1);
			sb.append((char)k2);
			sb.append("{1,");
			sb.append(maxDelay);
			sb.append("}|");
			sb.append((char)k2);
			sb.append((char)k1);
			sb.append("{1,");
			sb.append(maxDelay);
			sb.append("})(\\1{)");
			sb.append((sensitivity-1));
			sb.append(",}).*");
			Pattern pattern = Pattern.compile(regex.toString());
			Matcher matcher = pattern.matcher(sb);
			if(matcher.matches())
			{
				String sig = matcher.group(1);
				int delay = (sig.length()-1);
				// positive detection!
				KailleraGameImpl gameImpl = (KailleraGameImpl) game;
				gameImpl.announce("Autofire Detected: Delay " + delay + ": " + user.getName());
				logger.atInfo().log("Autofire Detected: Delay: " + delay + ": " + game + ": " + user + ": " + sig + ": " + sb.toString());
				logger.atInfo().log("AUTOUSERDUMP\t" + EmuUtil.DATE_FORMAT.format(gameImpl.getStartDate()) + "\t" + delay + "\t" + game.getID() + "\t" + game.getRomName() + "\t" + user.getName() + "\t" + user.getSocketAddress().getAddress().getHostAddress());
				return true;
			}

			return false;
		}
	}
}
*/
