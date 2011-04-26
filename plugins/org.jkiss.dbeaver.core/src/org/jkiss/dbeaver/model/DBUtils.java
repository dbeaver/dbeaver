/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.data.DBDColumnBinding;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueLocator;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.DBCDefaultValueHandler;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DataTypeProviderDescriptor;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * DBUtils
 */
public final class DBUtils {

    static final Log log = LogFactory.getLog(DBUtils.class);

    public static <TYPE extends DBSObject> Comparator<TYPE> nameComparator()
    {
        return new Comparator<TYPE>() {
            public int compare(DBSObject o1, DBSObject o2)
            {
                return o1.getName().compareTo(o2.getName());
            }
        };
    }

    public static String getUniqueObjectId(DBSObject object)
    {
        StringBuilder buffer = new StringBuilder();
        for (DBSObject obj = object; obj != null; obj = obj.getParentObject()) {
            final String objectName = obj.getName();
            //if (!isValidObjectName(objectName)) {
            //   continue;
            //}
            if (buffer.length() > 0) {
                buffer.insert(0, '.');
            }
            buffer.insert(0, objectName);
        }
        return buffer.toString();
    }

    public static String getQuotedIdentifier(DBSObject object)
    {
        return getQuotedIdentifier(object.getDataSource(), object.getName());
    }

    public static boolean isQuotedIdentifier(DBPDataSource dataSource, String str)
    {
        final String quote = dataSource.getInfo().getIdentifierQuoteString();
        return quote != null && str.startsWith(quote) && str.endsWith(quote);
    }

    public static String getQuotedIdentifier(DBPDataSource dataSource, String str)
    {
        final DBPDataSourceInfo info = dataSource.getInfo();
        String quoteString = info.getIdentifierQuoteString();
        if (quoteString == null) {
            return str;
        }
        if (str.startsWith(quoteString) && str.endsWith(quoteString)) {
            // Already quoted
            return str;
        }

        final String strUpper = str.toUpperCase();

        // Check for keyword conflict
        boolean hasBadChars = dataSource.getContainer().getKeywordManager().getKeywordType(strUpper) == DBPKeywordType.KEYWORD;

        // Check for case of quoted idents. Do not check for unquoted case - we don't need to quote em anyway
        if (!hasBadChars && info.supportsQuotedMixedCase()) {
            // See how unquoted idents are stored
            // If passed identifier case differs from unquoted then we need to escape it
            if (info.storesUnquotedCase() == DBPIdentifierCase.UPPER) {
                hasBadChars = !str.equals(strUpper);
            } else if (info.storesUnquotedCase() == DBPIdentifierCase.LOWER) {
                hasBadChars = !str.equals(str.toLowerCase());
            }
        }

        // Check for bad characters
        if (!hasBadChars) {
            for (int i = 0; i < str.length(); i++) {
                if (!info.validUnquotedCharacter(str.charAt(i))) {
                    hasBadChars = true;
                    break;
                }
            }
        }
        if (!hasBadChars) {
            return str;
        }
        // Escape quote chars
        if (str.indexOf(quoteString) != -1) {
            str = str.replace(quoteString, quoteString + quoteString);
        }
        return quoteString + str + quoteString;
    }

    public static String getFullQualifiedName(DBPDataSource dataSource, DBSObject ... path)
    {
        final DBPDataSourceInfo info = dataSource.getInfo();
        StringBuilder name = new StringBuilder();
        DBSObject parent = null;
        for (DBSObject namePart : path) {
            if (namePart == null) {
                continue;
            }
            if (namePart instanceof DBSCatalog && ((info.getCatalogUsage() & DBPDataSourceInfo.USAGE_DML) == 0)) {
                // Do not use catalog name in FQ name
                continue;
            }
            if (namePart instanceof DBSSchema && ((info.getSchemaUsage() & DBPDataSourceInfo.USAGE_DML) == 0)) {
                // Do not use schema name in FQ name
                continue;
            }
            // Check for valid object name
            if (!isValidObjectName(namePart.getName())) {
               continue;
            }
            if (name.length() > 0) {
                if (parent instanceof DBSCatalog) {
                    if (!info.isCatalogAtStart()) {
                        log.warn("Catalog name should be at the start of full-qualified name!");
                    }
                    name.append(info.getCatalogSeparator());
                } else {
                    name.append(info.getStructSeparator());
                }
            }
            name.append(DBUtils.getQuotedIdentifier(dataSource, namePart.getName()));
            parent = namePart;
        }
        return name.toString();
    }

    /**
     * Checks that object has valid object name.
     * Some DB objects have dummy names (like "" or ".") - we won't use them for certain purposes.
     * @param name object name
     * @return true or false
     */
    public static boolean isValidObjectName(String name)
    {
        if (name == null) {
            return false;
        }
        boolean validName = false;
        for (int i = 0; i < name.length(); i++) {
            if (Character.isLetter(name.charAt(i))) {
                validName = true;
                break;
            }
        }
        return validName;
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
        } else {
            // In some drivers only one catalog exists and it's not used in table names
            // So we can use it as default
            // Actually I saw it only in PostgreSQL
            Collection<? extends DBSEntity> children = rootSC.getChildren(monitor);
            if (children != null && children.size() == 1) {
                DBSEntity child = children.iterator().next();
                if (child instanceof DBSCatalog) {
                    rootSC = (DBSCatalog)child;
                }
            }
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
                DBSObject activeChild = ((DBSEntitySelector) rootSC).getSelectedEntity();
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
                if (parent instanceof DBSEntitySelector) {
                    DBSObject activeChild = ((DBSEntitySelector) parent).getSelectedEntity();
                    if (activeChild instanceof DBSEntityContainer) {
                        parent = (DBSEntityContainer)activeChild;
                        child = parent.getChild(monitor, childName);
                    }
                }
            }
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
    public static <T extends DBPNamedObject> T findObject(Collection<T> theList, String objectName)
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

