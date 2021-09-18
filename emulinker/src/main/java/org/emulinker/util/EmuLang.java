package org.emulinker.util;

import com.google.common.flogger.FluentLogger;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class EmuLang {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String BUNDLE_NAME = "language";

  private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

  private EmuLang() {}
  /*
  	public static void reload()
  	{
  		try
  		{
  			Class klass = RESOURCE_BUNDLE.getClass().getSuperclass();
  			Field field = klass.getDeclaredField("cacheList");
  			field.setAccessible(true);
  			sun.misc.SoftCache cache = (sun.misc.SoftCache)field.get(null);
  			cache.clear();
  		}
  		catch(Exception e)
  		{

  		}
  	}
  */
  public static boolean hasString(String key) {
    if (RESOURCE_BUNDLE.containsKey(key)) {
      try {
        RESOURCE_BUNDLE.getString(key);
        return true;
      } catch (Exception e) {
        // It exists but is not readable.
        e.printStackTrace();
      }
    }
    return false;
  }

  public static String getString(String key) {
    try {
      return RESOURCE_BUNDLE.getString(key);
    } catch (MissingResourceException e) {
      logger.atSevere().withCause(e).log("Missing language property: " + key);
      return key;
    }
  }

  public static String getString(String key, Object... messageArgs) {
    try {
      String str = RESOURCE_BUNDLE.getString(key);
      return (new MessageFormat(str)).format(messageArgs);
    } catch (MissingResourceException e) {
      logger.atSevere().withCause(e).log("Missing language property: " + key);
      return key;
    }
  }
}
