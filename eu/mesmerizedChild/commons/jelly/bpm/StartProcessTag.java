package eu.mesmerizedChild.commons.jelly.bpm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.jelly.JellyTagException;
import org.apache.commons.jelly.MissingAttributeException;
import org.apache.commons.jelly.XMLOutput;
import org.apache.commons.jelly.xpath.XPathTagSupport;

import com.niku.bpm.utilities.PersistenceUtils;
import com.niku.union.bpm.BusinessProcessFactory;
import com.niku.union.config.ConfigurationManager;
import com.niku.union.config.properties.Database;
import com.niku.union.config.properties.types.DbVendorType;
import com.niku.union.gel.GELContext;
import com.niku.union.persistence.connection.ConnectionContext;
import com.niku.union.security.DefaultSecurityIdentifier;
import com.niku.union.security.SecurityIdentifier;

// Mostly copied from com.niku.bpm.gel.tags.StartProcessTag
public class StartProcessTag extends XPathTagSupport {

  // @formatter:off
  private static final String QUERY = "SELECT proc.process_code" +
                                      "FROM   bpm_def_processes proc " +
                                      "       inner join bpm_def_process_versions vers " +
                                      "              ON ( proc.id = vers.process_id " +
                                      "                   AND vers.user_status_code = 'BPM_PUS_ACTIVE' ) " +
                                      "WHERE  proc.process_code LIKE ?";
  // @formatter:on

  private String              processCode;
  private int                 initUserId   = -1;
  private String              initObjectKey;
  private long                initObjectId = -1;
  private boolean             useLatest;

  public StartProcessTag() {
  }

  @Override
  public void doTag(XMLOutput output) throws JellyTagException {
    checkMandatoryAttributes();

    int initUserId = this.initUserId;
    String processCode = this.processCode;
    String initObjectKey = this.initObjectKey;
    boolean useLatest = this.useLatest;

    Connection conn = null;
    try {
      SecurityIdentifier secId;
      DbVendorType dbVendor;
      String schemaName;
      if (this.context instanceof GELContext) {
        GELContext gelContext = (GELContext) this.context;
        conn = gelContext.getConnection();
        secId = gelContext.getSecurityIdentifier();
        dbVendor = gelContext.getDbVendorType();
        schemaName = gelContext.getSchemaName();
      }
      else {
        secId = new DefaultSecurityIdentifier();
        secId.setUserId(initUserId);
        ConfigurationManager cm = ConfigurationManager.getInstance();
        Database db = cm.getDatabase("Niku");
        dbVendor = db.getVendor();
        schemaName = db.getSchemaName();

        ConnectionContext context = ConnectionContext.getContext("Niku");
        conn = context.getConnection(1); // No auto-commit
        String userName = PersistenceUtils.getUserName(initUserId, secId, conn);
        secId.setUserName(userName);
        conn.commit();
      }

      if (useLatest) {
        // Look for the process with the latest code.
        // If processCode is 'myProcess' then we'll look for 'myProcess%'
        // Any SQL exceptions will be caught by the outer catch()

        PreparedStatement ps = conn.prepareStatement(QUERY);
        ps.setString(1, processCode + "_%");

        // process the results
        ResultSet rs = ps.executeQuery();
        if (!rs.next())
          throw new IllegalArgumentException("Cannot find any active versions for process " + processCode);
        String newProcessCode = rs.getString("process_code");
        if (newProcessCode == null || newProcessCode.length() < processCode.length())
          throw new IllegalArgumentException("Found process code " + newProcessCode + " for process code " + processCode + ", which is not quite right...");
        if (rs.next())
          throw new IllegalStateException("Found more than one for for process code " + processCode);
        rs.close();
        ps.close();
        processCode = newProcessCode;
      }

      Map<String, Long> objectMap = new HashMap<>();
      // Put the object ID, if relevant
      if (initObjectKey != null && !"".equals(initObjectKey) && this.initObjectId != -1)
        objectMap.put(initObjectKey, Long.valueOf(this.initObjectId));
      BusinessProcessFactory.getBusinessProcess().startProcess(processCode, initUserId, objectMap, secId, conn, dbVendor, schemaName);
      conn.commit();
      return;
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
      throw new MissingAttributeException("processVersionId");
    if (this.initUserId == -1)
      throw new MissingAttributeException("initUserId");
    if (this.initObjectKey != null && !"".equals(this.initObjectKey) && this.initObjectId == -1)
      throw new MissingAttributeException("initObjectId");
  }

  public void setProcessCode(String processCode) {
    this.processCode = processCode;
  }

  public void setInitUserId(int initUserId) {
    this.initUserId = initUserId;
  }

  public void setInitObjectKey(String initObjectKey) {
    this.initObjectKey = initObjectKey;
  }

  public void setInitObjectId(long initObjectId) {
    this.initObjectId = initObjectId;
  }

  public void setUseLatest(boolean useLatest) {
    this.useLatest = useLatest;
  }
}
