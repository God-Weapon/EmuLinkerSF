package org.emulinker.util;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import javax.inject.Inject;

public class EmuLinkerExecutor extends ThreadPoolExecutor {

  @Inject
  EmuLinkerExecutor() {
    // super(config.getInt("threadPool.coreSize"), config.getInt("threadPool.maxSize"),
    // config.getLong("threadPool.keepAlive"), TimeUnit.SECONDS, queue);
    // super((config.getInt("server.maxUsers")*2)+10, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new
    // SynchronousQueue<Runnable>());
    // super.prestartAllCoreThreads();
    super(5, Integer.MAX_VALUE, 60L, SECONDS, new SynchronousQueue<Runnable>());
  }
}
