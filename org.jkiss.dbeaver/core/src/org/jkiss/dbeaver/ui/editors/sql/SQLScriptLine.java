package org.jkiss.dbeaver.ui.editors.sql;

/**
 * SQLScriptLine
 */
public class SQLScriptLine {

    private String query;
    private int offset;
    private int length;

    public SQLScriptLine(String query, int offset, int length)
    {
        this.query = query;
        this.offset = offset;
        this.length = length;
    }

    public String getQuery()
    {
        return query;
    }

    public int getOffset()
    {
        return offset;
    }

    public int getLength()
    {
        return length;
    }
}
