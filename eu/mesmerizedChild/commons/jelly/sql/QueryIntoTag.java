package eu.mesmerizedChild.commons.jelly.sql;

import javax.servlet.jsp.jstl.sql.Result;

import org.apache.commons.jelly.JellyContext;
import org.apache.commons.jelly.JellyTagException;
import org.apache.commons.jelly.MissingAttributeException;
import org.apache.commons.jelly.XMLOutput;
import org.apache.commons.jelly.tags.sql.QueryTag;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Extension on the {@link QueryTag query tag}.<br>
 * Writes the data from the first row into a list of variables, specified via the <code>into</code> attribute.<br>
 * When <code>into</code> contains the special value <code>"column_names"</code> [case sensitive!] then variables will be created with the same name as the columns returned by the query, after having the column names
 * converted to all-lowercase.<br>
 * <br>
 * If not data is returned from the query, then an exception is thrown, unless attribute <code>nullIfEmpty</code> is set to <code>true</code>.<br>
 * <br>
 * As this query extends the {@link QueryTag query tag}, the <code>var</code> attribute may still be used, as well as the <code>&lt;sql:param/></code> tags.<br>
 *
 * @author Roberto Giuntoli
 */

public class QueryIntoTag extends QueryTag {

  private static final Log log         = LogFactory.getLog(QueryIntoTag.class);
  protected String         into;
  protected boolean        nullIfEmpty = false;

  public QueryIntoTag() {
    //
  }

  public void setInto(String into) {
    this.into = into;
  }

  public void setNullIfEmpty(boolean nullIfEmpty) {
    this.nullIfEmpty = nullIfEmpty;
  }

  @Override
  public void doTag(XMLOutput output) throws MissingAttributeException, JellyTagException {
    String into = this.into;
    if (into == null || "".equals(into))
      throw new MissingAttributeException("Parameter 'into' must be specified.");

    // If the var attribute is not set, I must build my own for super.doTag() to use,
    // and then destroy it at the end.
    boolean myOwnVar;
    if (this.var == null) {
      myOwnVar = true;
      String var;
      do
        var = "v" + Integer.toString((int) (Math.random() * 0xFFFFFF), 16);
      while (context.getVariable(var) != null); // Unlikely as name conflicts may be, I make sure to avoid them
      super.setVar(var);
    }
    else
      myOwnVar = false;

    super.doTag(output);

    try {
      JellyContext context = getContext();

      Result result = (Result) context.getVariable(this.var);
      log.debug("rowCount: " + result.getRowCount());
      log.debug("columnNames: " + result.getColumnNames());

      boolean isEmpty;
      if ((isEmpty = result.getRowCount() < 1) && !nullIfEmpty)
        throw new JellyTagException("No data was retrieved from the database.");

      ResultInto ri = new ResultInto(result, into);
      ri.doInto(isEmpty ? -1 : 0, getContext());
      if (myOwnVar)
        context.setVariable(var, null);
    }
    catch (JellyTagException j) {
      throw j; // Just re-throw the exception
    }
    catch (Throwable t) {
      throw new JellyTagException(t); // Wrap the exception in a JellyTagException
    }
  }
}
