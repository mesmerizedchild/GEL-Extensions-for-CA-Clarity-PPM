package eu.mesmerizedChild.commons.jelly.sql;

import java.sql.SQLException;

import org.apache.commons.jelly.JellyTagException;
import org.apache.commons.jelly.XMLOutput;
import org.apache.commons.jelly.tags.sql.TransactionTag;

// Only Oracle is supported at the moment.
// Deadlocks coming from other RDBMS will still cause an SQLException at the first try
public class DeadlockProtectedTransactionTag extends TransactionTag {

  private static final String ORACLE_DB_VENDOR  = "Oracle";
  private static final int    ORACLE_ERROR_CODE = 60;

  private int                 retries           = 20;
  private int                 intervalMs        = 3000;

  public DeadlockProtectedTransactionTag() {
    //
  }

  public void setRetries(int retries) {
    this.retries = Math.max(1, retries);
  }

  public void setIntervalMs(int intervalMs) {
    this.intervalMs = Math.max(10, intervalMs);
  }

  @Override
  public void invokeBody(XMLOutput output) throws JellyTagException {

    // Try to execute the body
    int retry = 1;
    try {
      // FileOutputStream fos = new FileOutputStream("out.out", true);
      // DataOutputStream dos = new DataOutputStream(fos);

      String dbVendor = getSharedConnection().getMetaData().getDatabaseProductName();
      int intervalMs = this.intervalMs;

      // dos.writeUTF("\nPARAMS:\nretries: " + retries + ", intervalMs: " + intervalMs + "\n");
      // dos.writeUTF("DB Vendor: " + dbVendor + "\n");

      loopIt: while (true) {
        // Try to execute the body
        try {
          // dos.writeUTF("\n" + "0. Try no. " + retry + "\n");
          super.invokeBody(output);
        }
        catch (Throwable th) {
          // dos.writeUTF("1. " + th.getMessage() + "\n");

          Throwable t = th;
          // SQLException's are wrapped in JellyTagException's
          if (t instanceof JellyTagException)
            // Just in case the SQLException is nested within many JellyTagException's
            while (!((t = t.getCause()) instanceof SQLException) && t != null) {
            }

          // dos.writeUTF("2. " + t.toString() + "\n");

          if (t instanceof SQLException) {
            SQLException ex = (SQLException) t;
            // If it's a deadlock, and I haven't retried up to retries, then
            // rollback and wait, then retry.
            boolean isDeadlock = ex.getErrorCode() == ORACLE_ERROR_CODE && ORACLE_DB_VENDOR.equals(dbVendor);
            // dos.writeUTF("4.1. Detected deadlock: " + isDeadlock + "\n");

            if (isDeadlock) {
              if (retry++ < this.retries) {
                // dos.writeUTF("5. Rollback, sleep and retry. \n");
                getSharedConnection().rollback();
                try {
                  Thread.sleep(intervalMs);
                }
                catch (Exception e1) {
                }
                // Lengthen the interval a little bit with each retry
                intervalMs = (int) (intervalMs * (1.2 + Math.random() * 0.3));
                continue loopIt;
              }
              throw new JellyTagException("Abbandonata deadlock protection dopo " + this.retries + " tentativi.", t);
            }
          }
          // If none of the conditions above is met, just re-throw the exception
          throw th;
        }
        // dos.writeUTF("10. Done.\n");
        break loopIt;
      }
    }
    catch (Throwable th) {
      if (th instanceof JellyTagException)
        // Just re-throw the exception
        throw (JellyTagException) th;
      // Wrap anything else that came our way inside a JellyTagException
      throw new JellyTagException("Eccezione non JellyTagException:", th);
    }
  }
}