    /**
     * Finds object by its name (case insensitive)
     *
     * @param theList    object list
     * @param objectName object name
     * @return object or null
     */
    public static <T extends DBPNamedObject> List<T> findObjects(Collection<T> theList, String objectName)
    {
        if (!CommonUtils.isEmpty(theList)) {
            List<T> result = new ArrayList<T>();
            for (T object : theList) {
                if (object.getName().equalsIgnoreCase(objectName)) {
                    result.add(object);
                }
            }
            return result;
        }
        return null;
    }

    public static <T> T getAdapter(Class<T> adapterType, DBPObject object)
    {
        if (object instanceof DBSDataSourceContainer) {
            // Root object's parent is data source container (not datasource)
            // So try to get adapter from real datasource object
            object = ((DBSDataSourceContainer)object).getDataSource();
        }
        if (object == null) {
            return null;
        }
        if (adapterType.isAssignableFrom(object.getClass())) {
            return adapterType.cast(object);
        } else if (object instanceof IAdaptable) {
            return adapterType.cast(((IAdaptable)object).getAdapter(adapterType));
        } else {
            return null;
        }
    }

    public static <T> T getParentAdapter(Class<T> i, DBSObject object)
    {
        if (object == null || object.getParentObject() == null) {
            return null;
        }
        return getAdapter(i, object.getParentObject());
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

    public static DBDColumnBinding getColumnBinding(DBCExecutionContext context, DBCColumnMetaData columnMeta)
    {
        return new DBDColumnBinding(
            columnMeta,
            getColumnValueHandler(context, columnMeta));
    }

    public static DBDValueHandler getColumnValueHandler(DBCExecutionContext context, DBSTypedObject column)
    {
        DBDValueHandler typeHandler = null;
        DataTypeProviderDescriptor typeProvider = DataSourceProviderRegistry.getDefault().getDataTypeProvider(context.getDataSource(), column);
        if (typeProvider != null) {
            typeHandler = typeProvider.getInstance().getHandler(context, column);
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
                DBeaverCore.getInstance().runInProgressService(finder);
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

    public static DBCStatement prepareStatement(
        DBCExecutionContext context,
        String query,
        long offset,
        long maxRows) throws DBCException
    {
        boolean hasLimits = offset >= 0 && maxRows > 0;

        DBCQueryTransformer limitTransformer = null, fetchAllTransformer = null;
        DBCQueryTransformProvider transformProvider = DBUtils.getAdapter(DBCQueryTransformProvider.class, context.getDataSource());
        if (transformProvider != null) {
            if (hasLimits) {
                limitTransformer = transformProvider.createQueryTransformer(DBCQueryTransformType.RESULT_SET_LIMIT);
            } else {
                fetchAllTransformer = transformProvider.createQueryTransformer(DBCQueryTransformType.FETCH_ALL_TABLE);
            }
        }

        if (hasLimits && limitTransformer != null) {
            limitTransformer.setParameters(offset, maxRows);
            query = limitTransformer.transformQueryString(query);
        } else if (fetchAllTransformer != null) {
            query = fetchAllTransformer.transformQueryString(query);
        }

        DBCStatement dbStat = prepareStatement(context, query);

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

    public static DBCStatement prepareStatement(
        DBCExecutionContext context,
        String query) throws DBCException
    {
        DBCStatementType statementType = DBCStatementType.QUERY;
        // Normalize query
        query = SQLUtils.makeUnifiedLineFeeds(query);

/*
        // Check for output parameters
        String outParamName = SQLUtils.getQueryOutputParameter(context, query);
        if (outParamName != null) {
            statementType = DBCStatementType.EXEC;
        }
*/

            // Check for EXEC query
            final List<String> executeKeywords = context.getDataSource().getInfo().getExecuteKeywords();
            if (!CommonUtils.isEmpty(executeKeywords)) {
                final String queryStart = SQLUtils.getFirstKeyword(query);
                for (String keyword : executeKeywords) {
                    if (keyword.equalsIgnoreCase(queryStart)) {
                        statementType = DBCStatementType.EXEC;
                        break;
                    }
                }
            }

/*
        final DBCStatement statement = context.prepareStatement(statementType, query, false, false, false);
        if (outParamName != null) {
            if (statement instanceof CallableStatement) {
                try {
                    if (outParamName.equals("?")) {
                        ((CallableStatement)statement).registerOutParameter(1, java.sql.Types.OTHER);
                    } else {
                        ((CallableStatement)statement).registerOutParameter(outParamName, java.sql.Types.OTHER);
                    }
                } catch (SQLException e) {
                    throw new DBCException(e);
                }
            }
        }
        return statement;
*/
        return context.prepareStatement(statementType, query, false, false, false);
    }

    public static <T extends DBPNamedObject> void orderObjects(List<T> objects)
    {
        Collections.sort(objects, new Comparator<T>() {
            public int compare(T o1, T o2)
            {
                return o1.getName().compareTo(o2.getName());
            }
        });
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
