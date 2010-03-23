package org.jkiss.dbeaver.ext.generic.model;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * GenericStructureContainer
 */
public abstract class GenericStructureContainer implements DBSStructureContainer<GenericDataSource>, DBSStructureAssistant
{
    static Log log = LogFactory.getLog(GenericStructureContainer.class);

    private List<GenericTable> tableList;
    private Map<String, GenericTable> tableMap;
    private List<GenericProcedure> procedures;

    protected GenericStructureContainer()
    {
    }

    public abstract GenericCatalog getCatalog();

    public abstract GenericSchema getSchema();

    public abstract DBSObject getObject();

    public List<GenericTable> getTables()
        throws DBException
    {
        if (tableList == null) {
            loadTables();
        }
        return tableList;
    }

    public GenericTable getTable(String name)
        throws DBException
    {
        if (tableMap == null) {
            loadTables();
        }
        return tableMap.get(name);
    }

    public List<GenericIndex> getIndexes()
        throws DBException
    {
        // Copy indexes from tables because we do not want
        // to place the same objects in different places of the tree model
        List<GenericIndex> indexList = new ArrayList<GenericIndex>();
        for (GenericTable table : getTables()) {
            for (GenericIndex index : table.getIndexes()) {
                indexList.add(new GenericIndex(index));
            }
        }
        return indexList;
    }

    public void cacheStructure()
    {
        System.out.println("CACHE STRUCTURE!");
    }

    public List<GenericProcedure> getProcedures()
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
        List<GenericTable> tmpTableList = new ArrayList<GenericTable>();
        Map<String, GenericTable> tmpTableMap = new HashMap<String, GenericTable>();
        try {
            DatabaseMetaData metaData = getDataSource().getConnection().getMetaData();
            String catalogName = getCatalog() == null ? null : getCatalog().getName();
            String schemaName = getSchema() == null ? null : getSchema().getName();

            // Load tables
            ResultSet dbResult = metaData.getTables(
                catalogName,
                schemaName,
                null,
                null);//getDataSource().getTableTypes());
            try {
                while (dbResult.next()) {

                    String tableName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_NAME);
                    String tableType = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_TYPE);
                    String remarks = JDBCUtils.safeGetString(dbResult, JDBCConstants.REMARKS);

                    boolean isSystemTable = tableType != null && tableType.toUpperCase().indexOf("SYSTEM") != -1;
                    if (isSystemTable && !getDataSource().getContainer().isShowSystemObjects()) {
                        continue;
                    }
                    String typeName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_NAME);
                    String typeCatalogName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_CAT);
                    String typeSchemaName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_SCHEM);
                    GenericCatalog typeCatalog = CommonUtils.isEmpty(typeCatalogName) ?
                        null :
                        getDataSource().getCatalog(typeCatalogName);
                    GenericSchema typeSchema = CommonUtils.isEmpty(typeSchemaName) ?
                        null :
                        typeCatalog == null ?
                            getDataSource().getSchema(typeSchemaName) :
                            typeCatalog.getSchema(typeSchemaName);
                    GenericTable table = new GenericTable(
                        this,
                        tableName,
                        tableType,
                        remarks,
                        typeName,
                        typeCatalog,
                        typeSchema);
                    tmpTableList.add(table);
                    tmpTableMap.put(tableName, table);
                }
            }
            finally {
                dbResult.close();
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
        List<GenericProcedure> tmpProcedureList = new ArrayList<GenericProcedure>();

        try {
            DatabaseMetaData metaData = getDataSource().getConnection().getMetaData();
            String catalogName = getCatalog() == null ? null : getCatalog().getName();
            String schemaName = getSchema() == null ? null : getSchema().getName();

            // Load procedures
            ResultSet dbResult = metaData.getProcedures(
                catalogName,
                schemaName,
                null);
            try {
                while (dbResult.next()) {
                    String procedureName = JDBCUtils.safeGetString(dbResult, JDBCConstants.PROCEDURE_NAME);
                    int procTypeNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.PROCEDURE_TYPE);
                    String remarks = JDBCUtils.safeGetString(dbResult, JDBCConstants.REMARKS);
                    DBSProcedureType procedureType;
                    switch (procTypeNum) {
                        case DatabaseMetaData.procedureNoResult: procedureType = DBSProcedureType.PROCEDURE; break;
                        case DatabaseMetaData.procedureReturnsResult: procedureType = DBSProcedureType.FUNCTION; break;
                        case DatabaseMetaData.procedureResultUnknown: procedureType = DBSProcedureType.PROCEDURE; break;
                        default: procedureType = DBSProcedureType.UNKNOWN; break;
                    }
                    GenericProcedure procedure = new GenericProcedure(
                        this,
                        procedureName,
                        remarks, procedureType
                    );
                    tmpProcedureList.add(procedure);
                }
            }
            finally {
                dbResult.close();
            }

        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }
        this.procedures = tmpProcedureList;
    }

    public List<DBSTablePath> findTableNames(String tableMask, int maxResults) throws DBException
    {
        List<DBSTablePath> pathList = new ArrayList<DBSTablePath>();
        try {
            DatabaseMetaData metaData = getDataSource().getConnection().getMetaData();
            String catalogName = getCatalog() == null ? null : getCatalog().getName();
            String schemaName = getSchema() == null ? null : getSchema().getName();

            // Make table mask uppercase
            tableMask = tableMask.toUpperCase();

            // Load tables
            ResultSet dbResult = metaData.getTables(
                catalogName,
                schemaName,
                tableMask,
                null);
            try {
                int tableNum = maxResults;
                while (dbResult.next() && tableNum-- > 0) {

                    catalogName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_CAT);
                    schemaName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_SCHEM);
                    String tableName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_NAME);
                    String tableType = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_TYPE);
                    String remarks = JDBCUtils.safeGetString(dbResult, JDBCConstants.REMARKS);

                    pathList.add(
                        new DBSTablePath(
                            catalogName,
                            schemaName,
                            tableName,
                            tableType,
                            remarks));
                }
            }
            finally {
                dbResult.close();
            }
            return pathList;
        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }
    }

    public Collection<? extends DBSObject> getChildren()
        throws DBException
    {
        return getTables();
    }

    public DBSObject getChild(String childName)
        throws DBException
    {
        return DBSUtils.findObject(getTables(), childName);
    }

    public String toString()
    {
        return getName() == null ? "<NONE>" : getName();
    }
}
