package org.jkiss.dbeaver.ext.yashandb.model;



import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.util.List;


public class YashanDBTableSortKey extends YashanDBObject<YashanDBTableBase> {

    private String owner;
    private String table;
    private String column;
    private Integer position;

    private List<YashanDBTableSortKeyColumn> columns;

    protected YashanDBTableSortKey(YashanDBTableBase yashanDBTableBase, String name, long objectId, boolean persisted) {
        super(yashanDBTableBase, name, objectId, persisted);
    }

    protected YashanDBTableSortKey(YashanDBTableBase yashanDBTableBase, String name, boolean persisted) {
        super(yashanDBTableBase, name, persisted);
    }

    public YashanDBTableSortKey(YashanDBTableBase yashanDBTable, JDBCResultSet resultSet) {
        super(yashanDBTable, JDBCUtils.safeGetString(resultSet, "COLUMN_NAME"), true);
        owner = JDBCUtils.safeGetString(resultSet, "OWNER");
        table = JDBCUtils.safeGetString(resultSet, "TABLE_NAME");
        column = JDBCUtils.safeGetString(resultSet, "COLUMN_NAME");
        position = JDBCUtils.safeGetInteger(resultSet, "COLUMN_POSITION");
    }


    @Property(viewable = true, editable = false, order = 1)
    public String getColumn() {
        return column;
    }

    @Property(viewable = true, editable = false, order = 2)
    public String getOwner() {
        return owner;
    }

    @Property(viewable = true, editable = false, order = 3)
    public String getTable() {
        return table;
    }

    @Property(viewable = true, editable = false, order = 4)
    public Integer getPosition() {
        return position;
    }

    @Override
    public String getName() {
        return super.getName();
    }

    public List<YashanDBTableSortKeyColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<YashanDBTableSortKeyColumn> children) {
        columns = children;
    }
}
