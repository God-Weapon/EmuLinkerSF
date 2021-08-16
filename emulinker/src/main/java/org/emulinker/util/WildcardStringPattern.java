package org.emulinker.util;

import java.util.*;

public class WildcardStringPattern {
  protected boolean equals;
  protected boolean startsWith;
  protected boolean endsWith;
  protected boolean contains;

  private String startString = "";
  private String endString = "";
  private LinkedList<String> containsStrings = new LinkedList<String>();

  public static void main(String args[]) {
    WildcardStringPattern test = new WildcardStringPattern(args[0]);
    for (int i = 1; i < args.length; i++) {
      boolean match = test.match(args[i]);
      System.out.println(args[i] + " = " + match);
    }
  }

  public WildcardStringPattern(String pattern) {
    if (pattern == null || pattern.equals("")) {
      // match() function will always return true.
      return;
    }

    LinkedList<String> elements = new LinkedList<String>();
    StringTokenizer st = new StringTokenizer(pattern, "*", true);
    while (st.hasMoreElements()) {
      elements.add(st.nextToken());
    }

    if (elements.size() == 1) {
      String s = elements.getFirst();
      if (s.equals("*")) return;

      // Real text
      equals = true;
      startString = elements.getFirst();
      return;
    }

    // Multiple elements in the pattern
    // Pick off start and end strings.
    // Add remaining items to the list of contains strings.
    if (!elements.getFirst().equals("*")) {
      startsWith = true;
      startString = elements.getFirst();
      elements.removeFirst();
    }

    if (!elements.getLast().equals("*")) {
      endsWith = true;
      endString = elements.getLast();
      elements.removeLast();
    }

    for (String x : elements) {
      if (x.equals("*")) continue;
      containsStrings.add(x);
      contains = true;
    }
  }

  public boolean match(String s) {
    if (s == null || s.equals("")) return false;
    if (equals && !s.equals(startString)) return false;
    if (startsWith && !s.startsWith(startString)) return false;
    if (endsWith && !s.endsWith(endString)) return false;
    if (contains) {
      for (String pattern : containsStrings) {
        int idx = s.indexOf(pattern);
        if (idx == -1) return false;
        if (idx + pattern.length() == s.length()) {
          // Match occured at the end of the string.
          // In that case, the substring assignment below
          // would fail. Continue the loop. If there are
          // more items to match, the next test will fail.
          // Otherwise, the match has succeeded.
          s = "";
          continue;
        }
        s = s.substring(idx + pattern.length());
      }
    }
    return true;
  }

  @Override
  public String toString() {
    if (equals) return startString;

    // If there is a startString, append "*" to it.
    // (There must be a follow up, or this would be an equals match.)
    // If startString is null, the remainder of the pattern is
    // an endsWith or contains. In either case, it must start
    // with "*".

    String s = startString;
    s += "*";
    for (String pattern : containsStrings) s += (String) pattern + "*";
    s += endString;
    return s;
  }
}
