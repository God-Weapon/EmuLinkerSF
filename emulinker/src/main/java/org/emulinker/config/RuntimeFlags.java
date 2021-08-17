package org.emulinker.config;

import com.google.auto.value.AutoValue;
import java.nio.charset.Charset;

/** Configuration flags that are set at startup and do not change until the job is restarted. */
@AutoValue
public abstract class RuntimeFlags {
  public abstract Charset charset();

  public static Builder builder() {
    return new AutoValue_RuntimeFlags.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setCharset(Charset charset);

    public abstract RuntimeFlags build();
  }
}
