/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.DBCDefaultValueHandler;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.*;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DataTypeProviderDescriptor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

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
            @Override
            public int compare(DBSObject o1, DBSObject o2)
            {
                return o1.getName().compareTo(o2.getName());
            }
        };
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
        return getQuotedIdentifier(dataSource, str, true);
    }

    public static String getQuotedIdentifier(DBPDataSource dataSource, String str, boolean caseSensitiveNames)
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

        // Check for keyword conflict
        boolean hasBadChars = dataSource.getContainer().getKeywordManager().getKeywordType(str) == DBPKeywordType.KEYWORD;

        if (caseSensitiveNames) {
            // Check for case of quoted idents. Do not check for unquoted case - we don't need to quote em anyway
            if (!hasBadChars && info.supportsQuotedMixedCase()) {
                // See how unquoted idents are stored
                // If passed identifier case differs from unquoted then we need to escape it
                if (info.storesUnquotedCase() == DBPIdentifierCase.UPPER) {
                    hasBadChars = !str.equals(str.toUpperCase());
                } else if (info.storesUnquotedCase() == DBPIdentifierCase.LOWER) {
                    hasBadChars = !str.equals(str.toLowerCase());
                }
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
        if (str.contains(quoteString)) {
            str = str.replace(quoteString, quoteString + quoteString);
        }
        return quoteString + str + quoteString;
    }

    public static String getFullQualifiedName(DBPDataSource dataSource, DBSObject ... path)
    {
        final DBPDataSourceInfo info = dataSource.getInfo();
        StringBuilder name = new StringBuilder(20);
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

    public static String getSimpleQualifiedName(Object... names)
    {
        StringBuilder name = new StringBuilder();
        for (Object namePart : names) {
            if (namePart == null) {
                continue;
            }
            if (name.length() > 0) {
                name.append('.');
            }
            name.append(namePart);
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
            DBSObjectContainer rootSC,
            String catalogName,
            String schemaName,
            String tableName)
        throws DBException
    {
        if (!CommonUtils.isEmpty(catalogName)) {
            DBSObject catalog = rootSC.getChild(monitor, catalogName);
            if (!(catalog instanceof DBSObjectContainer)) {
                return null;
            }
            rootSC = (DBSObjectContainer) catalog;
        } else {
            // In some drivers only one catalog exists and it's not used in table names
            // So we can use it as default
            // Actually I saw it only in PostgreSQL
            Collection<? extends DBSObject> children = rootSC.getChildren(monitor);
            if (children != null && children.size() == 1) {
                DBSObject child = children.iterator().next();
                if (child instanceof DBSCatalog) {
                    rootSC = (DBSCatalog)child;
                }
            }
        }
        if (!CommonUtils.isEmpty(schemaName)) {
            DBSObject schema = rootSC.getChild(monitor, schemaName);
            if (!(schema instanceof DBSObjectContainer)) {
                return null;
            }
            rootSC = (DBSObjectContainer) schema;
        }
        Class<? extends DBSObject> childType = rootSC.getChildType(monitor);
        if (DBSTable.class.isAssignableFrom(childType)) {
            return rootSC.getChild(monitor, tableName);
        } else {
            // Child is not a table. May be catalog/schema names was omitted.
            // Try to use active child
            DBSObjectSelector objectSelector = DBUtils.getAdapter(DBSObjectSelector.class, rootSC);
            if (objectSelector != null) {
                DBSObjectContainer objectContainer = DBUtils.getAdapter(DBSObjectContainer.class, objectSelector.getSelectedObject());
                if (objectContainer != null && DBSTable.class.isAssignableFrom(objectContainer.getChildType(monitor))) {
                    return objectContainer.getChild(monitor, tableName);
                }
            }

            // Table container not found
            return null;
        }
    }

    public static DBSObject findNestedObject(
        DBRProgressMonitor monitor,
        DBSObjectContainer parent,
        List<String> names)
        throws DBException
    {
        for (int i = 0; i < names.size(); i++) {
            String childName = names.get(i);
            DBSObject child = parent.getChild(monitor, childName);
            if (child == null) {
                DBSObjectSelector selector = DBUtils.getAdapter(DBSObjectSelector.class, parent);
                if (selector != null) {
                    DBSObjectContainer container = DBUtils.getAdapter(DBSObjectContainer.class, selector.getSelectedObject());
                    if (container != null) {
                        parent = container;
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
            if (child instanceof DBSObjectContainer) {
                parent = DBSObjectContainer.class.cast(child);
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
        if (theList != null && !theList.isEmpty()) {
            if (theList instanceof List) {
                List<T> l = (List<T>)theList;
                for (int i = 0; i < l.size(); i++) {
                    if (l.get(i).getName().equalsIgnoreCase(objectName)) {
                        return l.get(i);
                    }

                }
            } else {
                for (T object : theList) {
                    if (object.getName().equalsIgnoreCase(objectName)) {
                        return object;
                    }
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
        if (theList != null && !theList.isEmpty()) {
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

    /**
     * Search for virtual entity descriptor
     * @param object object
     * @return object path
     */
    public static List<DBSObject> getObjectPath(DBSObject object, boolean includeSelf)
    {
        List<DBSObject> path = new ArrayList<DBSObject>();
        for (DBSObject obj = includeSelf ? object : object.getParentObject(); obj != null; obj = obj.getParentObject()) {
            path.add(0, obj);
        }
        return path;
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

    public static DBDAttributeBinding getColumnBinding(DBCExecutionContext context, DBCAttributeMetaData attributeMeta)
    {
        DBSTypedObject columnMeta = attributeMeta;
        // We won't query for real column because we don't want make (possibly) another
        // query in the the middle of currently fetching one.
        // Maybe sometimes its possible so leave this code commented
/*
        try {
            DBSEntityAttribute entityAttribute = attributeMeta.getAttribute(context.getProgressMonitor());
            if (entityAttribute != null) {
                columnMeta = entityAttribute;
            }
        } catch (DBException e) {
            log.warn("Can't obtain entity attribute", e);
        }
*/
        return new DBDAttributeBinding(
            attributeMeta,
            findValueHandler(context, columnMeta));
    }

    public static DBDValueHandler findValueHandler(DBCExecutionContext context, DBSTypedObject column)
    {
        return findValueHandler(context.getDataSource(), context, column.getTypeName(), column.getTypeID());
    }

    public static DBDValueHandler findValueHandler(DBPDataSource dataSource, DBSTypedObject column)
    {
        return findValueHandler(dataSource, dataSource.getContainer(), column.getTypeName(), column.getTypeID());
    }

    public static DBDValueHandler findValueHandler(DBPDataSource dataSource, DBDPreferences preferences, String typeName, int valueType)
    {
        DBDValueHandler typeHandler = null;
        DataTypeProviderDescriptor typeProvider = DataSourceProviderRegistry.getDefault().getDataTypeProvider(
            dataSource, typeName, valueType);
        if (typeProvider != null) {
            typeHandler = typeProvider.getInstance().getHandler(preferences, typeName, valueType);
        }
        if (typeHandler == null) {
            if (preferences == null) {
                typeHandler = DBCDefaultValueHandler.INSTANCE;
            } else {
                typeHandler = preferences.getDefaultValueHandler();
            }
        }
        return typeHandler;
    }

    public static void findValueLocators(
        DBRProgressMonitor monitor,
        DBDAttributeBinding[] bindings)
    {
        Map<DBSEntity, DBDValueLocator> locatorMap = new HashMap<DBSEntity, DBDValueLocator>();
        try {
            for (DBDAttributeBinding column : bindings) {
                DBCAttributeMetaData meta = column.getAttribute();
                if (meta.getEntity() == null || !meta.getEntity().isIdentified(monitor)) {
                    continue;
                }
                DBSEntityAttribute tableColumn = meta.getAttribute(monitor);
                if (tableColumn == null) {
                    continue;
                }
                // We got table name and column name
                // To be editable we need this result   set contain set of columns from the same table
                // which construct any unique key
                DBDValueLocator valueLocator = locatorMap.get(meta.getEntity().getEntity(monitor));
                if (valueLocator == null) {
                    DBCEntityIdentifier entityIdentifier = meta.getEntity().getBestIdentifier(monitor);
                    if (entityIdentifier == null) {
                        continue;
                    }
                    valueLocator = new DBDValueLocator(
                        meta.getEntity().getEntity(monitor),
                        entityIdentifier);
                    locatorMap.put(meta.getEntity().getEntity(monitor), valueLocator);
                }
                column.initValueLocator(tableColumn, valueLocator);
            }
        }
        catch (DBException e) {
            log.error("Can't extract column identifier info", e);
        }
    }

    public static DBSEntityReferrer getUniqueForeignConstraint(DBCAttributeMetaData attribute)
    {
        return getUniqueForeignConstraint(null, attribute);
    }

    public static DBSEntityReferrer getUniqueForeignConstraint(DBRProgressMonitor monitor, DBCAttributeMetaData attribute)
    {
        RefColumnFinder finder = new RefColumnFinder(attribute);
        try {
            if (monitor != null) {
                finder.run(monitor);
            } else {
                DBeaverUI.runInProgressService(finder);
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

    public static Collection<? extends DBSEntityAttribute> getBestTableIdentifier(DBRProgressMonitor monitor, DBSEntity entity)
        throws DBException
    {
        if (entity instanceof DBSTable && ((DBSTable) entity).isView()) {
            return Collections.emptyList();
        }
        if (CommonUtils.isEmpty(entity.getAttributes(monitor))) {
            return Collections.emptyList();
        }

        List<DBSEntityConstraint> identifiers = new ArrayList<DBSEntityConstraint>();
        // Check constraints
        Collection<? extends DBSEntityConstraint> uniqueKeys = entity.getConstraints(monitor);
        if (!CommonUtils.isEmpty(uniqueKeys)) {
            for (DBSEntityConstraint constraint : uniqueKeys) {
                if (constraint.getConstraintType().isUnique() &&
                    constraint instanceof DBSEntityReferrer &&
                    !CommonUtils.isEmpty(((DBSEntityReferrer)constraint).getAttributeReferences(monitor)))
                {
                    identifiers.add(constraint);
                }
            }
        }
        if (identifiers.isEmpty() && entity instanceof DBSTable) {
            // Check indexes only if no unique constraints found
            try {
                Collection<? extends DBSTableIndex> indexes = ((DBSTable)entity).getIndexes(monitor);
                if (!CommonUtils.isEmpty(indexes)) {
                    for (DBSTableIndex index : indexes) {
                        if (index.isUnique() && !CommonUtils.isEmpty(index.getAttributeReferences(monitor))) {
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
            DBSEntityConstraint uniqueId = null;
            //DBSEntityConstraint uniqueIndex = null;
            for (DBSEntityConstraint id : identifiers) {
                if (id instanceof DBSEntityReferrer && id.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY) {
                    return getEntityAttributes(monitor, (DBSEntityReferrer) id);
                } else if (id.getConstraintType().isUnique()) {
                    uniqueId = id;
                }
            }
            return uniqueId instanceof DBSEntityReferrer ?
                getEntityAttributes(monitor, (DBSEntityReferrer)uniqueId)
                : Collections.<DBSTableColumn>emptyList();
        } else {
            return Collections.emptyList();
        }
    }

    public static List<DBSEntityAttribute> getEntityAttributes(DBRProgressMonitor monitor, DBSEntityReferrer referrer)
    {
        Collection<? extends DBSEntityAttributeRef> constraintColumns = referrer == null ? null : referrer.getAttributeReferences(monitor);
        if (constraintColumns == null) {
            return Collections.emptyList();
        }
        List<DBSEntityAttribute> attributes = new ArrayList<DBSEntityAttribute>(constraintColumns.size());
        for (DBSEntityAttributeRef column : constraintColumns) {
            attributes.add(column.getAttribute());
        }
        return attributes;
    }

    public static DBSEntityAttributeRef getConstraintColumn(DBRProgressMonitor monitor, DBSEntityReferrer constraint, DBSEntityAttribute tableColumn) throws DBException
    {
        Collection<? extends DBSEntityAttributeRef> columns = constraint.getAttributeReferences(monitor);
        if (columns != null) {
            for (DBSEntityAttributeRef constraintColumn : columns) {
                if (constraintColumn.getAttribute() == tableColumn) {
                    return constraintColumn;
                }
            }
        }
        return null;
    }

    public static DBSEntityAttributeRef getConstraintColumn(DBRProgressMonitor monitor, DBSEntityReferrer constraint, String columnName) throws DBException
    {
        Collection<? extends DBSEntityAttributeRef> columns = constraint.getAttributeReferences(monitor);
        if (columns != null) {
            for (DBSEntityAttributeRef constraintColumn : columns) {
                if (constraintColumn.getAttribute().getName().equals(columnName)) {
                    return constraintColumn;
                }
            }
        }
        return null;
    }

    /**
     * Return reference column of referrer association (FK).
     * Assumes that columns in both constraints are in the same order.
     */
    public static DBSEntityAttribute getReferenceAttribute(DBRProgressMonitor monitor, DBSEntityAssociation association, DBSEntityAttribute tableColumn)
    {
        final DBSEntityConstraint refConstr = association.getReferencedConstraint();
        if (association instanceof DBSEntityReferrer && refConstr instanceof DBSEntityReferrer) {
            final Collection<? extends DBSEntityAttributeRef> ownAttrs = ((DBSEntityReferrer) association).getAttributeReferences(monitor);
            final Collection<? extends DBSEntityAttributeRef> refAttrs = ((DBSEntityReferrer) refConstr).getAttributeReferences(monitor);
            if (ownAttrs == null || refAttrs == null || ownAttrs.size() != refAttrs.size()) {
                log.error("Invalid internal state of referrer association");
                return null;
            }
            final Iterator<? extends DBSEntityAttributeRef> ownIterator = ownAttrs.iterator();
            final Iterator<? extends DBSEntityAttributeRef> refIterator = refAttrs.iterator();
            while (ownIterator.hasNext()) {
                if (ownIterator.next().getAttribute() == tableColumn) {
                    return refIterator.next().getAttribute();
                }
                refIterator.next();
            }
        }
        return null;
    }

    public static DBCStatement prepareStatement(
        DBCExecutionContext context,
        DBCStatementType statementType,
        String query,
        long offset,
        long maxRows) throws DBCException
    {
        final boolean dataModifyQuery = SQLUtils.isDataModifyQuery(query);
        final boolean hasLimits = !dataModifyQuery && offset >= 0 && maxRows > 0;

        DBCQueryTransformer limitTransformer = null, fetchAllTransformer = null;
        if (!dataModifyQuery) {
            DBCQueryTransformProvider transformProvider = DBUtils.getAdapter(DBCQueryTransformProvider.class, context.getDataSource());
            if (transformProvider != null) {
                if (hasLimits) {
                    limitTransformer = transformProvider.createQueryTransformer(DBCQueryTransformType.RESULT_SET_LIMIT);
                } else {
                    fetchAllTransformer = transformProvider.createQueryTransformer(DBCQueryTransformType.FETCH_ALL_TABLE);
                }
            }
        }

        if (hasLimits && limitTransformer != null) {
            limitTransformer.setParameters(offset, maxRows);
            query = limitTransformer.transformQueryString(query);
        } else if (fetchAllTransformer != null) {
            query = fetchAllTransformer.transformQueryString(query);
        }

        DBCStatement dbStat = statementType == DBCStatementType.SCRIPT ?
            createStatement(context, query) :
            prepareStatement(context, query);

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

    public static DBCStatement createStatement(
        DBCExecutionContext context,
        String query) throws DBCException
    {
        query = SQLUtils.makeUnifiedLineFeeds(query);
        return context.prepareStatement(DBCStatementType.SCRIPT, query, false, false, false);
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
            final Collection<String> executeKeywords = context.getDataSource().getInfo().getExecuteKeywords();
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

    public static void fireObjectUpdate(DBSObject object)
    {
        fireObjectUpdate(object, null);
    }

    public static void fireObjectUpdate(DBSObject object, Object data)
    {
        final DBSDataSourceContainer container = getContainer(object);
        if (container != null) {
            container.fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, object, data));
        }
    }

    public static void fireObjectAdd(DBSObject object)
    {
        final DBSDataSourceContainer container = getContainer(object);
        if (container != null) {
            container.fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_ADD, object));
        }
    }

    public static void fireObjectRemove(DBSObject object)
    {
        final DBSDataSourceContainer container = getContainer(object);
        if (container != null) {
            container.fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_REMOVE, object));
        }
    }

    public static void fireObjectSelect(DBSObject object, boolean select)
    {
        final DBSDataSourceContainer container = getContainer(object);
        if (container != null) {
            container.fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_SELECT, object, select));
        }
    }

    public static DBSDataSourceContainer getContainer(DBSObject object)
    {
        if (object == null) {
            log.warn("Null object passed");
            return null;
        }
        final DBPDataSource dataSource = object.getDataSource();
        if (dataSource == null) {
            return null;
        }
        return dataSource.getContainer();
    }

    public static String getObjectUniqueName(DBSObject object)
    {
        if (object instanceof DBSObjectUnique) {
            return ((DBSObjectUnique) object).getUniqueName();
        } else {
            return object.getName();
        }
    }

    public static DBSDataType findBestDataType(Collection<? extends DBSDataType> allTypes, String ... typeNames)
    {
        for (String testType : typeNames) {
            for (DBSDataType dataType : allTypes) {
                if (dataType.getName().equalsIgnoreCase(testType)) {
                    return dataType;
                }
            }
        }
        return null;
    }

    public static DBSDataType resolveDataType(
        DBRProgressMonitor monitor,
        DBPDataSource dataSource,
        String fullTypeName)
        throws DBException
    {
        if (!(dataSource instanceof DBPDataTypeProvider)) {
            // NoSuchElementException data type provider
            return null;
        }
        DBSDataType dataType = ((DBPDataTypeProvider) dataSource).resolveDataType(monitor, fullTypeName);
        if (dataType == null) {
            log.debug("Data type '" + fullTypeName + "' can't be resolved by '" + dataSource + "'");
        }
        return dataType;
    }

    public static <T extends DBPNamedObject> void orderObjects(List<T> objects)
    {
        Collections.sort(objects, new Comparator<T>() {
            @Override
            public int compare(T o1, T o2)
            {
                return o1.getName().compareTo(o2.getName());
            }
        });
    }

    public static String getDefaultValueDisplayString(Object value)
    {
        if (value == null) {
            return DBConstants.NULL_VALUE_LABEL;
        }
        if (value.getClass().isArray()) {
            if (value.getClass().getComponentType() == Byte.TYPE) {
                byte[] bytes = (byte[]) value;
                return CommonUtils.toHexString(bytes, 0, 2000);
            } else {
                return UIUtils.makeStringForUI(value).toString();
            }
        }
        String className = value.getClass().getName();
        if (className.startsWith("java.lang") || className.startsWith("java.util")) {
            // Standard types just use toString
            return value.toString();
        }
        // Unknown types print their class name
        boolean hasToString;
        try {
            hasToString = value.getClass().getMethod("toString").getDeclaringClass() != Object.class;
        } catch (Throwable e) {
            log.debug(e);
            hasToString = false;
        }
        if (hasToString) {
            return value.toString();
        } else {
            return "[" + value.getClass().getSimpleName() + "]";
        }
    }

    public static DBPObject getPublicObject(DBPObject object)
    {
        if (object instanceof DBSDataSourceContainer) {
            return ((DBSDataSourceContainer)object).getDataSource();
        } else {
            return object;
        }
    }

    public static String getObjectShortName(Object object)
    {
        String strValue;
        if (object instanceof DBPNamedObject) {
            strValue = ((DBPNamedObject)object).getName();
        } else {
            strValue = String.valueOf(object);
        }
        return strValue;
    }

    public static String getObjectFullName(DBPNamedObject object)
    {
        if (object instanceof DBPQualifiedObject) {
            return ((DBPQualifiedObject) object).getFullQualifiedName();
        } else if (object instanceof DBSObjectReference) {
            DBSObjectReference reference = (DBSObjectReference)object;
            DBPDataSource dataSource = reference.getContainer().getDataSource();
            return getFullQualifiedName(dataSource, reference.getContainer()) +
                dataSource.getInfo().getStructSeparator() +
                getQuotedIdentifier(dataSource, reference.getName());
        } else {
            return object.getName();
        }
    }

    public static String generateScript(DBPDataSource dataSource, IDatabasePersistAction[] persistActions)
    {
        String lineSeparator = ContentUtils.getDefaultLineSeparator();
        StringBuilder script = new StringBuilder(512);
        for (IDatabasePersistAction action : CommonUtils.safeArray(persistActions)) {
            if (script.length() > 0) {
                script.append(lineSeparator);
            }
            script.append(action.getScript());
            script.append(dataSource.getInfo().getScriptDelimiter()).append(lineSeparator);
        }
        return script.toString();
    }

    public static DBIcon getDataIcon(DBSTypedObject type)
    {
        switch (type.getDataKind()) {
            case BOOLEAN:
                return DBIcon.TYPE_BOOLEAN;
            case STRING:
                return DBIcon.TYPE_STRING;
            case NUMERIC:
                return DBIcon.TYPE_NUMBER;
            case DATETIME:
                return DBIcon.TYPE_DATETIME;
            case BINARY:
                return DBIcon.TYPE_BINARY;
            case LOB:
                if (type.getTypeName().toUpperCase().contains("XML")) {
                    return DBIcon.TYPE_XML;
                }
                return DBIcon.TYPE_LOB;
            case ARRAY:
                return DBIcon.TYPE_ARRAY;
            case STRUCT:
                return DBIcon.TYPE_STRUCT;
            case OBJECT:
                return DBIcon.TYPE_OBJECT;
            default:
                return DBIcon.TYPE_UNKNOWN;
        }
    }

    private static class RefColumnFinder implements DBRRunnableWithProgress {
        private DBCAttributeMetaData attribute;
        private DBSEntityReferrer refConstraint;

        private RefColumnFinder(DBCAttributeMetaData attribute)
        {
            this.attribute = attribute;
        }

        @Override
        public void run(DBRProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException
        {
            try {
                List<DBSEntityReferrer> refs = attribute.getReferrers(monitor);
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
