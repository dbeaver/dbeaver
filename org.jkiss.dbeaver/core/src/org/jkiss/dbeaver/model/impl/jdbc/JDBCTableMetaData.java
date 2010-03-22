package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.dbeaver.model.dbc.DBCTableMetaData;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.model.struct.DBSStructureContainer;
import org.jkiss.dbeaver.model.struct.DBSConstraint;
import org.jkiss.dbeaver.model.DBPDataSource;

import java.util.List;
import java.util.ArrayList;

/**
 * JDBC Table MetaData
 */
public class JDBCTableMetaData implements DBCTableMetaData {

    private JDBCResultSetMetaData resultSetMetaData;
    private DBSTable<DBPDataSource, DBSStructureContainer<DBPDataSource>> table;
    private String alias;
    private List<JDBCColumnMetaData> columns = new ArrayList<JDBCColumnMetaData>();
    private List<DBSConstraint> identifiers;

    public JDBCTableMetaData(JDBCResultSetMetaData resultSetMetaData, DBSTable table, String alias)
    {
        this.resultSetMetaData = resultSetMetaData;
        this.table = table;
        this.alias = alias;
    }

    public JDBCResultSetMetaData getResultSetMetaData()
    {
        return resultSetMetaData;
    }

    public DBSTable<DBPDataSource, DBSStructureContainer<DBPDataSource>> getTable()
    {
        return table;
    }

    public String getTableName()
    {
        return table.getName();
    }

    public String getTableAlias()
    {
        return alias;
    }

    public boolean isIdentitied()
    {
        return getBestIdentifier() != null;
    }

    public List<DBCColumnMetaData> getBestIdentifier()
    {
        return null;
    }

    public List<JDBCColumnMetaData> getColumns()
    {
        return columns;
    }

    public void addColumn(JDBCColumnMetaData columnMetaData)
    {
        columns.add(columnMetaData);
    }

}
