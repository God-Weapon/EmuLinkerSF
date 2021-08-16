package org.emulinker.kaillera.model.event;

public interface KailleraEventListener {
  public void actionPerformed(KailleraEvent event);

  public void stop();
}
