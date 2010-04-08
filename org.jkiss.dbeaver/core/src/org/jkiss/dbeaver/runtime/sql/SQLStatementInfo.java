package org.jkiss.dbeaver.runtime.sql;

import java.util.List;

/**
 * SQLScriptLine
 */
public class SQLStatementInfo {

    private String query;
    private List<SQLStatementParameter> parameters;
    private int offset;
    private int length;
    private Object data;

    public SQLStatementInfo(String query)
    {
        this(query, null);
    }

    public SQLStatementInfo(String query, List<SQLStatementParameter> parameters) {
        this.query = query;
        this.parameters = parameters;
    }

    public String getQuery()
    {
        return query;
    }

    public List<SQLStatementParameter> getParameters() {
        return parameters;
    }

    public int getOffset()
    {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLength()
    {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    /**
     * User defined data object. May be used to identify statements.
     * @return data or null
     */
    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
