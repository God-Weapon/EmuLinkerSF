package org.emulinker.util;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.nio.charset.Charset;
import java.util.NoSuchElementException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import org.apache.commons.configuration.Configuration;
import org.emulinker.config.RuntimeFlags;
import org.emulinker.kaillera.relay.KailleraRelay;

public class EmuLinkerExecutor extends ThreadPoolExecutor {
  private static final String CONFIG_CHARSET_KEY = "emulinker.charset";

  public EmuLinkerExecutor(Configuration config) throws NoSuchElementException {
    // super(config.getInt("threadPool.coreSize"), config.getInt("threadPool.maxSize"),
    // config.getLong("threadPool.keepAlive"), TimeUnit.SECONDS, queue);
    // super((config.getInt("server.maxUsers")*2)+10, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new
    // SynchronousQueue<Runnable>());
    // super.prestartAllCoreThreads();
    super(5, Integer.MAX_VALUE, 60L, SECONDS, new SynchronousQueue<Runnable>());
    buildRuntimeConfig(config);
  }

  private void buildRuntimeConfig(Configuration config) {
    String charsetName = getDesiredCharset(config);
    Charset charset;
    try {
      if (Charset.isSupported(charsetName)) {
        charset = Charset.forName(charsetName);
      } else {
        throw new IllegalStateException("Unsupported charset: " + charsetName);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Unable to load charset " + charsetName, e);
    }

    KailleraRelay.config = RuntimeFlags.builder().setCharset(charset).build();
  }

  private String getDesiredCharset(Configuration config) {
    return config.getString(CONFIG_CHARSET_KEY);
  }
}
