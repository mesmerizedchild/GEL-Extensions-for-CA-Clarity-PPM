package eu.mesmerizedChild.commons.jelly.bpm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.commons.jelly.JellyContext;
import org.apache.commons.jelly.JellyTagException;
import org.apache.commons.jelly.MissingAttributeException;
import org.apache.commons.jelly.XMLOutput;
import org.apache.commons.jelly.xpath.XPathTagSupport;

import com.niku.bpm.utilities.PersistenceUtils;
import com.niku.union.gel.GELContext;
import com.niku.union.persistence.connection.ConnectionContext;
import com.niku.union.security.DefaultSecurityIdentifier;
import com.niku.union.security.SecurityIdentifier;

// Mostly copied from com.niku.bpm.gel.tags.StartProcessTag
public class FindProcessTag extends XPathTagSupport {

  // @formatter:off
  private static final String QUERY_BASE = "SELECT vers.id version_id, \n" +
                                           "       proc.process_code \n" +
                                           "FROM   bpm_def_processes proc \n" +
                                           "       inner join bpm_def_process_versions vers \n" +
                                           "              ON ( proc.id = vers.process_id \n" +
                                           "                   AND vers.user_status_code = 'BPM_PUS_ACTIVE' ) \n";
  private static final String QUERY_EXACT = QUERY_BASE +
                                            "WHERE  proc.process_code = ?";
  private static final String QUERY_LATEST = QUERY_BASE +
                                             "WHERE  proc.process_code LIKE ?";
  // @formatter:on

  private String              processCode;
  private int                 initUserId   = -1;
  private boolean             useLatest;
  private String              var;

  public FindProcessTag() {
  }

  @Override
  public void doTag(XMLOutput output) throws JellyTagException {
    checkMandatoryAttributes();

    int initUserId = this.initUserId;
    String processCode = this.processCode;
    boolean useLatest = this.useLatest;

    Connection conn = null;
    JellyContext context = getContext();
    try {
      SecurityIdentifier secId;
      if (context instanceof GELContext) {
        GELContext gelContext = (GELContext) context;
        conn = gelContext.getConnection();
        secId = gelContext.getSecurityIdentifier();
      }
      else {
        secId = new DefaultSecurityIdentifier();
        secId.setUserId(initUserId);

        conn = ConnectionContext.getContext("Niku").getConnection(1); // No auto-commit
        String userName = PersistenceUtils.getUserName(initUserId, secId, conn);
        secId.setUserName(userName);
        conn.commit();
      }

      PreparedStatement ps;
      if (useLatest) {
        // Look for the process with the latest code.
        ps = conn.prepareStatement(QUERY_LATEST);
        ps.setString(1, processCode + "_%");
      }
      else {
        // Look for the process with the exact code.
        ps = conn.prepareStatement(QUERY_EXACT);
        ps.setString(1, processCode);
      }

      // Process the results, do some checks
      ResultSet rs = ps.executeQuery();
      if (!rs.next())
        throw new IllegalArgumentException("Cannot find any active versions for process " + processCode);
      String newProcessCode = rs.getString("process_code");
      if (newProcessCode == null || newProcessCode.length() < processCode.length())
        throw new IllegalArgumentException("Found process code " + newProcessCode + " for process code " + processCode + ", which is not quite right...");
      if (rs.next())
        throw new IllegalStateException("Found more than one version for for process code " + processCode);

      // Good to go, we can take version_id
      long processId = rs.getLong("version_id");
      // Don't cache, in this release [also because another caching mechanism should be found...]
      // processIdMap.put(processKey, processId);

      // Clean up
      rs.close();
      ps.close();
      conn.commit();

      context.setVariable(var, processId);
    }
    catch (Throwable t) {
      try {
        if (conn != null)
          conn.rollback();
      }
      catch (Exception e1) {
      }

      if (t instanceof JellyTagException)
        throw (JellyTagException) t;
      throw new JellyTagException("An error occurred when initiating a process instance using process version code " + this.processCode, t);
    }
    finally {
      try {
        if (!(this.context instanceof GELContext) && conn != null)
          conn.close();
      }
      catch (Exception e2) {
      }
    }
  }

  private void checkMandatoryAttributes() throws MissingAttributeException {
    if (this.processCode == null)
      throw new MissingAttributeException("processCode");
    if (this.initUserId == -1)
      throw new MissingAttributeException("initUserId");
    if (!(this.context instanceof GELContext) && this.initUserId == -1)
      throw new MissingAttributeException("initUserId");
  }

  public void setProcessCode(String processCode) {
    this.processCode = processCode;
  }

  public void setInitUserId(int initUserId) {
    this.initUserId = initUserId;
  }

  public void setUseLatest(boolean useLatest) {
    this.useLatest = useLatest;
  }

  public void setVar(String var) {
    this.var = var;
  }
}
