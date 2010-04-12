package org.jkiss.dbeaver.ext.mysql.model;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.anno.Property;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.meta.AbstractTable;
import org.jkiss.dbeaver.model.impl.meta.AbstractConstraint;
import org.jkiss.dbeaver.model.struct.*;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GenericTable
 */
public class MySQLTable extends AbstractTable<MySQLDataSource, MySQLCatalog>
{
    static Log log = LogFactory.getLog(MySQLTable.class);

    private MySQLEngine engine;
    private boolean isView;
    private boolean isSystem;

    private List<MySQLTableColumn> columns;
    private List<MySQLIndex> indexes;
    private List<MySQLConstraint> constraints;
    private List<MySQLForeignKey> foreignKeys;

    private long rowCount;
    private long autoIncrement;

    public MySQLTable(
        MySQLCatalog catalog,
        ResultSet dbResult)
    {
        super(catalog);
        this.loadInfo(dbResult);
    }

    public String getFullQualifiedName()
    {
        return getDataSource().getFullTableName(
            getContainer().getName(),
            null,
            getName());
    }

    @Property(name = "Engine", viewable = true, order = 3)
    public MySQLEngine getEngine()
    {
        return engine;
    }

    public boolean isView()
    {
        return this.isView;
    }

    public boolean isSystem()
    {
        return this.isSystem;
    }

    public List<MySQLTableColumn> getColumns()
        throws DBException
    {
        if (columns == null) {
            this.columns = loadColumns();
        }
        return columns;
    }

    public MySQLTableColumn getColumn(String columnName)
        throws DBException
    {
        return DBSUtils.findObject(getColumns(), columnName);
    }

    public List<MySQLIndex> getIndexes()
        throws DBException
    {
        if (indexes == null) {
            indexes = loadIndexes();
        }
        return indexes;
    }

    public MySQLIndex getIndex(String indexName)
        throws DBException
    {
        return DBSUtils.findObject(getIndexes(), indexName);
    }

    public List<? extends AbstractConstraint<MySQLDataSource, MySQLTable>> getConstraints()
        throws DBException
    {
        if (constraints == null) {
            constraints = loadConstraints();
        }
        return constraints;
    }

    public List<MySQLForeignKey> getExportedKeys()
        throws DBException
    {
        return loadForeignKeys(true);
    }

    public List<MySQLForeignKey> getImportedKeys()
        throws DBException
    {
        if (foreignKeys == null) {
            foreignKeys = loadForeignKeys(false);
        }
        return foreignKeys;
    }

    public boolean refreshObject()
        throws DBException
    {
        return false;
    }

    @Property(name = "Row Count", viewable = true, order = 5)
    public long getRowCount()
    {
        return rowCount;
    }

    void setRowCount(long rowCount)
    {
        this.rowCount = rowCount;
    }

    @Property(name = "Auto Increment", viewable = true, order = 6)
    public long getAutoIncrement()
    {
        return autoIncrement;
    }

    public void setAutoIncrement(long autoIncrement)
    {
        this.autoIncrement = autoIncrement;
    }

    public String getDDL()
        throws DBException
    {
        try {
            PreparedStatement dbStat = getDataSource().getConnection().prepareStatement(
                "SHOW CREATE TABLE " + getFullQualifiedName());
            try {
                ResultSet dbResult = dbStat.executeQuery();
                try {
                    if (dbResult.next()) {
                        return dbResult.getString("Create Table");
                    } else {
                        return "DDL is not available";
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
    }

    private void loadInfo(ResultSet dbResult)
    {
        this.setName(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TABLE_NAME));
        this.setTableType(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TABLE_TYPE));
        this.setDescription(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TABLE_COMMENT));
        this.engine = MySQLEngine.getByName(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ENGINE));
        this.rowCount = JDBCUtils.safeGetLong(dbResult, MySQLConstants.COL_TABLE_ROWS);
        this.autoIncrement = JDBCUtils.safeGetLong(dbResult, MySQLConstants.COL_AUTO_INCREMENT);

        if (!CommonUtils.isEmpty(this.getTableType())) {
            this.isView = (this.getTableType().toUpperCase().indexOf("VIEW") != -1);
            this.isSystem = (this.getTableType().toUpperCase().indexOf("SYSTEM") != -1);
        }
        this.columns = null;
    }

