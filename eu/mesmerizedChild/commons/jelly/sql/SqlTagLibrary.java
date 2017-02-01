package eu.mesmerizedChild.commons.jelly.sql;

import org.apache.commons.jelly.TagLibrary;

/** For backwards compatibility with the time when only the transaction tag was in this package... */
public class SqlTagLibrary extends TagLibrary {
  public SqlTagLibrary() {
    registerTag("transaction", DeadlockProtectedTransactionTag.class);
  }
}
