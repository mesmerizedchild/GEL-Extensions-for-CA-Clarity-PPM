package eu.mesmerizedChild.commons.jelly.sql;

import java.util.SortedMap;

import javax.servlet.jsp.jstl.sql.Result;

import org.apache.commons.jelly.JellyContext;
import org.apache.commons.jelly.JellyTagException;

class ResultInto {

  public static final String    ALL_COLUMNS = "*";

  private final Result          result;
  private final SortedMap<?, ?> values[];
  private final int             numberOfVars;
  private final String          varNames[];
  private final int             positions[];

  ResultInto(Result result, String into) throws JellyTagException {
    this.result = result;
    this.values = result.getRows();

    String colNames[];
    int numCols = (colNames = result.getColumnNames()).length;

    String items[];
    if (ALL_COLUMNS.equals(into)) {
      this.numberOfVars = numCols;
      int positions[] = this.positions = new int[numCols]; // All set to false
      String varNames[] = this.varNames = new String[numCols];
      for (int i = numCols - 1; i >= 0; i--) {
        varNames[i] = colNames[i].toLowerCase();
        positions[i] = i;
      }
    }
    else {
      items = into.split(",");
      int l = items.length;
      this.numberOfVars = l;
      int positions[] = this.positions = new int[l]; // All set to false
      String varNames[] = this.varNames = new String[l];
      for (int i = l - 1; i >= 0; i--) {
        String item = items[i];
        int equals = item.indexOf('=');
        if (equals == -1) { // Name alone was provided
          if (i >= numCols)
            throw new JellyTagException("Not enough columns from the query in order to satisfy all variables listed in 'into'; cannot read " + item + ".");
          varNames[i] = item;
          positions[i] = i;
        }
        else { // variableName=columnName
          String varName = item.substring(0, equals);
          String colName = item.substring(equals + 1);
          boolean found = false;
          for (int k = numCols - 1; k >= 0; k--)
            if (colNames[k].equalsIgnoreCase(colName)) {
              varNames[i] = varName;
              positions[i] = k;
              found = true;
              break;
            }
          if (!found)
            throw new JellyTagException("The query did not return any columns named " + colName + ".");
        }
      }
    }
  }

  void doInto(int rowNumber, JellyContext context) {

    Object[] lazyArray = null;
    boolean isEmpty = rowNumber == -1;

    for (int i = numberOfVars - 1; i >= 0; i--)
      if (isEmpty)
        context.setVariable(varNames[i], null);
      else {
        if (lazyArray == null)
          // Get the row
          lazyArray = result.getRowsByIndex()[rowNumber];
        context.setVariable(varNames[i], lazyArray[positions[i]]);
      }
  }

  SortedMap<?, ?> getValue(int rowNumber) {
    return values[rowNumber];
  }
}