    private List<MySQLTableColumn> loadColumns()
        throws DBException
    {
        try {
            String catalogName = getContainer().getName();
            List<MySQLTableColumn> tmpColumns = new ArrayList<MySQLTableColumn>();

            PreparedStatement dbStat = getDataSource().getConnection().prepareStatement(
                MySQLConstants.QUERY_SELECT_TABLE_COLUMNS);
            try {
                dbStat.setString(1, catalogName);
                dbStat.setString(2, getName());
                ResultSet dbResult = dbStat.executeQuery();
                try {
                    while (dbResult.next()) {
                        MySQLTableColumn tableColumn = new MySQLTableColumn(this, dbResult);
                        tmpColumns.add(tableColumn);
                    }
                }
                finally {
                    dbResult.close();
                }
            }
            finally {
                dbStat.close();
            }

            return tmpColumns;
        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }
    }

    private List<MySQLIndex> loadIndexes()
        throws DBException
    {
        // Load columns first
        getColumns();
        // Load index columns
        try {
            List<MySQLIndex> tmpIndexList = new ArrayList<MySQLIndex>();
            Map<String, MySQLIndex> tmpIndexMap = new HashMap<String, MySQLIndex>();
            DatabaseMetaData metaData = getDataSource().getConnection().getMetaData();
            // Load indexes
            ResultSet dbResult = metaData.getIndexInfo(
                getContainer().getName(),
                null,
                getName(),
                false,
                false);
            try {
                while (dbResult.next()) {
                    String indexName = JDBCUtils.safeGetString(dbResult, JDBCConstants.INDEX_NAME);
                    boolean isNonUnique = JDBCUtils.safeGetBoolean(dbResult, JDBCConstants.NON_UNIQUE);
                    String indexQualifier = JDBCUtils.safeGetString(dbResult, JDBCConstants.INDEX_QUALIFIER);
                    int indexTypeNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.TYPE);

                    int ordinalPosition = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);
                    String columnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_NAME);
                    String ascOrDesc = JDBCUtils.safeGetString(dbResult, JDBCConstants.ASC_OR_DESC);

                    if (CommonUtils.isEmpty(indexName)) {
                        // Bad index - can't evaluate it
                        continue;
                    }
                    DBSIndexType indexType;
                    switch (indexTypeNum) {
                        case DatabaseMetaData.tableIndexStatistic: indexType = DBSIndexType.STATISTIC; break;
                        case DatabaseMetaData.tableIndexClustered: indexType = DBSIndexType.CLUSTERED; break;
                        case DatabaseMetaData.tableIndexHashed: indexType = DBSIndexType.HASHED; break;
                        case DatabaseMetaData.tableIndexOther: indexType = DBSIndexType.OTHER; break;
                        default: indexType = DBSIndexType.UNKNOWN; break;
                    }
                    MySQLIndex index = tmpIndexMap.get(indexName);
                    if (index == null) {
                        index = new MySQLIndex(
                            this,
                            isNonUnique,
                            indexQualifier,
                            indexName,
                            indexType);
                        tmpIndexList.add(index);
                        tmpIndexMap.put(indexName, index);
                    }
                    MySQLTableColumn tableColumn = this.getColumn(columnName);
                    if (tableColumn == null) {
                        log.warn("Column '" + columnName + "' not found in table '" + this.getName() + "'");
                        continue;
                    }
                    index.addColumn(
                        new MySQLIndexColumn(
                            index,
                            tableColumn,
                            ordinalPosition,
                            !"D".equalsIgnoreCase(ascOrDesc)));
                }
            }
            finally {
                dbResult.close();
            }
            return tmpIndexList;
        } catch (SQLException ex) {
            throw new DBException(ex);
        }
    }

    private List<MySQLConstraint> loadConstraints()
        throws DBException
    {
        try {
            List<MySQLConstraint> pkList = new ArrayList<MySQLConstraint>();
            Map<String, MySQLConstraint> pkMap = new HashMap<String, MySQLConstraint>();
            DatabaseMetaData metaData = getDataSource().getConnection().getMetaData();
            // Load indexes
            ResultSet dbResult = metaData.getPrimaryKeys(
                getContainer().getName(),
                null,
                getName());
            try {
                while (dbResult.next()) {
                    String columnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_NAME);
                    int keySeq = JDBCUtils.safeGetInt(dbResult, JDBCConstants.KEY_SEQ);
                    String pkName = JDBCUtils.safeGetString(dbResult, JDBCConstants.PK_NAME);
                    MySQLConstraint pk = pkMap.get(pkName);
                    if (pk == null) {
                        pk = new MySQLConstraint(
                            DBSConstraintType.PRIMARY_KEY,
                            this,
                            pkName,
                            null);
                        pkList.add(pk);
                        pkMap.put(pkName, pk);
                    }
                    if (CommonUtils.isEmpty(columnName)) {
                        // Bad index - can't evaluate it
                        continue;
                    }
                    MySQLTableColumn tableColumn = this.getColumn(columnName);
                    if (tableColumn == null) {
                        log.warn("Column '" + columnName + "' not found in table '" + this.getName() + "' for PK");
                        continue;
                    }
                    pk.addColumn(
                        new MySQLConstraintColumn(
                            pk,
                            tableColumn,
                            keySeq));
                }
            }
            finally {
                dbResult.close();
            }
            return pkList;
        } catch (SQLException ex) {
            throw new DBException(ex);
        }
    }

    private List<MySQLForeignKey> loadForeignKeys(boolean exported)
        throws DBException
    {
        try {
            List<MySQLForeignKey> fkList = new ArrayList<MySQLForeignKey>();
            Map<String, MySQLForeignKey> fkMap = new HashMap<String, MySQLForeignKey>();
            Map<String, MySQLConstraint> pkMap = new HashMap<String, MySQLConstraint>();
            DatabaseMetaData metaData = getDataSource().getConnection().getMetaData();
            // Load indexes
            ResultSet dbResult;
            if (exported) {
                dbResult = metaData.getExportedKeys(
                    getContainer().getName(),
                    null,
                    getName());
            } else {
                dbResult = metaData.getImportedKeys(
                    getContainer().getName(),
                    null,
                    getName());
            }
            try {
                while (dbResult.next()) {
                    String pkTableCatalog = JDBCUtils.safeGetString(dbResult, JDBCConstants.PKTABLE_CAT);
                    String pkTableSchema = JDBCUtils.safeGetString(dbResult, JDBCConstants.PKTABLE_SCHEM);
                    String pkTableName = JDBCUtils.safeGetString(dbResult, JDBCConstants.PKTABLE_NAME);
                    String pkColumnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.PKCOLUMN_NAME);
                    String fkTableCatalog = JDBCUtils.safeGetString(dbResult, JDBCConstants.FKTABLE_CAT);
                    String fkTableSchema = JDBCUtils.safeGetString(dbResult, JDBCConstants.FKTABLE_SCHEM);
                    String fkTableName = JDBCUtils.safeGetString(dbResult, JDBCConstants.FKTABLE_NAME);
                    String fkColumnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.FKCOLUMN_NAME);
                    int keySeq = JDBCUtils.safeGetInt(dbResult, JDBCConstants.KEY_SEQ);
                    int updateRuleNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.UPDATE_RULE);
                    int deleteRuleNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DELETE_RULE);
                    String fkName = JDBCUtils.safeGetString(dbResult, JDBCConstants.FK_NAME);
                    String pkName = JDBCUtils.safeGetString(dbResult, JDBCConstants.PK_NAME);
                    int defferabilityNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DEFERRABILITY);

                    DBSConstraintCascade deleteRule = getCascadeFromNum(deleteRuleNum);
                    DBSConstraintCascade updateRule = getCascadeFromNum(updateRuleNum);
                    DBSConstraintDefferability defferability;
                    switch (defferabilityNum) {
                        case DatabaseMetaData.importedKeyInitiallyDeferred: defferability = DBSConstraintDefferability.INITIALLY_DEFERRED; break;
                        case DatabaseMetaData.importedKeyInitiallyImmediate: defferability = DBSConstraintDefferability.INITIALLY_IMMEDIATE; break;
                        case DatabaseMetaData.importedKeyNotDeferrable: defferability = DBSConstraintDefferability.NOT_DEFERRABLE; break;
                        default: defferability = DBSConstraintDefferability.UNKNOWN; break;
                    }

                    String pkTableFullName = getDataSource().getFullTableName(pkTableCatalog, pkTableSchema, pkTableName);
                    MySQLTable pkTable = getDataSource().findTable(pkTableCatalog, pkTableName);
                    if (pkTable == null) {
                        log.warn("Can't find PK table " + pkTableFullName);
                        continue;
                    }
                    String fkTableFullName = getDataSource().getFullTableName(fkTableCatalog, fkTableSchema, fkTableName);
                    MySQLTable fkTable = getDataSource().findTable(fkTableCatalog, fkTableName);
                    if (fkTable == null) {
                        log.warn("Can't find FK table " + fkTableFullName);
                        continue;
                    }
                    MySQLTableColumn pkColumn = pkTable.getColumn(pkColumnName);
                    if (pkColumn == null) {
                        log.warn("Can't find PK table " + getDataSource().getFullTableName(pkTableCatalog, pkTableSchema, pkTableName) + " column " + pkColumnName);
                        continue;
                    }
                    MySQLTableColumn fkColumn = fkTable.getColumn(fkColumnName);
                    if (fkColumn == null) {
                        log.warn("Can't find FK table " + getDataSource().getFullTableName(fkTableCatalog, fkTableSchema, fkTableName) + " column " + fkColumnName);
                        continue;
                    }

                    String pkFullName = pkTableFullName + "." + pkName;
                    MySQLConstraint pk = pkMap.get(pkFullName);
                    if (pk == null) {
                        pk = new MySQLConstraint(DBSConstraintType.PRIMARY_KEY, pkTable, pkName, null);
                        pkMap.put(pkFullName, pk);
                    }
                    MySQLForeignKey fk = fkMap.get(fkName);
                    if (fk == null) {
                        fk = new MySQLForeignKey(fkTable, fkName, null, pk, deleteRule, updateRule, defferability);
                        fkMap.put(fkName, fk);
                        fkList.add(fk);
                    }
                    MySQLForeignKeyColumn fkColumnInfo = new MySQLForeignKeyColumn(fk, fkColumn, keySeq, pkColumn);
                    fk.addColumn(fkColumnInfo);
                }
            }
            finally {
                dbResult.close();
            }
            return fkList;
        } catch (SQLException ex) {
            throw new DBException(ex);
        }
    }

    private static DBSConstraintCascade getCascadeFromNum(int num)
    {
        switch (num) {
            case DatabaseMetaData.importedKeyNoAction: return DBSConstraintCascade.NO_ACTION;
            case DatabaseMetaData.importedKeyCascade: return DBSConstraintCascade.CASCADE;
            case DatabaseMetaData.importedKeySetNull: return DBSConstraintCascade.SET_NULL;
            case DatabaseMetaData.importedKeySetDefault: return DBSConstraintCascade.SET_DEFAULT;
            case DatabaseMetaData.importedKeyRestrict: return DBSConstraintCascade.RESTRICT;
            default: return DBSConstraintCascade.UNKNOWN;
        }
    }

    public void cacheStructure()
        throws DBException
    {
        getColumns();
    }

}
