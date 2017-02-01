package eu.mesmerizedChild.commons.jelly.core;

import org.apache.commons.jelly.JellyTagException;

public class ContinueException extends JellyTagException {
  private static final long serialVersionUID = 4749808997069384907L;

  public ContinueException() {
    super("Continue exception, moving to the next iteration in the parent loop");
  }
}
