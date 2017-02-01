package eu.mesmerizedChild.commons.jelly.core;

import org.apache.commons.jelly.JellyTagException;
import org.apache.commons.jelly.MissingAttributeException;
import org.apache.commons.jelly.TagSupport;
import org.apache.commons.jelly.XMLOutput;
import org.apache.commons.jelly.impl.BreakException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** A code block where flow can jump to the end by invoking <code>&lt;core:break><code>. */
public class BreakableTag extends TagSupport {

  private static final Log LOG = LogFactory.getLog(BreakableTag.class);

  public BreakableTag() {
    //
  }

  @Override
  public void doTag(XMLOutput output) throws MissingAttributeException, JellyTagException {
    try {
      invokeBody(output);
    }
    catch (BreakException e) {
      if (LOG.isDebugEnabled())
        LOG.debug("block terminated by break: " + e, e);
    }
  }
}
