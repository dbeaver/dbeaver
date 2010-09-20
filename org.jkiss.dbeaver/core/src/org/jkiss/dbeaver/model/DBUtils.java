/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.data.DBDColumnBinding;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueLocator;
import org.jkiss.dbeaver.model.dbc.*;
import org.jkiss.dbeaver.model.impl.DBCDefaultValueHandler;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.DataTypeProviderDescriptor;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * DBUtils
 */
public final class DBUtils {

    static final Log log = LogFactory.getLog(DBUtils.class);

    public static String getQuotedIdentifier(DBPDataSource dataSource, String str)
    {
        String quoteString;
        quoteString = dataSource.getInfo().getIdentifierQuoteString();
        String delimString;
        delimString = dataSource.getInfo().getCatalogSeparator();

        boolean hasBadChars = false;
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isUnicodeIdentifierPart(str.charAt(i)) && delimString.indexOf(str.charAt(i)) == -1) {
                hasBadChars = true;
                break;
            }
        }

        if (!hasBadChars) {
            return str;
        }
        if (quoteString != null && quoteString.length() != 0 && !quoteString.equals(" ")) {
            str = quoteString + str + quoteString;
        }
        return str;
    }

    public static String getFullTableName(DBPDataSource dataSource, String catalogName, String schemaName,
                                          String tableName)
    {
        String catalogSeparator = dataSource.getInfo().getCatalogSeparator();
        StringBuilder name = new StringBuilder();
        if (!CommonUtils.isEmpty(catalogName)) {
            name.append(DBUtils.getQuotedIdentifier(dataSource, catalogName)).append(catalogSeparator);
        }
        if (!CommonUtils.isEmpty(schemaName)) {
            name.append(DBUtils.getQuotedIdentifier(dataSource, schemaName)).append(catalogSeparator);
        }
        name.append(DBUtils.getQuotedIdentifier(dataSource, tableName));
        return name.toString();
    }

    public static DBSObject getTableByPath(DBRProgressMonitor monitor, DBSEntityContainer rootSC, DBSTablePath path)
        throws DBException
    {
        return getObjectByPath(monitor, rootSC, path.getCatalogName(), path.getSchemaName(), path.getTableName());
    }

    public static DBSObject getObjectByPath(
            DBRProgressMonitor monitor,
            DBSEntityContainer rootSC,
            String catalogName,
            String schemaName,
            String tableName)
        throws DBException
    {
        if (!CommonUtils.isEmpty(catalogName)) {
            DBSObject catalog = rootSC.getChild(monitor, catalogName);
            if (!(catalog instanceof DBSEntityContainer)) {
                return null;
            }
            rootSC = (DBSEntityContainer) catalog;
        }
        if (!CommonUtils.isEmpty(schemaName)) {
            DBSObject schema = rootSC.getChild(monitor, schemaName);
            if (!(schema instanceof DBSEntityContainer)) {
                return null;
            }
            rootSC = (DBSEntityContainer) schema;
        }
        Class<? extends DBSObject> childType = rootSC.getChildType(monitor);
        if (DBSTable.class.isAssignableFrom(childType)) {
            return rootSC.getChild(monitor, tableName);
        } else {
            // Child is not a table. May be catalog/schema names was omitted.
            // Try to use active child
            if (rootSC instanceof DBSEntitySelector) {
                DBSObject activeChild = ((DBSEntitySelector) rootSC).getActiveChild(monitor);
                if (activeChild instanceof DBSEntityContainer && DBSTable.class.isAssignableFrom(((DBSEntityContainer)activeChild).getChildType(monitor))) {
                    return ((DBSEntityContainer)activeChild).getChild(monitor, tableName);
                }
            }

            // Table container not found
            return null;
        }
    }

    public static DBSObject findNestedObject(
        DBRProgressMonitor monitor,
        DBSEntityContainer parent,
        List<String> names
    )
        throws DBException
    {
        for (int i = 0; i < names.size(); i++) {
            String childName = names.get(i);
            DBSObject child = parent.getChild(monitor, childName);
            if (child == null) {
                break;
            }
            if (i == names.size() - 1) {
                return child;
            }
            if (child instanceof DBSEntityContainer) {
                parent = DBSEntityContainer.class.cast(child);
            } else {
                break;
            }
        }
        return null;
    }

    /**
     * Finds object by its name (case insensitive)
     *
     * @param theList    object list
     * @param objectName object name
     * @return object or null
     */
    public static <T extends DBSObject> T findObject(Collection<T> theList, String objectName)
    {
        if (!CommonUtils.isEmpty(theList)) {
            for (T object : theList) {
                if (object.getName().equalsIgnoreCase(objectName)) {
                    return object;
                }
            }
        }
        return null;
    }

    public static <T> T queryParentInterface(Class<T> i, DBSObject object)
    {
        if (object == null || object.getParentObject() == null) {
            return null;
        }
        DBSObject parent = object.getParentObject();
        if (i.isAssignableFrom(parent.getClass())) {
            return i.cast(object.getParentObject());
        } else if (parent instanceof DBSDataSourceContainer && object.getDataSource() != null && i.isAssignableFrom(
            object.getDataSource().getClass())) {
            // Root object's parent is data source container (not datasource)
            // So try to extract this info from real datasource object
            return i.cast(object.getDataSource());
        } else {
            return null;
        }
    }

    public static boolean isNullValue(Object value)
    {
        return (value == null || (value instanceof DBDValue && ((DBDValue) value).isNull()));
    }

    public static Object makeNullValue(Object value)
    {
        if (value instanceof DBDValue) {
            return ((DBDValue) value).makeNull();
        } else {
            return null;
        }
    }

    public static DBDColumnBinding getColumnBinding(DBPDataSource dataSource, DBCColumnMetaData columnMeta)
    {
        return new DBDColumnBinding(
            columnMeta,
            getColumnValueHandler(dataSource, columnMeta));
    }

    public static DBDValueHandler getColumnValueHandler(DBPDataSource dataSource, DBSTypedObject column)
    {
        DBDValueHandler typeHandler = null;
        DataTypeProviderDescriptor typeProvider = DataSourceRegistry.getDefault().getDataTypeProvider(dataSource, column);
        if (typeProvider != null) {
            typeHandler = typeProvider.getInstance().getHandler(dataSource, column);
        }
        if (typeHandler == null) {
            typeHandler = DBCDefaultValueHandler.INSTANCE;
        }
        return typeHandler;
    }

    public static void findValueLocators(
        DBRProgressMonitor monitor,
        DBDColumnBinding[] bindings)
    {
        Map<DBSTable, DBDValueLocator> locatorMap = new HashMap<DBSTable, DBDValueLocator>();
        try {
            for (DBDColumnBinding column : bindings) {
                DBCColumnMetaData meta = column.getColumn();
                if (meta.getTable() == null || !meta.getTable().isIdentitied(monitor)) {
                    continue;
                }
                DBSTableColumn tableColumn = meta.getTableColumn(monitor);
                if (tableColumn == null) {
                    continue;
                }
                // We got table name and column name
                // To be editable we need this result set contain set of columns from the same table
                // which construct any unique key
                DBDValueLocator valueLocator = locatorMap.get(meta.getTable().getTable(monitor));
                if (valueLocator == null) {
                    DBCTableIdentifier tableIdentifier = meta.getTable().getBestIdentifier(monitor);
                    if (tableIdentifier == null) {
                        continue;
                    }
                    valueLocator = new DBDValueLocator(
                        meta.getTable().getTable(monitor),
                        tableIdentifier);
                    locatorMap.put(meta.getTable().getTable(monitor), valueLocator);
                }
                column.initValueLocator(tableColumn, valueLocator);
            }
        }
        catch (DBException e) {
            log.error("Can't extract column identifier info", e);
        }
    }

    public static DBSForeignKey getUniqueForeignConstraint(DBCColumnMetaData column)
    {
        return getUniqueForeignConstraint(null, column);
    }

    public static DBSForeignKey getUniqueForeignConstraint(DBRProgressMonitor monitor, DBCColumnMetaData column)
    {
        RefColumnFinder finder = new RefColumnFinder(column);
        try {
            if (monitor != null) {
                finder.run(monitor);
            } else {
                DBeaverCore.getInstance().runAndWait2(finder);
            }
        }
        catch (InvocationTargetException e) {
            // ignore
        }
        catch (InterruptedException e) {
            // do nothing
        }
        return finder.refConstraint;
    }

    public static List<DBSTableColumn> getBestTableIdentifier(DBRProgressMonitor monitor, DBSTable table)
        throws DBException
    {
        if (table.isView() || CommonUtils.isEmpty(table.getColumns(monitor))) {
            return Collections.emptyList();
        }

        List<DBSObject> identifiers = new ArrayList<DBSObject>();
        // Check constraints
        Collection<? extends DBSConstraint> uniqueKeys = table.getUniqueKeys(monitor);
        if (!CommonUtils.isEmpty(uniqueKeys)) {
            for (DBSConstraint constraint : uniqueKeys) {
                if (constraint.getConstraintType().isUnique() && !CommonUtils.isEmpty(constraint.getColumns(monitor))) {
                    identifiers.add(constraint);
                }
            }
        }
        if (identifiers.isEmpty()) {
            // Check indexes only if no unique constraints found
            try {
                Collection<? extends DBSIndex> indexes = table.getIndexes(monitor);
                if (!CommonUtils.isEmpty(indexes)) {
                    for (DBSIndex index : indexes) {
                        if (index.isUnique() && !CommonUtils.isEmpty(index.getColumns(monitor))) {
                            identifiers.add(index);
                        }
                    }
                }
            } catch (DBException e) {
                log.debug(e);
            }
        }

        if (!identifiers.isEmpty()) {
            // Find PK or unique key
            DBSConstraint uniqueId = null;
            DBSIndex uniqueIndex = null;
            for (DBSObject id : identifiers) {
                if (id instanceof DBSConstraint) {
                    if (((DBSConstraint)id).getConstraintType() == DBSConstraintType.PRIMARY_KEY) {
                        return getTableColumns(monitor, (DBSConstraint)id);
                    } else if (((DBSConstraint)id).getConstraintType().isUnique()) {
                        uniqueId = (DBSConstraint)id;
                    }
                } else {
                    uniqueIndex = (DBSIndex)id;
                }
            }
            return uniqueId != null ? getTableColumns(monitor, uniqueId) : uniqueIndex != null ? getTableColumns(monitor, uniqueIndex) : Collections.<DBSTableColumn>emptyList();
        } else {
            return Collections.emptyList();
        }
    }

    public static List<DBSTableColumn> getTableColumns(DBRProgressMonitor monitor, DBSConstraint constraint)
    {
        Collection<? extends DBSConstraintColumn> constraintColumns = constraint.getColumns(monitor);
        List<DBSTableColumn> columns = new ArrayList<DBSTableColumn>(constraintColumns.size());
        for (DBSConstraintColumn column : constraintColumns) {
            columns.add(column.getTableColumn());
        }
        return columns;
    }

    public static List<DBSTableColumn> getTableColumns(DBRProgressMonitor monitor, DBSIndex index)
    {
        Collection<? extends DBSIndexColumn> indexColumns = index.getColumns(monitor);
        List<DBSTableColumn> columns = new ArrayList<DBSTableColumn>(indexColumns.size());
        for (DBSIndexColumn column : indexColumns) {
            columns.add(column.getTableColumn());
        }
        return columns;
    }

    public static DBCStatement prepareSelectQuery(
        DBCExecutionContext context,
        String query,
        long offset,
        long maxRows) throws DBCException
    {
        boolean hasLimits = offset >= 0 && maxRows > 0;

        DBCQueryTransformer limitTransformer = null, fetchAllTransformer = null;
        if (context.getDataSource() instanceof DBCQueryTransformProvider) {
            if (hasLimits) {
                limitTransformer = ((DBCQueryTransformProvider) context.getDataSource()).createQueryTransformer(DBCQueryTransformType.RESULT_SET_LIMIT);
            } else {
                fetchAllTransformer = ((DBCQueryTransformProvider) context.getDataSource()).createQueryTransformer(DBCQueryTransformType.FETCH_ALL_TABLE);
            }
        }

        if (hasLimits && limitTransformer != null) {
            limitTransformer.setParameters(offset, maxRows);
            query = limitTransformer.transformQueryString(query);
        } else if (fetchAllTransformer != null) {
            query = fetchAllTransformer.transformQueryString(query);
        }

        DBCStatement dbStat = context.prepareStatement(query, false, false, false);

        if (hasLimits) {
            if (limitTransformer == null) {
                dbStat.setLimit(offset, maxRows);
            } else {
                limitTransformer.transformStatement(dbStat, 0);
            }
        } else if (fetchAllTransformer != null) {
            fetchAllTransformer.transformStatement(dbStat, 0);
        }

        return dbStat;
    }

    private static class RefColumnFinder implements DBRRunnableWithProgress {
        private DBCColumnMetaData column;
        private DBSForeignKey refConstraint;

        private RefColumnFinder(DBCColumnMetaData column)
        {
            this.column = column;
        }

        public void run(DBRProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException
        {
            try {
                List<DBSForeignKey> refs = column.getForeignKeys(monitor);
                if (refs != null && !refs.isEmpty()) {
                    refConstraint = refs.get(0);
                }
            }
            catch (DBException e) {
                throw new InvocationTargetException(e);
            }
        }
    }
}
