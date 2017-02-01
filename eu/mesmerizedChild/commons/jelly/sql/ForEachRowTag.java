package eu.mesmerizedChild.commons.jelly.sql;

import javax.servlet.jsp.jstl.sql.Result;

import org.apache.commons.jelly.JellyContext;
import org.apache.commons.jelly.JellyTagException;
import org.apache.commons.jelly.MissingAttributeException;
import org.apache.commons.jelly.TagSupport;
import org.apache.commons.jelly.XMLOutput;
import org.apache.commons.jelly.expression.Expression;
import org.apache.commons.jelly.impl.BreakException;
import org.apache.commons.jelly.tags.core.ForEachTag.LoopStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import eu.mesmerizedChild.commons.jelly.core.ContinueException;

public class ForEachRowTag extends TagSupport {

  private static final Log log = LogFactory.getLog(ForEachRowTag.class);

  private String           into;
  private Expression       result;
  private String           statusVar;

  public ForEachRowTag() {
    //
  }

  /** Expected to evaluate to a {@link Result} instance. */
  public void setResult(Expression result) {
    this.result = result;
  }

  public void setInto(String into) {
    this.into = into;
  }

  public void setVarStatus(String statusVar) {
    this.statusVar = statusVar;
  }

  @Override
  public void doTag(XMLOutput output) throws MissingAttributeException, JellyTagException {
    try {
      JellyContext context = getContext();
      String into;
      if ((into = this.into) == null || result == null)
        throw new JellyTagException("This tag needs at least the into and query attributes.");
      Object o = result.evaluate(getContext());
      if (!(o instanceof Result))
        throw new JellyTagException("query must be a variable of type " + Result.class.getName());
      Result r = (Result) result.evaluate(getContext());

      int l = r.getRowCount();
      String statusVar = this.statusVar;
      LoopStatus status = null;
      if (statusVar != null)
        status = new LoopStatus(0, l - 1, 1);

      ResultInto ri = new ResultInto(r, into);
      for (int i = 0; i < l; i++) {
        if (statusVar != null) {
          status.setIndex(i);
          status.setCount(i + 1);
          status.setCurrent(ri.getValue(i));
          status.setFirst(i == 0);
          status.setLast(i == l - 1);
          context.setVariable(statusVar, status);
        }
        ri.doInto(i, context);
        try {
          invokeBody(output);
        }
        catch (ContinueException e) {
          log.debug("continuing to the next loop iteration: " + e, e);
          continue;
        }
        catch (BreakException e) {
          log.debug("loop terminated by break: " + e, e);
          break;
        }
      }
    }
    catch (JellyTagException j) {
      throw j; // Just re-throw the exception
    }
    catch (Throwable t) {
      throw new JellyTagException(t); // Wrap the exception in a JellyTagException
    }
  }
}
