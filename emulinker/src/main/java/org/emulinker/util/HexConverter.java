package org.emulinker.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class HexConverter {
  public static void main(String args[]) throws Exception {
    BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(args[1]));
    BufferedReader is = Files.newBufferedReader(Paths.get(args[0]), UTF_8);
    String line = null;
    while ((line = is.readLine()) != null) {
      byte[] bytes = EmuUtil.hexToByteArray(line);
      os.write(bytes);
    }

    is.close();
    os.close();
  }
}
