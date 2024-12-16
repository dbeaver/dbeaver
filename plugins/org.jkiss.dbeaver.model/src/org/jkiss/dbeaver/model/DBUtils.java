/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.data.DBDValueError;
import org.jkiss.dbeaver.model.impl.data.DefaultValueHandler;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.runtime.DBRProgressListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryType;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.*;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVEntityAttribute;
import org.jkiss.dbeaver.model.virtual.DBVEntityConstraint;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.dbeaver.runtime.DBServiceConnections;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.IVariableResolver;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * DBUtils
 */
public final class DBUtils {

    private static final Log log = Log.getLog(DBUtils.class);

    @NotNull
    public static String getQuotedIdentifier(@NotNull DBPNamedObject object) {
        if (object instanceof DBSContextBoundAttribute cba) {
            return cba.formatMemberReference(false, null, DBPAttributeReferencePurpose.UNSPECIFIED);
        } else {
            return object instanceof DBSObject dbo
                ? getQuotedIdentifier(dbo.getDataSource(), object.getName())
                : object.getName();
        }
    }

    /**
     * Get object name in quotes if they are needed.
     *
     * @param object to get identifier of
     * @return object identifier
     */
    @NotNull
    public static String getQuotedIdentifier(@NotNull DBSObject object) {
        if (object instanceof DBSContextBoundAttribute cba) {
            return cba.formatMemberReference(false, null, DBPAttributeReferencePurpose.UNSPECIFIED);
        } else {
            return getQuotedIdentifier(object.getDataSource(), object.getName());
        }
    }

    /**
     * Get object name in quotes if they are needed.
     *
     * @param object  to get identifier of
     * @param purpose of identifier usage
     * @return object identifier
     */
    @NotNull
    public static String getQuotedIdentifier(@NotNull DBSObject object, @NotNull DBPAttributeReferencePurpose purpose) {
        if (object instanceof DBSContextBoundAttribute cba) {
            return cba.formatMemberReference(false, null, purpose);
        } else {
            return getQuotedIdentifier(object.getDataSource(), object.getName());
        }
    }

    public static boolean isQuotedIdentifier(@NotNull DBPDataSource dataSource, @NotNull String str) {
        return dataSource.getSQLDialect().isQuotedIdentifier(str);
    }

    @NotNull
    public static String getUnQuotedIdentifier(@NotNull DBPDataSource dataSource, @NotNull String str) {
        return dataSource.getSQLDialect().getUnquotedIdentifier(str);
    }

    @NotNull
    public static String getUnQuotedIdentifier(@NotNull String str, String[][] quoteStrings) {
        if (ArrayUtils.isEmpty(quoteStrings)) {
            quoteStrings = BasicSQLDialect.DEFAULT_IDENTIFIER_QUOTES;
        }
        for (String[] quoteString : quoteStrings) {
            str = getUnQuotedIdentifier(str, quoteString[0], quoteString[1]);
        }
        return str;
    }

    @NotNull
    public static String getUnQuotedIdentifier(@NotNull String str, @NotNull String quote) {
        return getUnQuotedIdentifier(str, quote, quote);
    }

    @NotNull
    public static String getUnQuotedIdentifier(@NotNull String str, @NotNull String quote1, @NotNull String quote2) {
        if (quote1 != null && quote2 != null && str.length() >= quote1.length() + quote2.length() &&
            str.startsWith(quote1) && str.endsWith(quote2)) {
            return str.substring(quote1.length(), str.length() - quote2.length());
        }
        return str;
    }

    @NotNull
    public static String getQuotedIdentifier(@NotNull DBPDataSource dataSource, @NotNull String str) {
        return getQuotedIdentifier(dataSource, str, true, false);
    }

    @NotNull
    public static String getQuotedIdentifier(@NotNull DBPDataSource dataSource, @NotNull String str, boolean caseSensitiveNames, boolean quoteAlways) {
        return dataSource.getSQLDialect().getQuotedIdentifier(str, caseSensitiveNames, quoteAlways);
    }

    @NotNull
    public static String getFullQualifiedName(@Nullable DBPDataSource dataSource, @NotNull DBPNamedObject... path) {
        StringBuilder name = new StringBuilder(20 * path.length);
        if (dataSource == null) {
            // It is not SQL identifier, let's just make it simple then
            for (DBPNamedObject namePart : path) {
                if (isVirtualObject(namePart)) {
                    continue;
                }
                if (!name.isEmpty()) {
                    name.append('.');
                }
                name.append(namePart.getName());
            }
        } else {
            final SQLDialect sqlDialect = dataSource.getSQLDialect();

            DBPNamedObject parent = null;
            for (DBPNamedObject namePart : path) {
                if (namePart == null || isVirtualObject(namePart)) {
                    continue;
                }
                if (namePart instanceof DBSCatalog && ((sqlDialect.getCatalogUsage() & SQLDialect.USAGE_DML) == 0)) {
                    // Do not use catalog name in FQ name
                    continue;
                }
                if (namePart instanceof DBSSchema && ((sqlDialect.getSchemaUsage() & SQLDialect.USAGE_DML) == 0)) {
                    // Do not use schema name in FQ name
                    continue;
                }
                // Check for valid object name
                if (!isValidObjectName(namePart.getName())) {
                    continue;
                }
                if (!name.isEmpty()) {
                    if (parent instanceof DBSCatalog) {
                        if (!sqlDialect.isCatalogAtStart()) {
                            log.warn("Catalog name should be at the start of full-qualified name!");
                        }
                        name.append(sqlDialect.getCatalogSeparator());
                    } else {
                        name.append(sqlDialect.getStructSeparator());
                    }
                }
                name.append(DBUtils.getQuotedIdentifier(dataSource, namePart.getName()));
                parent = namePart;
            }
        }
        return name.toString();
    }

    @NotNull
    public static String getSimpleQualifiedName(@NotNull Object... names) {
        StringBuilder name = new StringBuilder(names.length * 16);
        for (Object namePart : names) {
            if (namePart == null) {
                continue;
            }
            if (!name.isEmpty() && name.charAt(name.length() - 1) != '.') {
                name.append('.');
            }
            name.append(namePart);
        }
        return name.toString();
    }

    @NotNull
    public static String getFullyQualifiedName(@NotNull DBPDataSource dataSource, @NotNull String... names) {
        SQLDialect dialect = SQLUtils.getDialectFromDataSource(dataSource);
        StringBuilder name = new StringBuilder(names.length * 16);
        for (String namePart : names) {
            if (namePart == null) {
                continue;
            }
            if (!name.isEmpty()) name.append(dialect.getStructSeparator());
            name.append(DBUtils.getQuotedIdentifier(dataSource, namePart));
        }
        return name.toString();
    }

