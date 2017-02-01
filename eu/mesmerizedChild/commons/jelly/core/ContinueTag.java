package eu.mesmerizedChild.commons.jelly.core;

import org.apache.commons.jelly.TagSupport;
import org.apache.commons.jelly.XMLOutput;
import org.apache.commons.jelly.expression.Expression;

public class ContinueTag extends TagSupport {

  private Expression test;
  private String     var;

  public ContinueTag() {
    //
  }

  @Override
  public void doTag(XMLOutput output) throws ContinueException {
    boolean doContinue = false;
    if (this.test == null || this.test.evaluateAsBoolean(this.context))
      doContinue = true;
    if (this.var != null)
      this.context.setVariable(this.var, String.valueOf(doContinue));
    if (doContinue)
      throw new ContinueException();
  }

  public void setTest(Expression test) {
    this.test = test;
  }

  public void setVar(String var) {
    this.var = var;
  }
}
