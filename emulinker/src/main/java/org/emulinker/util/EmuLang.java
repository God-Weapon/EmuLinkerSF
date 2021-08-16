package org.emulinker.util;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.*;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EmuLang {
  private static Log log = LogFactory.getLog(EmuLang.class);

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
    try {
      RESOURCE_BUNDLE.getString(key);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public static String getString(String key) {
    try {
      String str = RESOURCE_BUNDLE.getString(key);
      try {
        byte[] buff = str.getBytes("ISO-8859-1");
        str = new String(buff, System.getProperty("emulinker.charset"));
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
      return str;
    } catch (MissingResourceException e) {
      log.error("Missing language property: " + key);
      return key;
    }
  }

  public static String getString(String key, Object... messageArgs) {
    try {
      String str = RESOURCE_BUNDLE.getString(key);
      try {
        byte[] buff = str.getBytes("ISO-8859-1");
        str = new String(buff, System.getProperty("emulinker.charset"));
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
      return (new MessageFormat(str)).format(messageArgs);
    } catch (MissingResourceException e) {
      log.error("Missing language property: " + key);
      return key;
    }
  }
}
