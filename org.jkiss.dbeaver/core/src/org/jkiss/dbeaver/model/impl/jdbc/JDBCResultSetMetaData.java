package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.dbc.DBCResultSetMetaData;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSStructureContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSUtils;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.DBException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.ResultSetMetaData;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import net.sf.jkiss.utils.CommonUtils;

/**
 * JDBCResultSetMetaData
 */
public class JDBCResultSetMetaData implements DBCResultSetMetaData
{
    private JDBCResultSet resultSet;
    private ResultSetMetaData jdbcMetaData;
    private List<DBCColumnMetaData> columns = new ArrayList<DBCColumnMetaData>();
    private Map<DBSObject, JDBCTableMetaData> tables = new HashMap<DBSObject, JDBCTableMetaData>();

    JDBCResultSetMetaData(JDBCResultSet resultSet)
        throws SQLException
    {
        this.resultSet = resultSet;
        this.jdbcMetaData = resultSet.getResultSet().getMetaData();
        int count = jdbcMetaData.getColumnCount();
        for (int i = 1; i <= count; i++) {
            columns.add(new JDBCColumnMetaData(this, i));
        }
    }

    public JDBCResultSet getResultSet()
    {
        return resultSet;
    }

    ResultSetMetaData getJdbcMetaData()
    {
        return jdbcMetaData;
    }

    public List<DBCColumnMetaData> getColumns()
    {
        return columns;
    }

    public JDBCTableMetaData getTableMetaData(String catalogName, String schemaName, String tableName)
        throws DBException
    {
        if (CommonUtils.isEmpty(tableName)) {
            // some constant instead of table name
            return null;
        }
        DBPDataSource dataSource = resultSet.getStatement().getSession().getDataSource();
        if (dataSource instanceof DBSStructureContainer) {
            DBSObject tableObject = DBSUtils.getObjectByPath((DBSStructureContainer) dataSource, catalogName, schemaName, tableName);
            if (tableObject instanceof DBSTable) {
                JDBCTableMetaData tableMetaData = tables.get(tableObject);
                if (tableMetaData == null) {
                    tableMetaData = new JDBCTableMetaData(this, (DBSTable)tableObject, null);
                    tables.put(tableObject, tableMetaData);
                }
                return tableMetaData;
            }
        }
        return null;
    }
}
