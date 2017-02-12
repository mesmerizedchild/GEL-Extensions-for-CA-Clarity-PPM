package eu.mesmerizedChild.commons.jelly;

import org.apache.commons.jelly.TagLibrary;

import eu.mesmerizedChild.commons.jelly.bpm.FindProcessTag;
import eu.mesmerizedChild.commons.jelly.core.BreakableTag;
import eu.mesmerizedChild.commons.jelly.core.ContinueTag;
import eu.mesmerizedChild.commons.jelly.sql.DeadlockProtectedTransactionTag;
import eu.mesmerizedChild.commons.jelly.sql.ForEachRowTag;
import eu.mesmerizedChild.commons.jelly.sql.QueryIntoTag;
import eu.mesmerizedChild.commons.jelly.sql.SqlTagLibrary;

public class MChTagLibrary extends TagLibrary {

  public static final String         NAMESPACE_URI = "jelly:mch";

  // Just a reference so that compiling this class alone will also compile SqlTagLibrary
  @SuppressWarnings("unused")
  private static final SqlTagLibrary DUMMY         = null;

  public MChTagLibrary() {
    // Core
    registerTag("continue", ContinueTag.class);
    registerTag("breakable", BreakableTag.class);

    // SQL
    registerTag("transaction", DeadlockProtectedTransactionTag.class);
    registerTag("query", QueryIntoTag.class);
    registerTag("forEachRow", ForEachRowTag.class);

    // BPM
    registerTag("findProcess", FindProcessTag.class);
  }
}