    /**
     * Checks that object has valid object name.
     *
     * @param name object name
     * @return true or false
     */
    public static boolean isValidObjectName(@Nullable String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        // We need at least one symbol that is not whitespace
        for (int i = 0; i < name.length(); i++) {
            if (!Character.isWhitespace(name.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds catalog, schema or table within specified object container
     *
     * @param monitor          progress monitor
     * @param rootSC           container
     * @param catalogName      catalog name (optional)
     * @param schemaName       schema name (optional)
     * @param objectName       table name (optional)
     * @return found object or null
     */
    @Nullable
    public static DBSObject getObjectByPath(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull DBSObjectContainer rootSC,
        @Nullable String catalogName,
        @Nullable String schemaName,
        @Nullable String objectName)
        throws DBException {
        if (!CommonUtils.isEmpty(catalogName)) {
            Class<? extends DBSObject> childType = rootSC.getPrimaryChildType(monitor);
            if (DBSSchema.class.isAssignableFrom(childType) || DBSEntity.class.isAssignableFrom(childType)) {
                // Datasource supports only schemas. Do not use catalog
                catalogName = null;
            }
        }
        if (!CommonUtils.isEmpty(catalogName) && !CommonUtils.isEmpty(schemaName)) {
            // We have both both - just search both
            DBSObject catalog = rootSC.getChild(monitor, catalogName);
            if (!(catalog instanceof DBSObjectContainer catalogOC)) {
                return null;
            }
            rootSC = catalogOC;
            DBSObject schema = rootSC.getChild(monitor, schemaName);
            if (!(schema instanceof DBSObjectContainer schemaOC)) {
                return null;
            }
            rootSC = schemaOC;
        } else if (!CommonUtils.isEmpty(catalogName) || !CommonUtils.isEmpty(schemaName)) {
            // One container name
            String containerName = !CommonUtils.isEmpty(catalogName) ? catalogName : schemaName;
            DBSObject sc = rootSC.getChild(monitor, containerName);
            if (!DBStructUtils.isConnectedContainer(sc)) {
                sc = null;
            }
            if (!(sc instanceof DBSObjectContainer)) {
                // Not found - try to find in selected object
                DBSObject selectedObject = getSelectedObject(executionContext);
                if (selectedObject instanceof DBSObjectContainer) {
                    if (selectedObject instanceof DBSSchema && selectedObject.getParentObject() instanceof DBSCatalog && CommonUtils.isEmpty(catalogName) &&
                        !CommonUtils.equalObjects(schemaName, selectedObject.getName())) {
                        // We search for schema and active object is schema. Let's search our schema in catalog
                        selectedObject = selectedObject.getParentObject();
                    }
                    if (selectedObject instanceof DBSSchema && CommonUtils.equalObjects(schemaName, selectedObject.getName()) ||
                        selectedObject instanceof DBSCatalog && CommonUtils.equalObjects(catalogName, selectedObject.getName())) {
                        // Selected object is a catalog or schema which is also specified as catalogName/schemaName -
                        sc = selectedObject;
                    } else if (selectedObject instanceof DBSObjectContainer objectContainer) {
                        // Get schema in catalog
                        sc = objectContainer.getChild(monitor, containerName);
                    }
                }
                if (!(sc instanceof DBSObjectContainer)) {
                    return null;
                }
            } else if (CommonUtils.isEmpty(catalogName) && !CommonUtils.isEmpty(schemaName) && sc instanceof DBSCatalog catalog) {
                // Just check a side case - then we have catalog with schema inside with equal names.
                // Probably on this step we found a catalog, but not a schema.
                Class<? extends DBSObject> childType = catalog.getPrimaryChildType(monitor);
                if (DBSSchema.class.isAssignableFrom(childType)) {
                    DBSObject child = catalog.getChild(monitor, schemaName);
                    if (child instanceof DBSSchema) {
                        sc = child;
                    }
                }
            }
            rootSC = (DBSObjectContainer) sc;
        }
        if (objectName == null) {
            return rootSC;
        }
        final DBSObject object = rootSC.getChild(monitor, objectName);
        if (object instanceof DBSEntity) {
            return object;
        } else {
            // Child is not an entity. May be catalog/schema names was omitted.
            // Try to use selected object
            DBSObject selectedObject = DBUtils.getSelectedObject(executionContext);
            if (selectedObject instanceof DBSObjectContainer oc) {
                return oc.getChild(monitor, objectName);
            }

            // Table container not found
            return object;
        }
    }

    @Nullable
    public static <T extends DBPNamedObject> T findObject(@Nullable Collection<T> theList, String objectName) {
        return findObject(theList, objectName, false);
    }

    /**
     * Finds object by its name (case insensitive)
     *
     * @param theList    object list
     * @param objectName object name
     * @return object or null
     */
    @Nullable
    public static <T extends DBPNamedObject> T findObject(@Nullable Collection<T> theList, String objectName, boolean caseInsensitive) {
        if (theList != null && !theList.isEmpty()) {
            for (T object : theList) {
                if (caseInsensitive ? object.getName().equalsIgnoreCase(objectName) : object.getName().equals(objectName)) {
                    return object;
                }
            }
        }
        return null;
    }

    /**
     * Find object (case-sensitive)
     */
    @Nullable
    public static <T extends DBPNamedObject> T findObject(@Nullable T[] theList, String objectName) {
        if (theList != null) {
            for (T object : theList) {
                if (object.getName().equals(objectName)) {
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
    @Nullable
    public static <T extends DBPNamedObject> List<T> findObjects(@Nullable Collection<T> theList, @Nullable String objectName) {
        if (theList != null && !theList.isEmpty()) {
            List<T> result = new ArrayList<>();
            for (T object : theList) {
                if (object.getName().equalsIgnoreCase(objectName)) {
                    result.add(object);
                }
            }
            return result;
        }
        return null;
    }

    @Nullable
    public static <T> T getAdapter(@NotNull Class<T> adapterType, @Nullable Object object) {
        if (object instanceof DBPDataSourceContainer ds) {
            // Root object's parent is data source container (not datasource)
            // So try to get adapter from real datasource object
            object = ds.getDataSource();
        }
        if (object == null) {
            return null;
        }
        if (adapterType.isAssignableFrom(object.getClass())) {
            return adapterType.cast(object);
        } else if (object instanceof IAdaptable adaptable) {
            return adaptable.getAdapter(adapterType);
        } else {
            return null;
        }
    }

    @Nullable
    public static <T> T getParentAdapter(@NotNull Class<T> i, DBSObject object) {
        if (object == null) {
            return null;
        }
        DBSObject parent = object.getParentObject();
        if (parent == null) {
            return null;
        }
        T adapter = getAdapter(i, parent);
        // In some cases parent's adapter is object itself (e.g. DS maybe DS adapter of container)
        return adapter == object ? null : adapter;
    }

    @Nullable
    public static <T> T getParentOfType(@NotNull Class<T> type, DBSObject object) {
        if (object == null) {
            return null;
        }
        for (DBSObject parent = object.getParentObject(); parent != null; parent = parent.getParentObject()) {
            parent = DBUtils.getPublicObject(parent);
            if (parent == null) {
                break;
            }
            if (type.isInstance(parent)) {
                return type.cast(parent);
            } else if (parent instanceof DBPDataSource || parent instanceof DBPDataSourceContainer) {
                break;
            }
        }
        return null;
    }

    public static boolean isParentOf(@NotNull DBSObject child, @NotNull DBSObject parent) {
        for (DBSObject object = child; object != null; object = object.getParentObject()) {
            if (parent.equals(object)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Search for virtual entity descriptor
     *
     * @param object object
     * @return object path
     */
    @NotNull
    public static DBSObject[] getObjectPath(@NotNull DBSObject object, boolean includeSelf) {
        int depth = 0;
        final DBSObject root = includeSelf ? object :
            ((object instanceof DBSTablePartition part) && part.needFullPath() && part.isSubPartition()) ? part.getPartitionParent() :
        object.getParentObject();
        for (DBSObject obj = root; obj != null; obj = obj.getParentObject()) {
            obj = getPublicObjectContainer(obj);
            depth++;
        }
        if ((object instanceof DBSTablePartition part) && part.needFullPath()) {
            // For a parent table
            depth++;
        }
        DBSObject[] path = new DBSObject[depth];
        for (DBSObject obj = root; obj != null; obj = obj.getParentObject()) {
            obj = getPublicObjectContainer(obj);
            path[depth-- - 1] = obj;
            if ((obj instanceof DBSTablePartition part) && part.needFullPath()) {
                path[depth-- - 1] = part.getParentTable();
            }
        }
        return path;
    }

    public static String getObjectFullId(@NotNull DBSObject object) {
        DBSObject[] path = getObjectPath(object, true);
        StringBuilder pathStr = new StringBuilder();
        for (DBSObject obj : path) {
            if (isVirtualObject(obj)) {
                continue;
            }
            if (!pathStr.isEmpty()) {
                pathStr.append('/');
            }
            obj = getPublicObjectContainer(obj);
            if (obj instanceof DBPDataSourceContainer ds) {
                pathStr.append(ds.getId());
            } else {
                pathStr.append(obj.getName());
            }
        }
        return pathStr.toString();
    }

    public static String getObjectNameFromId(String objectId) {
        String[] parts = objectId.split("/");
        return parts[parts.length - 1];
    }

    /**
     * Find object by unique ID.
     * Note: this function searches only inside DBSObjectContainer objects.
     * Usually it works only for entities and entity containers (schemas, catalogs).
     */
    public static DBSObject findObjectById(@NotNull DBRProgressMonitor monitor, @NotNull DBPProject project, @NotNull String objectId) throws DBException {
        String[] names = objectId.split("/");
        DBPDataSourceContainer dataSourceContainer = project.getDataSourceRegistry().getDataSource(names[0]);
        if (dataSourceContainer == null) {
            log.debug("Can't find datasource '" + names[0] + "' in project " + project.getName());
            return null;
        }
        if (names.length == 1) {
            return dataSourceContainer;
        }
        if (!dataSourceContainer.isConnected()) {
            try {
                dataSourceContainer.connect(monitor, true, true);
            } catch (DBException e) {
                throw new DBException("Error connecting to datasource '" + dataSourceContainer.getName() + "'", e);
            }
        }
        DBPDataSource dataSource = dataSourceContainer.getDataSource();
        if (dataSource == null) {
            log.debug("Null datasource in container " + dataSourceContainer.getId());
            return null;
        }
        DBSObjectContainer sc = DBUtils.getAdapter(DBSObjectContainer.class, dataSource);
        DBSEntity finalEntity = null;
        if (sc != null) {
            for (int i = 1; i < names.length - 1; i++) {
                String name = names[i];
                DBSObject child = sc.getChild(monitor, name);
                if (child == null) {
                    log.debug("Can't find child container " + name + " in container " + DBUtils.getObjectFullName(sc, DBPEvaluationContext.UI));
                    return null;
                }
                if (child instanceof DBSObjectContainer oc) {
                    sc = oc;
                } else if (child instanceof DBSEntity entity && i == names.length - 2) {
                    sc = null;
                    finalEntity = entity;
                    break;
                } else {
                    log.debug("Child object '" + name + "' is not a container or entity");
                    return null;
                }
            }
        }
        String objectName = names[names.length - 1];
        if (sc != null) {
            DBSObject object = sc.getChild(monitor, objectName);
            if (object == null) {
                log.debug("Child object '" + objectName + "' not found in container " + DBUtils.getObjectFullName(sc, DBPEvaluationContext.UI));
                throw new DBException("Child object '" + objectName + "' not found in container " + DBUtils.getObjectFullName(sc, DBPEvaluationContext.UI));
            }
            return object;
        } else if (finalEntity != null) {
            DBSEntityAttribute attribute = finalEntity.getAttribute(monitor, objectName);
            if (attribute != null) {
                return attribute;
            }
            if (finalEntity instanceof DBSTable table) {
                List<? extends DBSTrigger> triggers = table.getTriggers(monitor);
                if (triggers != null) {
                    DBSTrigger trigger = DBUtils.findObject(triggers, objectName);
                    if (trigger != null) {
                        return trigger;
                    }
                }
                Collection<? extends DBSTableIndex> indices = table.getIndexes(monitor);
                if (indices != null) {
                    DBSTableIndex index = DBUtils.findObject(indices, objectName);
                    if (index != null) {
                        return index;
                    }
                }
                if (finalEntity instanceof DBSPartitionContainer partitionContainer) {
                    Collection<? extends DBSTablePartition> partitions = partitionContainer.getPartitions(monitor);
                    if (!CommonUtils.isEmpty(partitions)) {
                        DBSTablePartition partition = DBUtils.findObject(partitions, objectName);
                        if (partition != null) {
                            return partition;
                        }
                    }
                }
                log.debug("Object '" + objectName + "' not found in entity " + DBUtils.getObjectFullName(finalEntity, DBPEvaluationContext.UI));
                return null;
            }
        }
        return null;
    }

    public static DBPDataSourceContainer findDataSourceByObjectId(@NotNull DBPProject project, @NotNull String objectId) {
        return project.getDataSourceRegistry().getDataSource(objectId.split("/")[0]);
    }

    public static boolean isNullValue(@Nullable Object value) {
        return (value == null || (value instanceof DBDValue dbv && dbv.isNull()));
    }

    public static boolean isErrorValue(@Nullable Object value) {
        return value instanceof DBDValueError;
    }

    @Nullable
    public static Object makeNullValue(@NotNull DBCSession session, @NotNull DBDValueHandler valueHandler, @NotNull DBSTypedObject type) throws DBCException {
        return valueHandler.getValueFromObject(session, type, null, false, false);
    }

    @NotNull
    public static DBDAttributeBindingMeta getAttributeBinding(@NotNull DBSDataContainer dataContainer, @NotNull DBCSession session, @NotNull DBCAttributeMetaData attributeMeta) {
        return new DBDAttributeBindingMeta(dataContainer, session, attributeMeta);
    }

    @NotNull
    public static DBDAttributeBinding[] getAttributeBindings(@NotNull DBCSession session, @NotNull DBSDataContainer dataContainer, @NotNull DBCResultSetMetaData metaData) {
        List<? extends DBCAttributeMetaData> metaAttributes = metaData.getAttributes();
        int columnsCount = metaAttributes.size();
        DBDAttributeBinding[] bindings = new DBDAttributeBinding[columnsCount];
        for (int i = 0; i < columnsCount; i++) {
            bindings[i] = DBUtils.getAttributeBinding(dataContainer, session, metaAttributes.get(i));
        }
        return injectAndFilterAttributeBindings(session.getDataSource(), dataContainer, bindings, false);
    }

    public static DBDAttributeBinding[] injectAndFilterAttributeBindings(@NotNull DBPDataSource dataSource, @NotNull DBSDataContainer dataContainer, DBDAttributeBinding[] bindings, boolean filterAttributes) {
        // Add custom attributes
        DBVEntity vEntity = DBVUtils.getVirtualEntity(dataContainer, false);
        if (vEntity != null) {
            List<DBVEntityAttribute> customAttributes = DBVUtils.getCustomAttributes(vEntity);
            if (!CommonUtils.isEmpty(customAttributes)) {
                DBDAttributeBinding[] customBindings = new DBDAttributeBinding[customAttributes.size()];
                for (int i = 0; i < customAttributes.size(); i++) {
                    customBindings[i] = new DBDAttributeBindingCustom(
                        null,
                        dataContainer,
                        dataSource,
                        customAttributes.get(i),
                        bindings.length + i);
                }
                DBDAttributeBinding[] combinedAttrs = new DBDAttributeBinding[bindings.length + customBindings.length];
                System.arraycopy(bindings, 0, combinedAttrs, 0, bindings.length);
                System.arraycopy(customBindings, 0, combinedAttrs, bindings.length, customBindings.length);
                bindings = combinedAttrs;
            }
        }

        if (filterAttributes && dataContainer instanceof DBDAttributeFilter attributeFilter) {
            return attributeFilter.filterAttributeBindings(bindings);
        } else {
            return bindings;
        }
    }

    @Nullable
    public static Object getAttributeValue(
        @NotNull DBDAttributeBinding attribute,
        @NotNull DBDAttributeBinding[] allAttributes,
        @NotNull Object[] row
    ) {
        return getAttributeValue(attribute, allAttributes, row, null, false);
    }

    @Nullable
    public static Object getAttributeValue(
        @NotNull DBDAttributeBinding attribute,
        @NotNull DBDAttributeBinding[] allAttributes,
        @NotNull Object[] row,
        @Nullable int[] nestedIndexes,
        boolean retrieveDeepestCollectionElement
    ) {
        if (attribute.isCustom()) {
            try {
                return DBVUtils.executeExpression(((DBDAttributeBindingCustom) attribute).getEntityAttribute(), allAttributes, row);
            } catch (Exception e) {
                return new DBDValueError(e);
            }
        }

        final int depth = attribute.getLevel();
        final int index = attribute.getTopParent().getOrdinalPosition();

        if (depth == 0 && attribute != attribute.getTopParent()) {
            log.debug("Top-level attribute '" + attribute.getName()
                + "' has bad top-level parent: '" + attribute.getTopParent().getName() + "'");
            return null;
        }

        if (index >= row.length) {
            log.debug("Bad attribute '" + attribute.getName() + "' index: " + index + " is out of row values' bounds (" + row.length + ")");
            return null;
        }

        int remainingIndices = nestedIndexes != null ? nestedIndexes.length : 0;
        int remainingAttributes = depth;
        Object curValue = row[index];

        while (remainingAttributes > 0 || remainingIndices > 0 || retrieveDeepestCollectionElement) {
            if (curValue == null) {
                break;
            }

            if (!(curValue instanceof DBDCollection)) {
                if (remainingAttributes == 0) {
                    return DBDVoid.INSTANCE;
                }
                remainingAttributes -= 1;
                DBDAttributeBinding parent = Objects.requireNonNull(attribute.getParent(remainingAttributes));
                try {
                    curValue = parent.extractNestedValue(curValue, 0);
                } catch (DBException e) {
                    return new DBDValueError(e);
                }
            }

            while (curValue instanceof DBDCollection collection) {
                int itemIndex;
                if (remainingIndices > 0) {
                    itemIndex = nestedIndexes[nestedIndexes.length - remainingIndices];
                    remainingIndices -= 1;
                } else if (retrieveDeepestCollectionElement) {
                    itemIndex = 0;
                } else {
                    return curValue;
                }
                if (itemIndex >= collection.getItemCount()) {
                    return DBDVoid.INSTANCE;
                }
                curValue = collection.get(itemIndex);
            }

            if (retrieveDeepestCollectionElement && remainingAttributes == 0) {
                return curValue;
            }
        }

        return curValue;
    }

    public static void updateAttributeValue(
        @NotNull DBDValue rootValue,
        @NotNull DBDAttributeBinding attribute,
        @Nullable int[] nestedIndexes,
        @Nullable Object elementValue
    ) throws DBCException {
        final int depth = attribute.getLevel();

        if (depth == 0 && attribute != attribute.getTopParent()) {
            throw new DBCException("Top-level attribute '" + attribute.getName()
                      + "' has bad top-level parent: '" + attribute.getTopParent().getName() + "'");
        }

        Object curValue = rootValue;
        Object ownerValue = null;

        int remainingIndices = nestedIndexes != null ? nestedIndexes.length : 0;
        int remainingAttributes = depth;

        // Find owner value. Iterate thru all attr and col indexes to find leaf value.
        // Parent of leaf value is the owner value
        // Create intermediate values if needed
        while (remainingAttributes + remainingIndices > 0) {
            if (curValue == null) {
                break;
            }
            if (!(curValue instanceof DBDCollection)) {
                if (remainingAttributes == 0) {
                    throw new DBCException("Cannot update complex attribute: out of nested attributes");
                }
                remainingAttributes--;
                DBDAttributeBinding parent = Objects.requireNonNull(attribute.getParent(remainingAttributes));
                try {
                    ownerValue = curValue;
                    curValue = parent.extractNestedValue(curValue, 0);
                } catch (DBException e) {
                    throw new DBCException("Error extracting nested value", e);
                }
            }

            while (curValue instanceof DBDCollection collection) {
                if (remainingIndices <= 0) {
                    throw new DBCException("Out of indexes for collections");
                }
                int itemIndex = nestedIndexes[nestedIndexes.length - remainingIndices];
                remainingIndices--;
                if (itemIndex >= collection.getItemCount()) {
                    throw new DBCException("Item index out of range " + itemIndex + ">=" + collection.getItemCount());
                }
                ownerValue = curValue;
                curValue = collection.get(itemIndex);
            }
        }

        if (ownerValue == null) {
            throw new DBCException("Cannot determine owner value for update");
        } else if (ownerValue instanceof DBDCollection collection) {
            collection.setItem(nestedIndexes[nestedIndexes.length - 1], elementValue);
        } else if (ownerValue instanceof DBDComposite composite) {
            composite.setAttributeValue(attribute, elementValue);
        } else {
            throw new DBCException("Don't know how to update complex value '" + ownerValue + "' (" + ownerValue.getClass().getSimpleName() + ")");
        }
    }

    @NotNull
    public static DBDValueHandler findValueHandler(@NotNull DBCSession session, @NotNull DBSTypedObject column) {
        return findValueHandler(session.getDataSource(), session, column);
    }

    @NotNull
    public static DBDValueHandler findValueHandler(@NotNull DBPDataSource dataSource, @NotNull DBSTypedObject column) {
        return findValueHandler(dataSource, dataSource.getContainer(), column);
    }

    @NotNull
    public static DBDValueHandler findValueHandler(@Nullable DBPDataSource dataSource, @Nullable DBDFormatSettings preferences, @NotNull DBSTypedObject column) {
        DBDValueHandler valueHandler = null;
        // Get handler provider from datasource
        DBDValueHandlerProvider handlerProvider = getAdapter(DBDValueHandlerProvider.class, dataSource);
        if (handlerProvider != null) {
            valueHandler = handlerProvider.getValueHandler(dataSource, preferences, column);
            if (valueHandler != null) {
                return valueHandler;
            }
        }
        // Get handler provider from registry
        // Note: datasource CAN be null. For example when we import data from local files (csv)
        if (dataSource != null) {
            handlerProvider = DBWorkbench.getPlatform().getValueHandlerRegistry().getValueHandlerProvider(
                dataSource, column);
            if (handlerProvider != null) {
                valueHandler = handlerProvider.getValueHandler(dataSource, preferences, column);
            }
        }
        // Use default handler
        if (valueHandler == null) {
            if (preferences == null) {
                valueHandler = DefaultValueHandler.INSTANCE;
            } else {
                valueHandler = preferences.getDefaultValueHandler();
            }
        }
        return valueHandler;
    }

    /**
     * Identifying association is an association which associated entity's attributes are included into owner entity primary key. I.e. they
     * identifies entity.
     */
    public static boolean isIdentifyingAssociation(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntityAssociation association) throws DBException {
        if (!(association instanceof DBSEntityReferrer referrer)) {
            return false;
        }
        final DBSEntity refEntity = association.getAssociatedEntity();
        final DBSEntity ownerEntity = association.getParentObject();

        if (refEntity == ownerEntity) {
            // Can't migrate into itself
            return false;
        }
        // Migrating association is: if all referenced attributes are included in some unique key
        List<DBSEntityAttribute> ownAttrs = getEntityAttributes(monitor, referrer);
        Collection<? extends DBSEntityConstraint> constraints = ownerEntity.getConstraints(monitor);
        if (constraints != null) {
            boolean hasPrimaryKey = false;
            for (DBSEntityConstraint constraint : constraints) {
                if (constraint.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY) {
                    hasPrimaryKey = true;
                    break;
                }
            }
            for (DBSEntityConstraint constraint : constraints) {
                if (constraint instanceof DBSEntityReferrer eref &&
                    ((hasPrimaryKey && constraint.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY) ||
                        (!hasPrimaryKey && constraint.getConstraintType().isUnique()))) {
                    List<DBSEntityAttribute> constAttrs = getEntityAttributes(monitor, eref);

                    boolean included = true;
                    for (DBSEntityAttribute attr : ownAttrs) {
                        if (!constAttrs.contains(attr)) {
                            included = false;
                            break;
                        }
                    }
                    if (included) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Optional association is the one which can be set to NULL
     */
    public static boolean isOptionalAssociation(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntityAssociation association) throws DBException {
        if (!(association instanceof DBSEntityReferrer eref)) {
            return false;
        }

        for (DBSEntityAttributeRef ref : CommonUtils.safeCollection(eref.getAttributeReferences(monitor))) {
            if (ref.getAttribute() != null && !ref.getAttribute().isRequired()) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public static String getDefaultDataTypeName(@NotNull DBSObject objectContainer, DBPDataKind dataKind) {
        if (objectContainer instanceof DBPDataTypeProvider dtp) {
            return dtp.getDefaultDataTypeName(dataKind);
        } else {
            // Unsupported data kind
            return "?";
        }
    }

    @Nullable
    public static DBDAttributeBinding findBinding(@NotNull Collection<DBDAttributeBinding> bindings, @NotNull DBSAttributeBase attribute) {
        for (DBDAttributeBinding binding : bindings) {
            if (binding.matches(attribute, true)) {
                return binding;
            }
            List<DBDAttributeBinding> nestedBindings = binding.getNestedBindings();
            if (nestedBindings != null) {
                DBDAttributeBinding subBinding = findBinding(nestedBindings, attribute);
                if (subBinding != null) {
                    return subBinding;
                }
            }
        }
        return null;
    }

    @Nullable
    public static DBDAttributeBinding findBinding(@NotNull DBDAttributeBinding[] bindings, @Nullable DBSAttributeBase attribute) {
        if (attribute == null) {
            return null;
        }
        for (DBDAttributeBinding binding : bindings) {
            if (binding.matches(attribute, true)) {
                return binding;
            }
            List<DBDAttributeBinding> nestedBindings = binding.getNestedBindings();
            if (nestedBindings != null) {
                DBDAttributeBinding subBinding = findBinding(nestedBindings, attribute);
                if (subBinding != null) {
                    return subBinding;
                }
            }
        }
        return null;
    }

    @NotNull
    public static List<DBSEntityReferrer> getAttributeReferrers(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntityAttribute entityAttribute, boolean includeVirtual)
        throws DBException {
        DBSEntity entity = entityAttribute.getParentObject();
        Collection<? extends DBSEntityAssociation> associations = includeVirtual ? DBVUtils.getAllAssociations(monitor, entity) : entity.getAssociations(monitor);
        if (associations != null) {
            List<DBSEntityReferrer> refs = new ArrayList<>();
            for (DBSEntityAssociation fk : associations) {
                if (fk instanceof DBSEntityReferrer referrer && DBUtils.getConstraintAttribute(monitor, referrer, entityAttribute) != null) {
                    refs.add((DBSEntityReferrer) fk);
                }
            }
            return refs;
        }
        return Collections.emptyList();
    }

    @NotNull
    public static List<? extends DBSEntityAttribute> getBestTableIdentifier(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntity entity)
        throws DBException {
        if (entity instanceof DBSTable table && table.isView()) {
            return Collections.emptyList();
        }
        if (CommonUtils.isEmpty(entity.getAttributes(monitor))) {
            return Collections.emptyList();
        }

        List<DBSEntityConstraint> identifiers = new ArrayList<>();
        //List<DBSEntityConstraint> nonIdentifyingConstraints = null;

        // Check indexes
        if (entity instanceof DBSTable table) {
            try {
                Collection<? extends DBSTableIndex> indexes = table.getIndexes(monitor);
                if (!CommonUtils.isEmpty(indexes)) {
                    for (DBSTableIndex index : indexes) {
                        if (isIdentifierIndex(monitor, index)) {
                            identifiers.add(index);
                        }
                    }
                }
            } catch (DBException e) {
                log.debug(e);
            }
        }

        // Check constraints only if no unique indexes found
        if (identifiers.isEmpty()) {
            Collection<? extends DBSEntityConstraint> uniqueKeys = entity.getConstraints(monitor);
            if (uniqueKeys != null) {
                for (DBSEntityConstraint constraint : uniqueKeys) {
                    if (isIdentifierConstraint(monitor, constraint)) {
                        identifiers.add(constraint);
                    }/* else {
                        if (nonIdentifyingConstraints == null) nonIdentifyingConstraints = new ArrayList<>();
                        nonIdentifyingConstraints.add(constraint);
                    }*/
                }
            }
        }

        if (!identifiers.isEmpty()) {
            // Find PK or unique key
            DBSEntityConstraint uniqueId = null;
            //DBSEntityConstraint uniqueIndex = null;
            for (DBSEntityConstraint id : identifiers) {
                if (id instanceof DBSEntityReferrer referrer && id.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY) {
                    return getEntityAttributes(monitor, referrer);
                } else if (id.getConstraintType().isUnique()) {
                    uniqueId = id;
                } else if (id instanceof DBSTableIndex tableIndex && tableIndex.isUnique()) {
                    uniqueId = id;
                }
            }
            return uniqueId instanceof DBSEntityReferrer referrer ?
                getEntityAttributes(monitor, referrer)
                : Collections.<DBSTableColumn>emptyList();
        } else {
//            if (nonIdentifyingConstraints != null) {
//                return getEntityAttributes(monitor, (DBSEntityReferrer)nonIdentifyingConstraints.get(0));
//            }
            return Collections.emptyList();
        }
    }

    public static boolean isIdentifierIndex(DBRProgressMonitor monitor, DBSTableIndex index) throws DBException {
        if (!index.isUnique()) {
            return false;
        }
        List<? extends DBSTableIndexColumn> attrs = index.getAttributeReferences(monitor);
        if (attrs == null || attrs.isEmpty()) {
            return false;
        }
        for (DBSTableIndexColumn col : attrs) {
            if (col.getTableColumn() == null || !col.getTableColumn().isRequired()) {
                // Do not use indexes with NULL columns (because they are not actually unique: #424)
                return false;
            }
        }
        return true;
    }

    public static boolean isIdentifierConstraint(DBRProgressMonitor monitor, DBSEntityConstraint constraint) throws DBException {
        if (constraint.getConstraintType().isUnique()) {
            if (constraint instanceof DBSEntityReferrer referrer) {
                List<? extends DBSEntityAttributeRef> attrs = referrer.getAttributeReferences(monitor);
                if (attrs == null || attrs.isEmpty()) {
                    return false;
                }
                for (DBSEntityAttributeRef col : attrs) {
                    if (col.getAttribute() == null || !col.getAttribute().isRequired()) {
                        if (!constraint.getDataSource().getInfo().supportsNullableUniqueConstraints()) {
                            // Do not use constraints with NULL columns (because they are not actually unique: #424)
                            return false;
                        }
                    }
                }
                return true;
            } else {
                // Non-referrer constraint. It must identify rows somehow else. We don't care actually.
                return true;
            }
        }
        return false;
    }

    public static DBSEntityConstraint findEntityConstraint(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntity entity, @NotNull Collection<? extends DBSEntityAttribute> attributes)
        throws DBException {
        // Check constraints
        Collection<? extends DBSEntityConstraint> constraints = entity.getConstraints(monitor);
        if (!CommonUtils.isEmpty(constraints)) {
            for (DBSEntityConstraint constraint : constraints) {
                if (constraint instanceof DBSEntityReferrer referrer && referrerMatches(monitor, referrer, attributes)) {
                    return constraint;
                }
            }
        }
        if (entity instanceof DBSTable table) {
            Collection<? extends DBSTableIndex> indexes = table.getIndexes(monitor);
            if (!CommonUtils.isEmpty(indexes)) {
                for (DBSTableIndex index : indexes) {
                    if (index.isUnique() && referrerMatches(monitor, index, attributes)) {
                        return index;
                    }
                }
            }
        }
        return null;
    }

    public static boolean referrerMatches(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntityReferrer referrer, @NotNull Collection<? extends DBSEntityAttribute> attributes) throws DBException {
        final List<? extends DBSEntityAttributeRef> refs = referrer.getAttributeReferences(monitor);
        if (refs != null && !refs.isEmpty() && attributes.size() == refs.size()) {
            Iterator<? extends DBSEntityAttribute> attrIterator = attributes.iterator();
            for (DBSEntityAttributeRef ref : refs) {
                if (!attrIterator.hasNext()) {
                    return false;
                }
                if (ref.getAttribute() == null ||
                    !CommonUtils.equalObjects(ref.getAttribute().getName(), attrIterator.next().getName())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @NotNull
    public static List<DBSEntityAttribute> getEntityAttributes(@NotNull DBRProgressMonitor monitor, @Nullable DBSEntityReferrer referrer) {
        try {
            if (referrer instanceof DBVEntityConstraint ec && ec.isUseAllColumns()) {
                DBSEntity realEntity = ec.getEntity().getRealEntity(monitor);
                if (realEntity != null) {
                    Collection<? extends DBSEntityAttribute> attributes = realEntity.getAttributes(monitor);
                    return attributes == null ? Collections.emptyList() : new ArrayList<>(attributes);
                }
            }
        } catch (DBException e) {
            log.error("Error discovering virtual constraint attributes", e);
        }
        Collection<? extends DBSEntityAttributeRef> constraintColumns = null;
        if (referrer != null) {
            try {
                constraintColumns = referrer.getAttributeReferences(monitor);
            } catch (DBException e) {
                log.warn("Error reading reference attributes", e);
            }
        }
        if (constraintColumns == null) {
            return Collections.emptyList();
        }
        List<DBSEntityAttribute> attributes = new ArrayList<>(constraintColumns.size());
        for (DBSEntityAttributeRef column : constraintColumns) {
            final DBSEntityAttribute attribute = column.getAttribute();
            if (attribute != null) {
                attributes.add(attribute);
            }
        }
        return attributes;
    }

    @Nullable
    public static DBSEntityAttributeRef getConstraintAttribute(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntityReferrer constraint, @NotNull DBSEntityAttribute tableColumn) throws DBException {
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

    @Nullable
    public static DBSEntityAttributeRef getConstraintAttribute(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntityReferrer constraint, @NotNull String columnName) throws DBException {
        Collection<? extends DBSEntityAttributeRef> columns = constraint.getAttributeReferences(monitor);
        if (columns != null) {
            for (DBSEntityAttributeRef constraintColumn : columns) {
                final DBSEntityAttribute attribute = constraintColumn.getAttribute();
                if (attribute != null && attribute.getName().equals(columnName)) {
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
    @Nullable
    public static DBSEntityAttribute getReferenceAttribute(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSEntityAssociation association,
        @NotNull DBSEntityAttribute tableColumn,
        boolean reference) throws DBException {
        final DBSEntityConstraint refConstr = association.getReferencedConstraint();
        if (association instanceof DBSEntityReferrer eRef && refConstr instanceof DBSEntityReferrer rRef) {
            final Collection<? extends DBSEntityAttributeRef> ownAttrs = eRef.getAttributeReferences(monitor);
            final Collection<? extends DBSEntityAttributeRef> refAttrs = rRef.getAttributeReferences(monitor);
            if (ownAttrs == null || refAttrs == null || ownAttrs.size() != refAttrs.size()) {
                log.error("Invalid internal state of referrer association");
                return null;
            }
            final Iterator<? extends DBSEntityAttributeRef> ownIterator = ownAttrs.iterator();
            final Iterator<? extends DBSEntityAttributeRef> refIterator = refAttrs.iterator();
            while (ownIterator.hasNext()) {
                DBSEntityAttributeRef ownAttr = ownIterator.next();
                if (reference) {
                    if (ownAttr instanceof DBSTableForeignKeyColumn fkc && fkc.getReferencedColumn() == tableColumn) {
                        return refIterator.next().getAttribute();
                    }
                } else if (ownAttr.getAttribute() == tableColumn) {
                    return refIterator.next().getAttribute();
                }
                refIterator.next();
            }
        }
        return null;
    }

    @NotNull
    public static DBCStatement makeStatement(
        @Nullable DBCExecutionSource executionSource,
        @NotNull DBCSession session,
        @NotNull DBCStatementType statementType,
        @NotNull String query,
        long offset,
        long maxRows) throws DBCException {
        SQLQuery sqlQuery = new SQLQuery(session.getDataSource(), query);
        return makeStatement(
            executionSource,
            session,
            statementType,
            sqlQuery,
            offset,
            maxRows);
    }

    @NotNull
    public static DBCStatement makeStatement(
        @Nullable DBCExecutionSource executionSource,
        @NotNull DBCSession session,
        @NotNull DBCStatementType statementType,
        @NotNull SQLQuery sqlQuery,
        long offset,
        long maxRows)
        throws DBCException {
        // We need to detect whether it is a plain select statement
        // or some DML. For DML statements we mustn't set limits
        // because it sets update rows limit [SQL Server]
        boolean selectQuery = sqlQuery.getType() == SQLQueryType.SELECT && sqlQuery.isPlainSelect();
        final boolean hasLimits = (offset > 0 || selectQuery) && maxRows > 0;
        boolean isShouldSetLimit = true;
        // This is a flag for any potential SELECT query
        boolean possiblySelect = sqlQuery.getType() == SQLQueryType.SELECT || sqlQuery.getType() == SQLQueryType.UNKNOWN;
        boolean limitAffectsDML = Boolean.TRUE.equals(session.getDataSource().getDataSourceFeature(DBPDataSource.FEATURE_LIMIT_AFFECTS_DML));

        DBCQueryTransformer limitTransformer = null, fetchAllTransformer = null;
        if (selectQuery) {
            DBCQueryTransformProvider transformProvider = DBUtils.getAdapter(DBCQueryTransformProvider.class, session.getDataSource());
            if (transformProvider != null) {
                if (transformProvider instanceof DBCQueryTransformProviderExt qtp) {
                    isShouldSetLimit = qtp.isLimitApplicableTo(sqlQuery);
                }
                if (hasLimits) {
                    if (session.getDataSource().getContainer().getPreferenceStore().getBoolean(ModelPreferences.RESULT_SET_MAX_ROWS_USE_SQL) ||
                        (transformProvider instanceof DBCQueryTransformProviderExt qtp && qtp.isForceTransform(session, sqlQuery))) {
                        limitTransformer = transformProvider.createQueryTransformer(DBCQueryTransformType.RESULT_SET_LIMIT);
                    }
                } else {
                    fetchAllTransformer = transformProvider.createQueryTransformer(DBCQueryTransformType.FETCH_ALL_TABLE);
                }
            }
        }
        // Check that transformers are applicable
        if (limitTransformer instanceof DBCQueryTransformerExt lQT && !lQT.isApplicableTo(sqlQuery)) {
            limitTransformer = null;
        }
        if (fetchAllTransformer instanceof DBCQueryTransformerExt faQT && !faQT.isApplicableTo(sqlQuery)) {
            fetchAllTransformer = null;
        }

        // Transform query
        boolean doScrollable = (offset > 0);
        String queryText;
        try {
            if (hasLimits && limitTransformer != null) {
                limitTransformer.setParameters(offset, maxRows);
                queryText = limitTransformer.transformQueryString(sqlQuery);
                doScrollable = false;
            } else if (fetchAllTransformer != null) {
                queryText = fetchAllTransformer.transformQueryString(sqlQuery);
            } else {
                queryText = sqlQuery.getText();
            }
        } catch (Exception e) {
            log.debug("Error transforming SQL query", e);
            queryText = sqlQuery.getText();
        }

        DBCStatement dbStat = statementType == DBCStatementType.SCRIPT ?
            createStatement(session, queryText, doScrollable) :
            makeStatement(session, queryText, doScrollable);
        dbStat.setStatementSource(executionSource);

        if (offset > 0 || hasLimits || (possiblySelect && maxRows > 0 && !limitAffectsDML)) {
            if (limitTransformer == null) {
                if (isShouldSetLimit) {
                    // Set explicit limit - it is safe because we pretty sure that this is a plain SELECT query
                    dbStat.setLimit(offset, maxRows);
                }
            } else {
                limitTransformer.transformStatement(dbStat, 0);
            }
        } else if (fetchAllTransformer != null) {
            fetchAllTransformer.transformStatement(dbStat, 0);
        }

        return dbStat;
    }

    @NotNull
    public static DBCStatement createStatement(
        @NotNull DBCSession session,
        @NotNull String query,
        boolean scrollable) throws DBCException {
        SQLDialect dialect = SQLUtils.getDialectFromObject(session.getDataSource());

        DBCStatementType statementType = DBCStatementType.SCRIPT;
        query = SQLUtils.makeUnifiedLineFeeds(session.getDataSource(), query);
        if (SQLUtils.isExecQuery(dialect, query)) {
            statementType = DBCStatementType.EXEC;
            query = dialect.formatStoredProcedureCall(session.getDataSource(), query);
        }
        return session.prepareStatement(
            statementType,
            query,
            scrollable && session.getDataSource().getInfo().supportsResultSetScroll(),
            false,
            false);
    }

    @NotNull
    public static DBCStatement makeStatement(
        @NotNull DBCSession session,
        @NotNull String query,
        boolean scrollable) throws DBCException {
        DBCStatementType statementType = DBCStatementType.QUERY;
        // Normalize query
        query = SQLUtils.makeUnifiedLineFeeds(session.getDataSource(), query);

        if (SQLUtils.isExecQuery(SQLUtils.getDialectFromObject(session.getDataSource()), query)) {
            statementType = DBCStatementType.EXEC;
        }

        return session.prepareStatement(
            statementType,
            query,
            scrollable && session.getDataSource().getInfo().supportsResultSetScroll(),
            false,
            false);
    }

    public static void fireObjectUpdate(@NotNull DBSObject object) {
        fireObjectUpdate(object, null, null);
    }

    public static void fireObjectUpdate(DBSObject object, boolean enabled) {
        final DBPDataSourceContainer container = getContainer(object);
        if (container != null) {
            container.fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, object, enabled));
        }
    }

    public static void fireObjectUpdate(DBSObject object, @Nullable Map<String, Object> options, @Nullable Object data) {
        final DBPDataSourceContainer container = getContainer(object);
        if (container != null) {
            DBPEvent event = new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, object, data);
            event.setOptions(options);
            container.fireEvent(event);
        }
    }

    public static void fireObjectUpdate(DBSObject object, @Nullable Object data) {
        fireObjectUpdate(object, null, data);
    }

    public static void fireObjectAdd(DBSObject object, Map<String, Object> options) {
        final DBPDataSourceContainer container = getContainer(object);
        if (container != null) {
            DBPEvent event = new DBPEvent(DBPEvent.Action.OBJECT_ADD, object);
            event.setOptions(options);
            container.fireEvent(event);
        }
    }

    public static void fireObjectRemove(DBSObject object) {
        final DBPDataSourceContainer container = getContainer(object);
        if (container != null) {
            container.fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_REMOVE, object));
        }
    }

    /**
     * Fire event that the object was selected or unselected
     */
    public static void fireObjectSelect(@Nullable DBSObject object, boolean select, @Nullable DBCExecutionContext context) {
        final DBPDataSourceContainer container = getContainer(object);
        if (container != null) {
            container.fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_SELECT, object, select, context));
        }
    }

    /**
     * Refresh object in UI
     */
    public static void fireObjectRefresh(DBSObject object) {
        // Select with true parameter is the same as refresh
        fireObjectSelect(object, true, null);
    }

    @NotNull
    public static String getObjectUniqueName(@NotNull DBSObject object) {
        if (object instanceof DBPUniqueObject uo) {
            return uo.getUniqueName();
        } else {
            return object.getName();
        }
    }

    @Nullable
    public static DBSDataType findBestDataType(@NotNull Collection<? extends DBSDataType> allTypes, @NotNull String... typeNames) {
        for (String testType : typeNames) {
            for (DBSDataType dataType : allTypes) {
                if (dataType.getName().equalsIgnoreCase(testType)) {
                    return dataType;
                }
            }
        }
        return null;
    }

    @NotNull
    public static <T extends DBSTypedObject> T getMoreCommonType(@NotNull T t1, @NotNull T t2) {
        if (!t1.equals(t2) && t1.getDataKind().getCommonality() < t2.getDataKind().getCommonality()) {
            return t2;
        }
        return t1;
    }

    @Nullable
    public static DBSDataType resolveDataType(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @NotNull String fullTypeName)
        throws DBException {
        DBPDataTypeProvider dataTypeProvider = getAdapter(DBPDataTypeProvider.class, dataSource);
        if (dataTypeProvider == null) {
            // NoSuchElementException data type provider
            return null;
        }
        return dataTypeProvider.resolveDataType(monitor, fullTypeName);
    }

    @Nullable
    public static DBSDataType getLocalDataType(
        @NotNull DBPDataSource dataSource,
        @NotNull String fullTypeName) {
        DBPDataTypeProvider dataTypeProvider = getAdapter(DBPDataTypeProvider.class, dataSource);
        if (dataTypeProvider == null) {
            return null;
        }
        return dataTypeProvider.getLocalDataType(fullTypeName);
    }

    public static DBSObject getPublicObject(@Nullable DBSObject object) {
        if (object instanceof DBPDataSourceContainer) {
            return object.getDataSource();
        } else {
            return object;
        }
    }

    /**
     * Returns DBPDataSourceContainer from DBPDataSource or object itself otherwise
     */
    public static DBSObject getPublicObjectContainer(@NotNull DBSObject object) {
        if (object instanceof DBPDataSource ds) {
            return ds.getContainer();
        } else {
            return object;
        }
    }

    @Nullable
    public static DBPDataSourceContainer getContainer(@Nullable DBSObject object) {
        if (object == null) {
            return null;
        }
        if (object instanceof DBPDataSourceContainer dsc) {
            return dsc;
        }
        final DBPDataSource dataSource = object.getDataSource();
        return dataSource == null ? null : dataSource.getContainer();
    }

    @Nullable
    public static DBPDataSourceRegistry getObjectRegistry(@NotNull DBSObject object) {
        DBPDataSourceContainer container;
        if (object instanceof DBPDataSourceContainer dsc) {
            container = dsc;
        } else {
            DBPDataSource dataSource = object.getDataSource();
            if (dataSource == null) {
                return null;
            }
            container = dataSource.getContainer();
        }
        return container.getRegistry();
    }


    @Nullable
    public static DBPProject getObjectOwnerProject(DBSObject object) {
        var registry = getObjectRegistry(object);
        return registry == null ? null : registry.getProject();
    }

    @NotNull
    public static String getObjectShortName(Object object) {
        String strValue;
        if (object instanceof DBPNamedObject no) {
            strValue = no.getName();
        } else {
            strValue = String.valueOf(object);
        }
        return strValue;
    }

    @NotNull
    public static String getObjectFullName(@NotNull DBPNamedObject object, DBPEvaluationContext context) {
        if (object instanceof DBPQualifiedObject qo) {
            return qo.getFullyQualifiedName(context);
        } else if (object instanceof DBSObject dbso && dbso.getDataSource() != null) {
            return getObjectFullName(dbso.getDataSource(), object, context, DBPAttributeReferencePurpose.UNSPECIFIED);
        } else {
            return object.getName();
        }
    }

    /**
     * Get the full name of the object.
     *
     * @param dataSource container
     * @param object     object to get name of
     * @param context    evaluation context
     * @return full name of the object
     */
    @NotNull
    public static String getObjectFullName(
        @NotNull DBPDataSource dataSource,
        @NotNull DBPNamedObject object,
        @NotNull DBPEvaluationContext context) {
        return getObjectFullName(dataSource, object, context, DBPAttributeReferencePurpose.UNSPECIFIED);
    }

    /**
     * Get the full name of the object.
     *
     * @param dataSource container
     * @param object     object to get name of
     * @param context    evaluation context
     * @param purpose    to use object name to
     * @return full name of the object
     */
    @NotNull
    public static String getObjectFullName(
        @NotNull DBPDataSource dataSource,
        @NotNull DBPNamedObject object,
        @NotNull DBPEvaluationContext context,
        @NotNull DBPAttributeReferencePurpose purpose) {
        if (object instanceof DBDAttributeBinding binding) {
            return binding.getFullyQualifiedName(context, purpose);
        } else if (object instanceof DBPQualifiedObject qo) {
            return qo.getFullyQualifiedName(context);
        } else {
            return getQuotedIdentifier(dataSource, object.getName());
        }
    }

    @NotNull
    public static String getFullTypeName(@NotNull DBSTypedObject typedObject) {
        DBSObject structObject = getFromObject(typedObject);
        DBPDataSource dataSource = structObject == null ? null : structObject.getDataSource();
        return getFullTypeName(dataSource, typedObject);
    }

    @NotNull
    public static String getFullTypeName(DBPDataSource dataSource, @NotNull DBSTypedObject typedObject) {
        String typeName = typedObject.getTypeName();
        if (CommonUtils.isEmpty(typeName)) {
            // No answer from the driver side
            return "<" + ModelMessages.dbutils_type_name_unknown + ">";
        }
        String typeModifiers = SQLUtils.getColumnTypeModifiers(dataSource, typedObject, typeName, typedObject.getDataKind());
        return typeModifiers == null ? typeName : (typeName + typeModifiers);
    }

    public static void releaseValue(@Nullable Object value) {
        if (value instanceof DBDValue dbdv) {
            dbdv.release();
        }
    }

    public static void resetValue(@Nullable Object value) {
        if (value instanceof DBDContent dbdc) {
            dbdc.resetContents();
        }
    }

    @NotNull
    public static DBCLogicalOperator[] getAttributeOperators(DBSTypedObject attribute) {
        if (attribute instanceof DBSTypedObjectEx typedObjectEx) {
            DBSDataType dataType = typedObjectEx.getDataType();
            if (dataType != null) {
                return dataType.getSupportedOperators(attribute);
            }
        }
        return getDefaultOperators(attribute);
    }

    @NotNull
    public static DBCLogicalOperator[] getDefaultOperators(DBSTypedObject attribute) {
        List<DBCLogicalOperator> operators = new ArrayList<>();
        DBPDataKind dataKind = attribute.getDataKind();
        if (attribute instanceof DBSAttributeBase && !((DBSAttributeBase) attribute).isRequired()) {
            operators.add(DBCLogicalOperator.IS_NULL);
            operators.add(DBCLogicalOperator.IS_NOT_NULL);
        }
        if (dataKind == DBPDataKind.BOOLEAN || dataKind == DBPDataKind.ROWID || dataKind == DBPDataKind.OBJECT || dataKind == DBPDataKind.BINARY) {
            operators.add(DBCLogicalOperator.EQUALS);
            operators.add(DBCLogicalOperator.NOT_EQUALS);
        } else if (dataKind == DBPDataKind.NUMERIC || dataKind == DBPDataKind.DATETIME || dataKind == DBPDataKind.STRING) {
            operators.add(DBCLogicalOperator.EQUALS);
            operators.add(DBCLogicalOperator.NOT_EQUALS);
            operators.add(DBCLogicalOperator.GREATER);
            //operators.add(DBCLogicalOperator.GREATER_EQUALS);
            operators.add(DBCLogicalOperator.LESS);
            //operators.add(DBCLogicalOperator.LESS_EQUALS);
            operators.add(DBCLogicalOperator.IN);
        }
        if (dataKind == DBPDataKind.STRING) {
            operators.add(DBCLogicalOperator.LIKE);
        }
        return operators.toArray(new DBCLogicalOperator[0]);
    }

    public static Object getRawValue(Object value) {
        if (value instanceof DBDValue) {
            return ((DBDValue) value).getRawValue();
        } else {
            return value;
        }
    }

    public static DBSTableIndex findAttributeIndex(DBRProgressMonitor monitor, DBSEntityAttribute attribute) throws DBException {
        DBSEntity entity = attribute.getParentObject();
        if (entity instanceof DBSTable) {
            Collection<? extends DBSTableIndex> indexes = ((DBSTable) entity).getIndexes(monitor);
            if (!CommonUtils.isEmpty(indexes)) {
                for (DBSTableIndex index : indexes) {
                    if (getConstraintAttribute(monitor, index, attribute) != null) {
                        return index;
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    public static DBCTransactionManager getTransactionManager(@Nullable DBCExecutionContext executionContext) {
        if (executionContext != null && executionContext.isConnected()) {
            return getAdapter(DBCTransactionManager.class, executionContext);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T> Class<T> getDriverClass(@NotNull DBPDataSource dataSource, @NotNull String className) throws ClassNotFoundException {
        return (Class<T>) Class.forName(className, true, dataSource.getContainer().getDriver().getClassLoader());
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T extends DBCSession> T openMetaSession(@NotNull DBRProgressMonitor monitor, @NotNull DBSObject object, @NotNull String task) throws DBCException {
        DBCExecutionContext defaultContext = getOrOpenDefaultContext(object, true);
        if (defaultContext == null) {
            throw new DBCException("Default context not found");
        }
        return (T) defaultContext.openSession(monitor, DBCExecutionPurpose.META, task);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T extends DBCSession> T openMetaSession(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSource dataSource, @NotNull String task) throws DBCException {
        return (T) dataSource.getDefaultInstance().getDefaultContext(monitor, true).openSession(monitor, DBCExecutionPurpose.META, task);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T extends DBCSession> T openUtilSession(@NotNull DBRProgressMonitor monitor, @NotNull DBSObject object, @NotNull String task) throws DBCException {
        DBCExecutionContext defaultContext = getOrOpenDefaultContext(object, true);
        if (defaultContext == null) {
            throw new DBCException("Default context not found");
        }
        return (T) getOrOpenDefaultContext(object, false).openSession(monitor, DBCExecutionPurpose.UTIL, task);
    }

    @Nullable
    public static DBSObject getFromObject(Object object) {
        if (object == null) {
            return null;
        } else if (object instanceof DBSWrapper) {
            return ((DBSWrapper) object).getObject();
        } else if (object instanceof DBSObject) {
            return (DBSObject) object;
        } else {
            return RuntimeUtils.getObjectAdapter(object, DBSObject.class);
        }
    }

    public static boolean isAtomicParameter(Object o) {
        return o == null || o instanceof CharSequence || o instanceof Number || o instanceof java.util.Date || o instanceof Boolean;
    }

    @NotNull
    public static DBSObject getDefaultOrActiveObject(@NotNull DBSInstance object) {
        DBCExecutionContext defaultContext = getDefaultContext(object, true);
        DBSObject activeObject = defaultContext == null ? null : getActiveInstanceObject(defaultContext);
        return activeObject == null ? object.getDataSource() : activeObject;
    }

    @Nullable
    public static DBSObject getActiveInstanceObject(@NotNull DBCExecutionContext executionContext) {
        return getSelectedObject(executionContext);
    }

    @Nullable
    public static DBSObject getSelectedObject(@NotNull DBCExecutionContext context) {
        DBCExecutionContextDefaults<?,?> contextDefaults = context.getContextDefaults();
        if (contextDefaults != null) {
            DBSSchema defaultSchema = contextDefaults.getDefaultSchema();
            if (defaultSchema != null) {
                return defaultSchema;
            }
            DBSCatalog defaultCatalog = contextDefaults.getDefaultCatalog();
            if (defaultCatalog != null) {
                return defaultCatalog;
            }
        }
        return null;
    }

    @Nullable
    public static <T> T getSelectedObject(@NotNull DBCExecutionContext context, Class<T> theClass) {
        DBSObject selectedObject = getSelectedObject(context);
        if (theClass.isInstance(selectedObject)) {
            return theClass.cast(selectedObject);
        }
        return null;
    }

    /**
     * Sometimes it is all info that we know about datasource - default catalog or default schema. This method returns an array of them.
     *
     * @param context execution context contains info about default table containers
     * @return array of the default table containers. First - default catalog, second - default schema. If they exist.
     */
    @NotNull
    public static DBSObject[] getSelectedObjects(@Nullable DBCExecutionContext context) {
        if (context == null || context.getContextDefaults() == null) {
            return new DBSObject[0];
        }

        DBCExecutionContextDefaults<?, ?> contextDefaults = context.getContextDefaults();
        DBSCatalog defaultCatalog = contextDefaults.getDefaultCatalog();
        DBSSchema defaultSchema = contextDefaults.getDefaultSchema();
        if (defaultCatalog != null && defaultSchema != null) {
            return new DBSObject[]{defaultCatalog, defaultSchema};
        } else if (defaultCatalog != null) {
            return new DBSObject[]{defaultCatalog};
        } else if (defaultSchema != null) {
            return new DBSObject[]{defaultSchema};
        } else {
            return new DBSObject[0];
        }
    }

    /**
     * Update execution context default schema and catalog
     */
    public static void refreshContextDefaultsAndReflect(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContextDefaults<?,?> contextDefaults,
        @Nullable DBCExecutionContext context
    ) {
        try {
            DBSCatalog defaultCatalog = contextDefaults.getDefaultCatalog();
            DBSSchema defaultSchema = contextDefaults.getDefaultSchema();
            if (contextDefaults.refreshDefaults(monitor, false)) {
                fireObjectSelectionChange(defaultCatalog, contextDefaults.getDefaultCatalog(), context);
                fireObjectSelectionChange(defaultSchema, contextDefaults.getDefaultSchema(), context);
            }
        } catch (Exception e) {
            log.debug(e);
        }
    }

    /**
     * Fire event to unselecting the oldDefaultObject and selecting the newDefaultObject
     */
    public static void fireObjectSelectionChange(
        @Nullable DBSObject oldDefaultObject,
        @Nullable DBSObject newDefaultObject,
        @Nullable DBCExecutionContext context
    ) {
        if (oldDefaultObject != newDefaultObject) {
            if (oldDefaultObject != null) {
                DBUtils.fireObjectSelect(oldDefaultObject, false, context);
            }
            if (newDefaultObject != null) {
                DBUtils.fireObjectSelect(newDefaultObject, true, context);
            }
        }
    }

    public static DBSObjectContainer getChangeableObjectContainer(
        DBCExecutionContextDefaults<?,?> contextDefaults,
        DBSObjectContainer root,
        Class<? extends DBSObject> childType
    ) {
        if (contextDefaults == null) {
            return null;
        }
        if (childType == DBSCatalog.class && contextDefaults.supportsCatalogChange()) {
            return root;
        }
        if (childType == DBSSchema.class && contextDefaults.supportsSchemaChange()) {
            DBSCatalog defaultCatalog = contextDefaults.getDefaultCatalog();
            if (defaultCatalog != null) {
                return defaultCatalog;
            } else {
                return root;
            }
        }
        return null;
    }

    public static boolean isHiddenObject(Object object) {
        return object instanceof DBPHiddenObject ho && ho.isHidden();
    }

    public static boolean isSystemObject(Object object) {
        return object instanceof DBPSystemObject so && so.isSystem();
    }

    public static boolean isVirtualObject(Object object) {
        return object instanceof DBPVirtualObject vo && vo.isVirtual();
    }

    public static boolean isInheritedObject(Object object) {
        return object instanceof DBPInheritedObject io && io.isInherited();
    }

    public static DBDPseudoAttribute getRowIdAttribute(DBSEntity entity) {
        if (entity instanceof DBDPseudoAttributeContainer pac) {
            try {
                return DBDPseudoAttribute.getAttribute(
                    pac.getPseudoAttributes(),
                    DBDPseudoAttributeType.ROWID);
            } catch (DBException e) {
                log.warn("Can't get pseudo attributes for '" + entity.getName() + "'", e);
            }
        }
        return null;
    }

    public static boolean isDynamicAttribute(DBSAttributeBase attr) {
        return attr instanceof DBSAttributeDynamic da && da.isDynamicAttribute();
    }

    public static boolean isRowIdAttribute(DBSEntityAttribute attr) {
        DBDPseudoAttribute rowIdAttribute = getRowIdAttribute(attr.getParentObject());
        return rowIdAttribute != null && rowIdAttribute.getName().equals(attr.getName());
    }

    public static DBDPseudoAttribute getPseudoAttribute(DBSEntity entity, String attrName) {
        if (entity instanceof DBDPseudoAttributeContainer pac) {
            try {
                DBDPseudoAttribute[] pseudoAttributes = pac.getPseudoAttributes();
                if (pseudoAttributes != null) {
                    for (DBDPseudoAttribute pa : pseudoAttributes) {
                        String attrId = pa.getAlias();
                        if (CommonUtils.isEmpty(attrId)) {
                            attrId = pa.getName();
                        }
                        if (attrId.equals(attrName)) {
                            return pa;
                        }
                    }
                }
            } catch (DBException e) {
                log.warn("Can't get pseudo attributes for '" + entity.getName() + "'", e);
            }
        }
        return null;
    }

    public static boolean isPseudoAttribute(DBSAttributeBase attr) {
        return attr instanceof DBDAttributeBinding ab && ab.isPseudoAttribute();
    }

    public static <TYPE extends DBPNamedObject> Comparator<TYPE> nameComparator() {
        return Comparator.comparing(DBPNamedObject::getName);
    }

    public static <TYPE extends DBPNamedObject> Comparator<TYPE> nameComparatorIgnoreCase() {
        return (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName());
    }

    public static Comparator<? super DBPObjectWithOrdinalPosition> orderComparator() {
        return Comparator.comparingInt(DBPObjectWithOrdinalPosition::getOrdinalPosition);
    }

    public static <T extends DBPNamedObject> List<T> makeOrderedObjectList(@NotNull Collection<T> objects) {
        List<T> ordered = new ArrayList<>(objects);
        orderObjects(ordered);
        return ordered;
    }

    public static <T extends DBPNamedObject> List<T> makeOrderedObjectList(@NotNull T[] objects) {
        List<T> ordered = new ArrayList<>();
        Collections.addAll(ordered, objects);
        orderObjects(ordered);
        return ordered;
    }

    public static <T extends DBPNamedObject> void orderObjects(@NotNull List<T> objects) {
        objects.sort((o1, o2) -> {
            String name1 = o1.getName();
            String name2 = o2.getName();
            return name1 == null && name2 == null ? 0 :
                (name1 == null ? -1 :
                    (name2 == null ? 1 : name1.compareTo(name2)));
        });
    }

    /**
     * Returns client application identifier that contains application name, version and current connection purpose
     *
     * @param container data source container
     * @param context   execution context
     * @param purpose   if null, purpose will not be included
     * @return the client application name built according to passed arguments
     */
    public static String getClientApplicationName(
        @NotNull DBPDataSourceContainer container,
        @Nullable DBCExecutionContext context,
        @Nullable String purpose
    ) {
        return getClientApplicationName(container, context, purpose, true);
    }

    /**
     * Returns client application identifier that contains application name and
     * optionally version and current connection purpose
     *
     * @param container  data source container
     * @param context    execution context
     * @param purpose    if null, purpose will not be included
     * @param addVersion if false version will not be included
     * @return the client application name built according to passed arguments
     */
    public static String getClientApplicationName(
        @NotNull DBPDataSourceContainer container,
        @Nullable DBCExecutionContext context,
        @Nullable String purpose,
        boolean addVersion
    ) {
        if (container.getPreferenceStore().getBoolean(ModelPreferences.META_CLIENT_NAME_OVERRIDE)) {
            String appName = container.getPreferenceStore().getString(ModelPreferences.META_CLIENT_NAME_VALUE);
            IVariableResolver cVarResolver = container.getVariablesResolver(false);
            return GeneralUtils.replaceVariables(appName, name -> switch (name) {
                case DBConstants.VAR_CONTEXT_NAME -> context == null ? null : context.getContextName();
                case DBConstants.VAR_CONTEXT_ID -> context == null ? null : String.valueOf(context.getContextId());
                default -> cVarResolver.get(name);
            });
        }
        final String productTitle = addVersion ? GeneralUtils.getProductTitle() : GeneralUtils.getProductName();
        return purpose == null ? productTitle : productTitle + " - " + purpose;
    }

    public static DBSInstance getObjectOwnerInstance(DBSObject object) {
        if (object == null) {
            return null;
        }
        for (DBSObject p = object; p != null; p = p.getParentObject()) {
            if (p instanceof DBSInstance) {
                return (DBSInstance) p;
            }
        }
        DBPDataSource dataSource = object.getDataSource();
        if (dataSource == null || dataSource.isConnectionRefreshing()) {
            return null;
        }
        return dataSource.getDefaultInstance();
    }

    public static DBCExecutionContext getDefaultContext(DBSObject object, boolean meta) {
        if (object == null) {
            return null;
        }
        DBSInstance instance = getObjectOwnerInstance(object);
        if (instance == null
            || (instance instanceof DBSInstanceLazy instanceLazy && !instanceLazy.isInstanceConnected())
            || (instance.getDataSource() != null && instance.getDataSource().isConnectionRefreshing())) {
            return null;
        }

        return instance.getDefaultContext(new VoidProgressMonitor(), meta);
    }

    public static DBCExecutionContext getOrOpenDefaultContext(DBSObject object, boolean meta) throws DBCException {
        DBCExecutionContext context = DBUtils.getDefaultContext(object, meta);
        if (context == null) {
            // Not connected - try to connect
            DBSInstance ownerInstance = DBUtils.getObjectOwnerInstance(object);
            if (ownerInstance instanceof DBSInstanceLazy instanceLazy && !instanceLazy.isInstanceConnected()) {
                if (!RuntimeUtils.runTask(monitor -> {
                        try {
                            instanceLazy.checkInstanceConnection(monitor);
                        } catch (DBException e) {
                            throw new InvocationTargetException(e);
                        }
                    }, "Initiate instance connection",
                    object.getDataSource().getContainer().getPreferenceStore().getInt(ModelPreferences.CONNECTION_OPEN_TIMEOUT))) {
                    throw new DBCException("Timeout while opening database connection");
                }
                context = DBUtils.getDefaultContext(object, meta);
            }
        }
        return context;
    }

    public static List<DBPDataSourceRegistry> getAllRegistries(boolean forceLoad) {
        List<DBPDataSourceRegistry> result = new ArrayList<>();
        for (DBPProject project : DBWorkbench.getPlatform().getWorkspace().getProjects()) {
            if (forceLoad || (project.isOpen() && project.isRegistryLoaded())) {
                project.ensureOpen();

                DBPDataSourceRegistry registry = project.getDataSourceRegistry();
                if (registry != null) {
                    result.add(registry);
                }
            }
        }
        return result;
    }

    /**
     * Find data source in all available registries.
     * Deprecated. Triggering all projects open may cause issues (especially when they are secured)
     */
    @Deprecated
    public static DBPDataSourceContainer findDataSource(String dataSourceId) {
        DBPWorkspace workspace = DBWorkbench.getPlatform().getWorkspace();
        for (DBPProject project : workspace.getProjects()) {
            DBPDataSourceRegistry dataSourceRegistry = project.getDataSourceRegistry();
            if (dataSourceRegistry != null) {
                DBPDataSourceContainer dataSourceContainer = dataSourceRegistry.getDataSource(dataSourceId);
                if (dataSourceContainer != null) {
                    return dataSourceContainer;
                }
            }
        }
        return null;
    }

    public static DBPDataSourceContainer findDataSource(String projectName, String dataSourceId) {
        DBPProject project = null;
        if (!CommonUtils.isEmpty(projectName)) {
            project = DBWorkbench.getPlatform().getWorkspace().getProject(projectName);
        }

        if (project != null) {
            return project.getDataSourceRegistry().getDataSource(dataSourceId);
        } else {
            return DBUtils.findDataSource(dataSourceId);
        }
    }

    /**
     * Compares two values read from database.
     * Main difference with regular compare is that all numbers are compared as doubles (i.e. data type doesn't matter).
     * Also checks DBValue for nullability
     */
    public static int compareDataValues(Object cell1, Object cell2) {
        if (cell1 == cell2 || (isNullValue(cell1) && isNullValue(cell2))) {
            return 0;
        } else if (isNullValue(cell1)) {
            return 1;
        } else if (isNullValue(cell2)) {
            return -1;
        } else if (cell1 instanceof Number && cell2 instanceof Number) {
            // Actual data type for the same column may differ (e.g. partially read from server, partially added on client side)
            return CommonUtils.compareNumbers((Number) cell1, (Number) cell2);
        } else if (cell1 instanceof Comparable cmp1 && cell1.getClass() == cell2.getClass()) {
            return cmp1.compareTo(cell2);
        } else {
            if (cell1 instanceof Number) {
                Object num2 = GeneralUtils.convertString(String.valueOf(cell2), cell1.getClass());
                if (num2 == null) {
                    return -1;
                }
                if (num2 instanceof Number) {
                    return CommonUtils.compareNumbers((Number) cell1, (Number) num2);
                }
            } else if (cell2 instanceof Number) {
                Object num1 = GeneralUtils.convertString(String.valueOf(cell1), cell2.getClass());
                if (num1 == null) {
                    return 1;
                }
                if (num1 instanceof Number) {
                    return CommonUtils.compareNumbers((Number) num1, (Number) cell2);
                }
            }
            String str1 = String.valueOf(cell1);
            String str2 = String.valueOf(cell2);
            return str1.compareTo(str2);
        }
    }

    public static DBSEntity getEntityFromMetaData(@NotNull DBRProgressMonitor monitor, DBCExecutionContext executionContext, DBCEntityMetaData entityMeta) throws DBException {
        final DBSObjectContainer objectContainer = getAdapter(DBSObjectContainer.class, executionContext.getDataSource());
        if (objectContainer != null) {
            DBSEntity entity = getEntityFromMetaData(monitor, executionContext, objectContainer, entityMeta, false);
            if (entity == null) {
                entity = getEntityFromMetaData(monitor, executionContext, objectContainer, entityMeta, true);
            }
            return entity;
        } else {
            return null;
        }
    }

    public static DBSEntity getEntityFromMetaData(@NotNull DBRProgressMonitor monitor, DBCExecutionContext executionContext, DBSObjectContainer objectContainer, DBCEntityMetaData entityMeta, boolean transformName) throws DBException {
        final DBPDataSource dataSource = objectContainer.getDataSource();
        String catalogName = entityMeta.getCatalogName();
        String schemaName = entityMeta.getSchemaName();
        String entityName = entityMeta.getEntityName();
        if (transformName) {
            catalogName = DBObjectNameCaseTransformer.transformName(dataSource, catalogName);
            schemaName = DBObjectNameCaseTransformer.transformName(dataSource, schemaName);
            entityName = DBObjectNameCaseTransformer.transformName(dataSource, entityName);
        }
        if (CommonUtils.isEmpty(entityName)) {
            return null;
        }
        DBSObject entityObject = getObjectByPath(monitor, executionContext, objectContainer, catalogName, schemaName, entityName);
        if (entityObject instanceof DBSAlias alias && !(entityObject instanceof DBSEntity)) {
            entityObject = alias.getTargetObject(monitor);
        }
        if (entityObject == null) {
            return null;
        } else if (entityObject instanceof DBSEntity entity) {
            return entity;
        } else {
            log.debug("Unsupported table class: " + entityObject.getClass().getName());
            return null;
        }
    }

    public static DBSEntityConstraint getConstraint(DBRProgressMonitor monitor, DBSEntity dbsEntity, DBSAttributeBase attribute) throws DBException {
        for (DBSEntityConstraint constraint : CommonUtils.safeCollection(dbsEntity.getConstraints(monitor))) {
            DBSEntityAttributeRef constraintAttribute = getConstraintAttribute(monitor, ((DBSEntityReferrer) constraint), attribute.getName());
            if (constraintAttribute != null && constraintAttribute.getAttribute() == attribute) {
                return constraint;
            }
        }
        return null;
    }

    public static boolean isReadOnly(DBSObject object) {
        if (object == null) {
            return true;
        }
        DBPDataSource dataSource = object.getDataSource();
        return dataSource == null ||
            !dataSource.getContainer().hasModifyPermission(DBPDataSourcePermission.PERMISSION_EDIT_METADATA) ||
            dataSource.getInfo().isReadOnlyMetaData();
    }

    public static <T> T createNewAttributeValue(DBCExecutionContext context, DBDValueHandler valueHandler, DBSTypedObject valueType, Class<T> targetType) throws DBCException {
        DBRRunnableWithResult<Object> runnable = new DBRRunnableWithResult<>() {
            @Override
            public void run(DBRProgressMonitor monitor) throws InvocationTargetException {
                try (DBCSession session = context.openSession(monitor, DBCExecutionPurpose.UTIL, "Create new object")) {
                    result = valueHandler.createNewValueObject(session, valueType);
                } catch (DBCException e) {
                    throw new InvocationTargetException(e);
                }
            }
        };
        try {
            DBWorkbench.getPlatformUI().executeWithProgress(runnable);
            //UIUtils.runInProgressService(runnable);
        } catch (InvocationTargetException e) {
            throw new DBCException(e.getTargetException(), context);
        } catch (InterruptedException e) {
            throw new DBCException(e, context);
        }

        Object result = runnable.getResult();
        if (result == null) {
            throw new DBCException("Internal error - null object created");
        }
        if (!targetType.isInstance(result)) {
            throw new DBCException("Internal error - wrong object type '" + result.getClass().getName() + "' while '" + targetType.getName() + "' was expected");
        }
        return targetType.cast(result);
    }

    public static boolean isView(DBSEntity table) {
        return table instanceof DBSView || table instanceof DBSTable dbsTab && dbsTab.isView();
    }

    public static String getEntityScriptName(DBSObject object, Map<String, Object> options) {
        return CommonUtils.getOption(options, DBPScriptObject.OPTION_FULLY_QUALIFIED_NAMES, true)
               && object instanceof DBPQualifiedObject qo ?
            qo.getFullyQualifiedName(DBPEvaluationContext.DDL) : DBUtils.getQuotedIdentifier(object);
    }

    public static String getObjectTypeName(DBSObject object) {
        if (object instanceof DBSObjectWithType owt) {
            DBSObjectType objectType = owt.getObjectType();
            if (objectType != null) {
                return objectType.getTypeName();
            }
        }
        DBSObjectType[] objectTypes = object.getDataSource().getInfo().getSupportedObjectTypes();
        for (DBSObjectType ot : objectTypes) {
            Class<? extends DBSObject> typeClass = ot.getTypeClass();
            if (typeClass != null && typeClass != DBSObject.class && typeClass.isInstance(object)) {
                return ot.getTypeName();
            }
        }
        return "Object";
    }

    @Nullable
    public static DBSDataType getDataType(@NotNull DBSTypedObject typedObject) {
        if (typedObject instanceof DBSDataType dt) {
            return dt;
        } else if (typedObject instanceof DBSTypedObjectEx dte) {
            return dte.getDataType();
        }
        return null;
    }

    @Nullable
    public static DBSDataType getDataType(@Nullable DBPDataSource dataSource, @NotNull DBSTypedObject typedObject) {
        DBSDataType dataType = getDataType(typedObject);
        if (dataType != null) {
            return dataType;
        }
        if (dataSource instanceof DBPDataTypeProvider dtp) {
            return dtp.getLocalDataType(typedObject.getFullTypeName());
        }
        return null;
    }

    /**
     * Extracts modifiers from a given type.
     * <p>
     * <h3>Example:</h3>
     * <pre>{@code
     * final Pair<String, String[]> modifiers = DBUtils.getTypeModifiers(NUMBER(5, 10)");
     * Assert.assertEquals("NUMBER", modifiers.getFirst());
     * Assert.assertArrayEquals(new String[]{"5", "10"}, modifiers.getSecond());
     * }</pre>
     *
     * @param type string containing type with modifiers to retrieve them from
     * @return pair of extracted type name and array of modifiers
     * @throws DBException if value is malformed and cannot be parsed
     */
    @NotNull
    public static Pair<String, String[]> getTypeModifiers(@NotNull String type) throws DBException {
        final int modStartIndex = type.indexOf('(');
        final int modEndIndex = type.indexOf(')');
        if (modStartIndex < 0 ^ modEndIndex < 0) {
            throw new DBException("Type '" + type + "' has malformed modifiers");
        }
        if (modStartIndex < 0) {
            return new Pair<>(type, new String[0]);
        }
        final String name = (type.substring(0, modStartIndex) + type.substring(modEndIndex + 1)).trim();
        if (name.isEmpty()) {
            throw new DBException("Type name is missing");
        }
        final String mod = type.substring(modStartIndex + 1, modEndIndex).trim();
        if (mod.isEmpty()) {
            throw new DBException("Type with zero modifiers is not allowed");
        }
        final String[] mods = mod.split(",");
        for (int i = 0; i < mods.length; i++) {
            if ((mods[i] = mods[i].trim()).isEmpty()) {
                throw new DBException("Type has empty modifiers");
            }
        }
        return new Pair<>(name, mods);
    }

    @NotNull
    public static <PARENT extends DBSObject, CHILD extends DBSObject> String makeNewObjectName(
        @NotNull DBRProgressMonitor monitor,
        @NotNull String template,
        @NotNull PARENT parent,
        @NotNull Class<? extends CHILD> type,
        @NotNull ChildExtractor<PARENT, CHILD> extractor,
        @NotNull DBECommandContext context) {
        int suffix = 1;

        while (true) {
            final String name = Objects.requireNonNull(DBObjectNameCaseTransformer.transformName(parent.getDataSource(), NLS.bind(template, suffix)));

            try {
                boolean exists = extractor.extract(parent, monitor, name) != null;

                if (!exists) {
                    for (DBPObject object : context.getEditedObjects()) {
                        if (type.isInstance(object) && ((DBSObject) object).getParentObject() == parent && name.equalsIgnoreCase(((DBSObject) object).getName())) {
                            exists = true;
                            break;
                        }
                    }
                }

                if (!exists) {
                    return name;
                }
            } catch (DBException e) {
                log.warn(e);
                return name;
            }

            suffix += 1;
        }
    }

    /**
     * Returns list of all underlying data containers from different kind of parent nodes (usually from the navigator tree)
     *
     * @param monitor can not be null
     * @param parent  Parent object: schema, catalog, datasource, DBNDatabaseFolder, even table
     * @return List of data containers (tables, views etc.) from the parent container (schema, catalog, datasource etc.)
     * @throws DBException if connection is lost or something is going wrong during children loading
     */
    public static List<DBSDataContainer> getAllDataContainersFromParentContainer(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObject parent) throws DBException {
        List<DBSDataContainer> result = new ArrayList<>();
        if (parent instanceof DBSDataContainer) {
            result.add((DBSDataContainer) parent);
        } else if (parent instanceof DBSObjectContainer container) {
            Class<? extends DBSObject> primaryChildType = container.getPrimaryChildType(monitor);
            if (DBSDataContainer.class.isAssignableFrom(primaryChildType)) {
                // This is schema or catalog with tables
                Collection<? extends DBSObject> children = container.getChildren(monitor);
                if (!CommonUtils.isEmpty(children)) {
                    for (DBSObject child : children) {
                        if (child instanceof DBSDataContainer) {
                            result.add((DBSDataContainer) child);
                        }
                    }
                }
            } else if (DBSObjectContainer.class.isAssignableFrom(primaryChildType)) {
                // This is datasource or database probably
                Collection<? extends DBSObject> children = container.getChildren(monitor);
                for (DBSObject child : children) {
                    Collection<? extends DBSObject> dbsObjects = ((DBSObjectContainer) child).getChildren(monitor);
                    for (DBSObject dbsObject : dbsObjects) {
                        if (dbsObject instanceof DBSDataContainer) {
                            result.add((DBSDataContainer) dbsObject);
                        }
                    }
                }
            }
        } else if (parent instanceof DBNDatabaseFolder) {
            Collection<DBSObject> dbsObjects = ((DBNDatabaseFolder) parent).getChildrenObjects(monitor);
            for (DBSObject dbsObject : dbsObjects) {
                List<DBSDataContainer> containers = getAllDataContainersFromParentContainer(monitor, dbsObject);
                if (!CommonUtils.isEmpty(containers)) {
                    result.addAll(containers);
                }
            }
        } else if (parent instanceof DBPDataSourceContainer) {
            DBPDataSource dataSource = parent.getDataSource();
            if (dataSource instanceof DBSObjectContainer) {
                List<DBSDataContainer> containers = getAllDataContainersFromParentContainer(monitor, dataSource);
                if (!CommonUtils.isEmpty(containers)) {
                    result.addAll(containers);
                }
            }
        }
        return result;
    }

    public static boolean initDataSource(
        @Nullable DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer dataSource,
        @Nullable DBRProgressListener onFinish
    ) throws DBException {
        if (!dataSource.isConnected()) {
            DBServiceConnections serviceConnections = DBWorkbench.getService(DBServiceConnections.class);
            if (serviceConnections != null) {
                serviceConnections.initConnection(monitor, dataSource, onFinish);
            }
        } else {
            if (onFinish != null) {
                onFinish.onTaskFinished(Status.OK_STATUS);
            }
        }
        return dataSource.isConnected();
    }

    public static long readRowCount(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBCExecutionContext executionContext,
        @Nullable DBSDataContainer dataContainer,
        @Nullable DBDDataFilter dataFilter,
        @NotNull Object controller
    ) throws DBException {
        if (executionContext == null || dataContainer == null) {
            throw new DBException(ModelMessages.error_not_connected_to_database);
        }
        long[] result = new long[1];
        DBExecUtils.tryExecuteRecover(monitor, executionContext.getDataSource(), param -> {
            try (DBCSession session = executionContext.openSession(
                monitor,
                DBCExecutionPurpose.USER,
                "Read total row count")) {
                long rowCount = dataContainer.countData(
                    new AbstractExecutionSource(dataContainer, executionContext, controller),
                    session,
                    dataFilter,
                    DBSDataContainer.FLAG_NONE);
                result[0] = rowCount;
            } catch (DBCException e) {
                throw new InvocationTargetException(e);
            }
        });
        return result[0];
    }

    public static long countDataFromQuery(
        @NotNull DBCExecutionSource source,
        @NotNull DBCSession session,
        @NotNull SQLQuery query
    ) throws DBCException {
        try (DBCStatement dbStatement = makeStatement(source, session, DBCStatementType.SCRIPT, query, 0, 0)) {
            if (dbStatement.executeStatement()) {
                try (DBCResultSet rs = dbStatement.openResultSet()) {
                    if (rs.nextRow()) {
                        List<? extends DBCAttributeMetaData> resultAttrs = rs.getMeta().getAttributes();
                        Object countValue = null;
                        if (resultAttrs.size() == 1) {
                            countValue = rs.getAttributeValue(0);
                        } else {
                            // In some databases (Influx?) SELECT count(*) produces multiple columns. Try to find first one with 'count' in its name.
                            for (int i = 0; i < resultAttrs.size(); i++) {
                                DBCAttributeMetaData ma = resultAttrs.get(i);
                                if (ma.getName().toLowerCase(Locale.ENGLISH).contains("count")) {
                                    countValue = rs.getAttributeValue(i);
                                    break;
                                }
                            }
                        }
                        if (countValue instanceof Map && ((Map<?, ?>) countValue).size() == 1) {
                            // For document-based DBs
                            Object singleValue = ((Map<?, ?>) countValue).values().iterator().next();
                            if (singleValue instanceof Number) {
                                countValue = singleValue;
                            }
                        }
                        if (countValue instanceof Number) {
                            return ((Number) countValue).longValue();
                        } else {
                            throw new DBCException("Unexpected row count value: " + countValue);
                        }
                    } else {
                        throw new DBCException("Row count result is empty");
                    }
                }
            } else {
                throw new DBCException("Row count query didn't return any value");
            }
        }
    }

    public interface ChildExtractor<PARENT, CHILD> {
        @Nullable
        CHILD extract(@NotNull PARENT parent, @NotNull DBRProgressMonitor monitor, @NotNull String name) throws DBException;
    }

    /**
     * The method returns true if index based by source attributes is UNIQUE
     */
    public static boolean isUniqueIndexForAttributes(
        @NotNull DBRProgressMonitor monitor,
        @NotNull List<DBSEntityAttribute> attributes,
        @NotNull DBSEntity entity
    ) throws DBException, InterruptedException {
        Collection<? extends DBSTableIndex> tableIndexes = ((DBSTable) entity).getIndexes(monitor);
        for (DBSTableIndex tableIndex : tableIndexes) {
            if (monitor.isCanceled()) {
                break;
            }
            // find composite index that presented as a compositions of columns (source attributes)
            List<DBSEntityAttribute> indexAttributes = DBUtils.getEntityAttributes(monitor, tableIndex);
            if (tableIndex.isUnique() && indexAttributes.equals(attributes)) {
                return true;
            }
        }
        return false;
    }
}
