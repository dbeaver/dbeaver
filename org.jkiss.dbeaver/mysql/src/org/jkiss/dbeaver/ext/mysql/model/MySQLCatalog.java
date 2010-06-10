package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.anno.Property;
import org.jkiss.dbeaver.model.impl.meta.AbstractCatalog;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.dbeaver.model.struct.DBSTablePath;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * GenericCatalog
 */
public class MySQLCatalog
    extends AbstractCatalog<MySQLDataSource>
    implements DBSStructureAssistant
{
    private String defaultCharset;
    private String defaultCollation;
    private String sqlPath;
    private List<MySQLTable> tableList;
    private Map<String, MySQLTable> tableMap;
    private List<MySQLProcedure> procedures;

    public MySQLCatalog(MySQLDataSource dataSource, String catalogName)
    {
        super(dataSource, catalogName);
    }

    public String getDescription()
    {
        return null;
    }

    @Property(name = "Default Charset", viewable = true, order = 2)
    public String getDefaultCharset()
    {
        return defaultCharset;
    }

    void setDefaultCharset(String defaultCharset)
    {
        this.defaultCharset = defaultCharset;
    }

    public String getDefaultCollation()
    {
        return defaultCollation;
    }

    void setDefaultCollation(String defaultCollation)
    {
        this.defaultCollation = defaultCollation;
    }

    public String getSqlPath()
    {
        return sqlPath;
    }

    void setSqlPath(String sqlPath)
    {
        this.sqlPath = sqlPath;
    }

    public List<MySQLTable> getTableList()
    {
        return tableList;
    }

    public void setTableList(List<MySQLTable> tableList)
    {
        this.tableList = tableList;
    }

    public Map<String, MySQLTable> getTableMap()
    {
        return tableMap;
    }

    public void setTableMap(Map<String, MySQLTable> tableMap)
    {
        this.tableMap = tableMap;
    }

    public List<MySQLIndex> getIndexes()
        throws DBException
    {
        // Copy indexes from tables because we do not want
        // to place the same objects in different places of the tree model
        List<MySQLIndex> indexList = new ArrayList<MySQLIndex>();
        for (MySQLTable table : getTables()) {
            for (MySQLIndex index : table.getIndexes()) {
                indexList.add(new MySQLIndex(index));
            }
        }
        return indexList;
    }

    public List<MySQLTable> getTables()
        throws DBException
    {
        if (tableList == null) {
            loadTables();
        }
        return tableList;
    }

    public MySQLTable getTable(String name)
        throws DBException
    {
        if (tableMap == null) {
            loadTables();
        }
        return tableMap.get(name);
    }

    public List<MySQLProcedure> getProcedures()
        throws DBException
    {
        if (procedures == null) {
            loadProcedures();
        }
        return procedures;
    }

    private void loadTables()
        throws DBException
    {
        List<MySQLTable> tmpTableList = new ArrayList<MySQLTable>();
        Map<String, MySQLTable> tmpTableMap = new HashMap<String, MySQLTable>();
        try {
            PreparedStatement dbStat = getDataSource().getConnection().prepareStatement(MySQLConstants.QUERY_SELECT_TABLES);
            try {
                dbStat.setString(1, getName());
                ResultSet dbResult = dbStat.executeQuery();
                try {
                    while (dbResult.next()) {
                        MySQLTable table = new MySQLTable(this, dbResult);
                        tmpTableList.add(table);
                        tmpTableMap.put(table.getName(), table);
                    }
                }
                finally {
                    dbResult.close();
                }
            }
            finally {
                dbStat.close();
            }
        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }
        this.tableList = tmpTableList;
        this.tableMap = tmpTableMap;
    }

    private void loadProcedures()
        throws DBException
    {
        List<MySQLProcedure> tmpProcedureList = new ArrayList<MySQLProcedure>();

        try {
            PreparedStatement dbStat = getDataSource().getConnection().prepareStatement(MySQLConstants.QUERY_SELECT_ROUTINES);
            try {
                dbStat.setString(1, getName());
                ResultSet dbResult = dbStat.executeQuery();
                try {
                    while (dbResult.next()) {
                        MySQLProcedure procedure = new MySQLProcedure(this, dbResult);
                        tmpProcedureList.add(procedure);
                    }
                }
                finally {
                    dbResult.close();
                }
            }
            finally {
                dbStat.close();
            }
        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }
        this.procedures = tmpProcedureList;
    }

    public List<DBSTablePath> findTableNames(String tableMask, int maxResults) throws DBException
    {
        return getDataSource().findTableNames(tableMask, maxResults);
    }

    public Collection<MySQLTable> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        return getTables();
    }

    public MySQLTable getChild(DBRProgressMonitor monitor, String childName)
        throws DBException
    {
        return getTable(childName);
    }

    public void cacheStructure(DBRProgressMonitor monitor)
        throws DBException
    {

    }

}
