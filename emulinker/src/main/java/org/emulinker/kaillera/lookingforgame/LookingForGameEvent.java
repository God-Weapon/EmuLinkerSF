package org.emulinker.kaillera.lookingforgame;

import com.google.auto.value.AutoValue;
import org.emulinker.kaillera.model.KailleraUser;

@AutoValue
public abstract class LookingForGameEvent {
  public abstract int gameId();

  public abstract String gameTitle();

  public abstract KailleraUser user();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setGameId(int gameId);

    public abstract Builder setGameTitle(String gameTitle);

    public abstract Builder setUser(KailleraUser user);

    public abstract LookingForGameEvent build();
  }

  public static Builder builder() {
    return new AutoValue_LookingForGameEvent.Builder();
  }
}
