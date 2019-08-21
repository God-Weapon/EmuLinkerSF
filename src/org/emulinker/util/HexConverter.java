package org.emulinker.util;

import java.io.*;

public class HexConverter
{
	public static void main(String args[]) throws Exception
	{
		BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(args[1]));
		BufferedReader is = new BufferedReader(new FileReader(args[0]));
		String line = null;
		while((line = is.readLine()) != null)
		{
			byte[] bytes = EmuUtil.hexToByteArray(line);
			os.write(bytes);
		}
		
		is.close();
		os.close();
	}
}
