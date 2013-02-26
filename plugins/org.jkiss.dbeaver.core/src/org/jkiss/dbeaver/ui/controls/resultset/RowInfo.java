package org.jkiss.dbeaver.ui.controls.resultset;

/**
* Row information
*/
class RowInfo implements Comparable<RowInfo> {
    int row;

    RowInfo(int row)
    {
        this.row = row;
    }
    @Override
    public int hashCode()
    {
        return row;
    }
    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof RowInfo && ((RowInfo)obj).row == row;
    }
    @Override
    public String toString()
    {
        return String.valueOf(row);
    }
    @Override
    public int compareTo(RowInfo o)
    {
        return row - o.row;
    }
}
