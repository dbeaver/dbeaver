/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.data.DBDValueError;
import org.jkiss.dbeaver.model.impl.data.DefaultValueHandler;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
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
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.IVariableResolver;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * DBUtils
 */
public final class DBUtils {

    private static final Log log = Log.getLog(DBUtils.class);
    private static final int MAX_SAMPLE_ROWS = 1000;

    @NotNull
    public static String getQuotedIdentifier(@NotNull DBPNamedObject object)
    {
        return object instanceof DBSObject ? getQuotedIdentifier(((DBSObject) object).getDataSource(), object.getName()) : object.getName();
    }

    @NotNull
    public static String getQuotedIdentifier(@NotNull DBSObject object)
    {
        return getQuotedIdentifier(object.getDataSource(), object.getName());
    }

    public static boolean isQuotedIdentifier(@NotNull DBPDataSource dataSource, @NotNull String str)
    {
        {
            final String[][] quoteStrings = dataSource.getSQLDialect().getIdentifierQuoteStrings();
            if (ArrayUtils.isEmpty(quoteStrings)) {
                return false;
            }
            for (int i = 0; i < quoteStrings.length; i++) {
                if (str.startsWith(quoteStrings[i][0]) && str.endsWith(quoteStrings[i][1])) {
                    return true;
                }
            }
        }
        return false;
    }

    @NotNull
    public static String getUnQuotedIdentifier(@NotNull DBPDataSource dataSource, @NotNull String str)
    {
        return getUnQuotedIdentifier(str, dataSource.getSQLDialect().getIdentifierQuoteStrings());
    }

    @NotNull
    public static String getUnQuotedIdentifier(@NotNull String str, String[][] quoteStrings) {
        if (ArrayUtils.isEmpty(quoteStrings)) {
            quoteStrings = BasicSQLDialect.DEFAULT_IDENTIFIER_QUOTES;
        }
        for (int i = 0; i < quoteStrings.length; i++) {
            str = getUnQuotedIdentifier(str, quoteStrings[i][0], quoteStrings[i][1]);
        }
        return str;
    }

    @NotNull
    public static String getUnQuotedIdentifier(@NotNull String str, @NotNull String quote)
    {
        return getUnQuotedIdentifier(str, quote, quote);
    }

    @NotNull
    public static String getUnQuotedIdentifier(@NotNull String str, @NotNull String quote1, @NotNull String quote2)
    {
        if (quote1 != null && quote2 != null && str.length() >= quote1.length() + quote2.length() &&
            str.startsWith(quote1) && str.endsWith(quote2))
        {
            return str.substring(quote1.length(), str.length() - quote2.length());
        }
        return str;
    }

    @NotNull
    public static String getQuotedIdentifier(@NotNull DBPDataSource dataSource, @NotNull String str)
    {
        return getQuotedIdentifier(dataSource, str, true, false);
    }

    @NotNull
    public static String getQuotedIdentifier(@NotNull DBPDataSource dataSource, @NotNull String str, boolean caseSensitiveNames, boolean quoteAlways)
    {
        if (isQuotedIdentifier(dataSource, str)) {
            // Already quoted
            return str;
        }
        final SQLDialect sqlDialect = dataSource.getSQLDialect();
        String[][] quoteStrings = sqlDialect.getIdentifierQuoteStrings();
        if (ArrayUtils.isEmpty(quoteStrings)) {
            return str;
        }

        // Check for keyword conflict
        final DBPKeywordType keywordType = sqlDialect.getKeywordType(str);
        boolean hasBadChars = quoteAlways ||
            ((keywordType == DBPKeywordType.KEYWORD || keywordType == DBPKeywordType.TYPE) &&
            sqlDialect.isQuoteReservedWords());

        if (!hasBadChars && !str.isEmpty()) {
            hasBadChars = !sqlDialect.validIdentifierStart(str.charAt(0));
        }
        if (!hasBadChars && caseSensitiveNames) {
            // Check for case of quoted idents. Do not check for unquoted case - we don't need to quote em anyway
            // Disable supportsQuotedMixedCase checking. Let's quote identifiers always if storage case doesn't match actual case
            // unless database use case-insensitive search always (e.g. MySL with lower_case_table_names <> 0)
            if (!sqlDialect.useCaseInsensitiveNameLookup()) {
                // See how unquoted identifiers are stored
                // If passed identifier case differs from unquoted then we need to escape it
                switch (sqlDialect.storesUnquotedCase()) {
                    case UPPER:
                        hasBadChars = !str.equals(str.toUpperCase());
                        break;
                    case LOWER:
                        hasBadChars = !str.equals(str.toLowerCase());
                        break;
                }
            }
        }

        // Check for bad characters
        if (!hasBadChars && !str.isEmpty()) {
            for (int i = 0; i < str.length(); i++) {
                if (!sqlDialect.validIdentifierPart(str.charAt(i), false)) {
                    hasBadChars = true;
                    break;
                }
            }
        }
        if (!hasBadChars) {
            return str;
        }

        // Escape quote chars
        for (int i = 0; i < quoteStrings.length; i++) {
            String q1 = quoteStrings[i][0], q2 = quoteStrings[i][1];
            if (q1.equals(q2) && (q1.equals("\"") || q1.equals("'"))) {
                if (str.contains(q1)) {
                    str = str.replace(q1, q1 + q1);
                }
            }
        }
        // Escape with first (default) quote string
        return quoteStrings[0][0] + str + quoteStrings[0][1];
    }

    @NotNull
    public static String getFullQualifiedName(@Nullable DBPDataSource dataSource, @NotNull DBPNamedObject ... path)
    {
        StringBuilder name = new StringBuilder(20 * path.length);
        if (dataSource  == null) {
            // It is not SQL identifier, let's just make it simple then
            for (DBPNamedObject namePart : path) {
                if (isVirtualObject(namePart)) {
                    continue;
                }
                if (name.length() > 0) { name.append('.'); }
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
                if (name.length() > 0) {
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
    public static String getSimpleQualifiedName(@NotNull Object... names)
    {
        StringBuilder name = new StringBuilder(names.length * 16);
        for (Object namePart : names) {
            if (namePart == null) {
                continue;
            }
            if (name.length() > 0 && name.charAt(name.length() - 1) != '.') {
                name.append('.');
            }
            name.append(namePart);
        }
        return name.toString();
    }

    @NotNull
    public static String getFullyQualifiedName(@NotNull DBPDataSource dataSource, @NotNull String... names)
    {
        SQLDialect dialect = SQLUtils.getDialectFromDataSource(dataSource);
        StringBuilder name = new StringBuilder(names.length * 16);
        for (String namePart : names) {
            if (namePart == null) {
                continue;
            }
            if (name.length() > 0) name.append(dialect.getStructSeparator());
            name.append(DBUtils.getQuotedIdentifier(dataSource, namePart));
        }
        return name.toString();
    }

    /**
     * Checks that object has valid object name.
     * Some DB objects have dummy names (like "" or ".") - we won't use them for certain purposes.
     * @param name object name
     * @return true or false
     */
    public static boolean isValidObjectName(@Nullable String name)
    {
        if (name == null || name.isEmpty()) {
            return false;
        }
        // We need at least one digit or letter
        for (int i = 0; i < name.length(); i++) {
            if (Character.isLetterOrDigit(name.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds catalog, schema or table within specified object container
     * @param monitor progress monitor
     * @param executionContext
     * @param rootSC container
     * @param catalogName catalog name (optional)
     * @param schemaName schema name (optional)
     * @param objectName table name (optional)
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
        throws DBException
    {
        if (!CommonUtils.isEmpty(catalogName)) {
            Class<? extends DBSObject> childType = rootSC.getChildType(monitor);
            if (DBSSchema.class.isAssignableFrom(childType) || DBSEntity.class.isAssignableFrom(childType)) {
                // Datasource supports only schemas. Do not use catalog
                catalogName = null;
            }
        }
        if (!CommonUtils.isEmpty(catalogName) && !CommonUtils.isEmpty(schemaName)) {
            // We have both both - just search both
            DBSObject catalog = rootSC.getChild(monitor, catalogName);
            if (!(catalog instanceof DBSObjectContainer)) {
                return null;
            }
            rootSC = (DBSObjectContainer) catalog;
            DBSObject schema = rootSC.getChild(monitor, schemaName);
            if (!(schema instanceof DBSObjectContainer)) {
                return null;
            }
            rootSC = (DBSObjectContainer) schema;
        } else if (!CommonUtils.isEmpty(catalogName) || !CommonUtils.isEmpty(schemaName)) {
            // One container name
            String containerName = !CommonUtils.isEmpty(catalogName) ? catalogName : schemaName;
            DBSObject sc = rootSC.getChild(monitor, containerName);
            if (!(sc instanceof DBSObjectContainer)) {
                // Not found - try to find in selected object
                DBSObject selectedObject = getSelectedObject(executionContext);
                if (selectedObject instanceof DBSObjectContainer) {
                    if (selectedObject instanceof DBSSchema && selectedObject.getParentObject() instanceof DBSCatalog && CommonUtils.isEmpty(catalogName) &&
                        !CommonUtils.equalObjects(schemaName, selectedObject.getName()))
                    {
                        // We search for schema and active object is schema. Let's search our schema in catalog
                        selectedObject = selectedObject.getParentObject();
                    }
                    if (selectedObject instanceof DBSSchema && CommonUtils.equalObjects(schemaName, selectedObject.getName()) ||
                        selectedObject instanceof DBSCatalog && CommonUtils.equalObjects(catalogName, selectedObject.getName())) {
                        // Selected object is a catalog or schema which is also specified as catalogName/schemaName -
                        sc = selectedObject;
                    } else {
                        sc = ((DBSObjectContainer) selectedObject).getChild(monitor, containerName);
                    }
                }
                if (!(sc instanceof DBSObjectContainer)) {
                    return null;
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
            if (selectedObject instanceof DBSObjectContainer) {
                return ((DBSObjectContainer) selectedObject).getChild(monitor, objectName);
            }

            // Table container not found
            return object;
        }
    }

    @Nullable
    public static DBSObject findNestedObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull DBSObjectContainer parent,
        @NotNull List<String> names)
        throws DBException
    {
        for (int i = 0; i < names.size(); i++) {
            String childName = names.get(i);
            DBSObject child = parent.getChild(monitor, childName);
            if (child == null && i == 0) {
                DBCExecutionContextDefaults contextDefaults = executionContext.getContextDefaults();
                if (contextDefaults != null) {
                    DBSObjectContainer container = contextDefaults.getDefaultCatalog();
                    if (container != null) {
                        child = container.getChild(monitor, childName);
                    }
                    if (child == null) {
                        container = contextDefaults.getDefaultSchema();
                        if (container != null) {
                            child = container.getChild(monitor, childName);
                        }
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
                parent = (DBSObjectContainer) child;
            } else {
                break;
            }
        }
        return null;
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
    public static <T extends DBPNamedObject> T findObject(@Nullable T[] theList, String objectName)
    {
        if (theList != null && theList.length > 0 ) {
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
    public static <T extends DBPNamedObject> List<T> findObjects(@Nullable Collection<T> theList, @Nullable String objectName)
    {
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
    public static <T> T getAdapter(@NotNull Class<T> adapterType, @Nullable Object object)
    {
        if (object instanceof DBPDataSourceContainer) {
            // Root object's parent is data source container (not datasource)
            // So try to get adapter from real datasource object
            object = ((DBPDataSourceContainer)object).getDataSource();
        }
        if (object == null) {
            return null;
        }
        if (adapterType.isAssignableFrom(object.getClass())) {
            return adapterType.cast(object);
        } else if (object instanceof IAdaptable) {
            return ((IAdaptable)object).getAdapter(adapterType);
        } else {
            return null;
        }
    }

    @Nullable
    public static <T> T getParentAdapter(@NotNull Class<T> i, DBSObject object)
    {
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
    public static <T> T getParentOfType(@NotNull Class<T> type, DBSObject object)
    {
        if (object == null) {
            return null;
        }
        for (DBSObject parent = object.getParentObject(); parent != null; parent = parent.getParentObject()) {
            if (type.isInstance(parent)) {
                return type.cast(parent);
            } else if (parent instanceof DBPDataSource || parent instanceof DBPDataSourceContainer) {
                break;
            }
        }
        return null;
    }

    /**
     * Search for virtual entity descriptor
     * @param object object
     * @return object path
     */
    @NotNull
    public static DBSObject[] getObjectPath(@NotNull DBSObject object, boolean includeSelf)
    {
        int depth = 0;
        final DBSObject root = includeSelf ? object : object.getParentObject();
        for (DBSObject obj = root; obj != null; obj = obj.getParentObject()) {
            obj = getPublicObjectContainer(obj);
            depth++;
        }
        DBSObject[] path = new DBSObject[depth];
        for (DBSObject obj = root; obj != null; obj = obj.getParentObject()) {
            obj = getPublicObjectContainer(obj);
            path[depth-- - 1] = obj;
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
            if (pathStr.length() > 0) {
                pathStr.append('/');
            }
            obj = getPublicObjectContainer(obj);
            if (obj instanceof DBPDataSourceContainer) {
                pathStr.append(((DBPDataSourceContainer) obj).getId());
            } else {
                pathStr.append(obj.getName());
            }
        }
        return pathStr.toString();
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
            dataSourceContainer = findDataSource(names[0]);
            if (dataSourceContainer == null) {
                log.debug("Can't find datasource '" + names[0] + "' in any project");
                return null;
            }
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
        if (sc != null) {
            for (int i = 1; i < names.length - 1; i++) {
                String name = names[i];
                DBSObject child = sc.getChild(monitor, name);
                if (child == null) {
                    log.debug("Can't find child container " + name + " in container " + DBUtils.getObjectFullName(sc, DBPEvaluationContext.UI));
                    return null;
                }
                if (child instanceof DBSObjectContainer) {
                    sc = (DBSObjectContainer) child;
                } else {
                    log.debug("Child object '" + name + "' is not a container");
                    return null;
                }
            }
        }
        if (sc != null) {
            String objectName = names[names.length - 1];
            DBSObject object = sc.getChild(monitor, objectName);
            if (object == null) {
                log.debug("Child object '" + objectName + "' not found in container " + DBUtils.getObjectFullName(sc, DBPEvaluationContext.UI));
                return null;
            }
            return object;
        }
        return null;
    }

    public static boolean isNullValue(@Nullable Object value)
    {
        return (value == null || (value instanceof DBDValue && ((DBDValue) value).isNull()));
    }

    public static boolean isErrorValue(@Nullable Object value)
    {
        return value instanceof DBDValueError;
    }

    @Nullable
    public static Object makeNullValue(@NotNull DBCSession session, @NotNull DBDValueHandler valueHandler, @NotNull DBSTypedObject type) throws DBCException
    {
        return valueHandler.getValueFromObject(session, type, null, false, false);
    }

    @NotNull
    public static DBDAttributeBindingMeta getAttributeBinding(@NotNull DBSDataContainer dataContainer, @NotNull DBCSession session, @NotNull DBCAttributeMetaData attributeMeta)
    {
        return new DBDAttributeBindingMeta(dataContainer, session, attributeMeta);
    }

    @NotNull
    public static DBDAttributeBinding[] getAttributeBindings(@NotNull DBCSession session, @NotNull DBSDataContainer dataContainer, @NotNull DBCResultSetMetaData metaData) {
        List<DBCAttributeMetaData> metaAttributes = metaData.getAttributes();
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

        if (filterAttributes && dataContainer instanceof DBDAttributeFilter) {
            return ((DBDAttributeFilter) dataContainer).filterAttributeBindings(bindings);
        } else {
            return bindings;
        }
    }

    /**
     * Returns "bottom" level attributes out of resultset.
     * For regular resultsets it is the same as getAttributeBindings, for compelx types it returns only leaf attributes.
     * @return
     */
    @NotNull
    public static DBDAttributeBinding[] makeLeafAttributeBindings(@NotNull DBCSession session, @NotNull DBSDataContainer dataContainer, @NotNull DBCResultSet resultSet) throws DBCException {
        List<DBDAttributeBinding> metaColumns = new ArrayList<>();
        List<DBCAttributeMetaData> attributes = resultSet.getMeta().getAttributes();
        if (attributes.size() == 1 && attributes.get(0).getDataKind() == DBPDataKind.DOCUMENT) {
            DBCAttributeMetaData attributeMeta = attributes.get(0);
            DBDAttributeBindingMeta docBinding = DBUtils.getAttributeBinding(dataContainer, session, attributeMeta);
            try {
                List<Object[]> sampleRows = Collections.emptyList();
                if (resultSet instanceof DBCResultSetSampleProvider) {
                    session.getProgressMonitor().subTask("Read sample rows");
                    sampleRows = ((DBCResultSetSampleProvider) resultSet).getSampleRows(session, MAX_SAMPLE_ROWS);
                }
                session.getProgressMonitor().subTask("Discover attribute structure");
                docBinding.lateBinding(session, sampleRows);
            } catch (Exception e) {
                log.error("Document attribute '" + docBinding.getName() + "' binding error", e);
            }
            List<DBDAttributeBinding> nested = docBinding.getNestedBindings();
            if (!CommonUtils.isEmpty(nested)) {
                metaColumns.addAll(nested);
            } else {
                // No nested bindings. Try to get entity attributes
                try {
                    DBSEntity docEntity = getEntityFromMetaData(session.getProgressMonitor(), session.getExecutionContext(), attributeMeta.getEntityMetaData());
                    if (docEntity != null) {
                        Collection<? extends DBSEntityAttribute> entityAttrs = docEntity.getAttributes(session.getProgressMonitor());
                        if (!CommonUtils.isEmpty(entityAttrs)) {
                            for (DBSEntityAttribute ea : entityAttrs) {
                                metaColumns.add(new DBDAttributeBindingType(docBinding, ea));
                            }
                        }
                    }
                } catch (DBException e) {
                    log.debug("Error getting attributes from document entity", e);
                }
            }
        }
        if (metaColumns.isEmpty()) {
            for (DBCAttributeMetaData attribute : attributes) {
                DBDAttributeBinding columnBinding = DBUtils.getAttributeBinding(dataContainer, session, attribute);
                metaColumns.add(columnBinding);
            }
        }

        List<DBDAttributeBinding> result = new ArrayList<>(metaColumns.size());
        for (DBDAttributeBinding binding : metaColumns) {
            addLeafBindings(result, binding);
        }

        return injectAndFilterAttributeBindings(session.getDataSource(), dataContainer, result.toArray(new DBDAttributeBinding[0]), true);
    }

    private static void addLeafBindings(List<DBDAttributeBinding> result, DBDAttributeBinding binding) {
        List<DBDAttributeBinding> nestedBindings = binding.getNestedBindings();
        if (CommonUtils.isEmpty(nestedBindings)) {
            result.add(binding);
        } else {
            for (DBDAttributeBinding nested : nestedBindings) {
                addLeafBindings(result, nested);
            }
        }
    }

    @Nullable
    public static Object getAttributeValue(@NotNull DBDAttributeBinding attribute, DBDAttributeBinding[] allAttributes, Object[] row) {
        if (attribute.isCustom()) {
            return DBVUtils.executeExpression(((DBDAttributeBindingCustom)attribute).getEntityAttribute(), allAttributes, row);
        }
        int depth = attribute.getLevel();
        if (depth == 0) {
            final int index = attribute.getOrdinalPosition();
            if (index >= row.length) {
                log.debug("Bad attribute '" + attribute.getName() + "' index: " + index + " is out of row values' bounds (" + row.length + ")");
                return null;
            } else {
                return row[index];
            }
        }
        Object curValue = row[attribute.getTopParent().getOrdinalPosition()];

        for (int i = 0; i < depth; i++) {
            if (curValue == null) {
                break;
            }
            DBDAttributeBinding attr = attribute.getParent(depth - i - 1);
            assert attr != null;
            try {
                curValue = attr.extractNestedValue(curValue);
            } catch (Throwable e) {
                //log.debug("Error reading nested value of [" + attr.getName() + "]", e);
                curValue = new DBDValueError(e);
                break;
            }
        }

        return curValue;
    }

    @NotNull
    public static DBDValueHandler findValueHandler(@NotNull DBCSession session, @NotNull DBSTypedObject column)
    {
        return findValueHandler(session.getDataSource(), session, column);
    }

    @NotNull
    public static DBDValueHandler findValueHandler(@NotNull DBPDataSource dataSource, @NotNull DBSTypedObject column)
    {
        return findValueHandler(dataSource, dataSource.getContainer(), column);
    }

    @NotNull
    public static DBDValueHandler findValueHandler(@Nullable DBPDataSource dataSource, @Nullable DBDPreferences preferences, @NotNull DBSTypedObject column)
    {
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
            handlerProvider = dataSource.getContainer().getPlatform().getValueHandlerRegistry().getValueHandlerProvider(
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
    public static boolean isIdentifyingAssociation(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntityAssociation association) throws DBException
    {
        if (!(association instanceof DBSEntityReferrer)) {
            return false;
        }
        final DBSEntityReferrer referrer = (DBSEntityReferrer)association;
        final DBSEntity refEntity = association.getAssociatedEntity();
        final DBSEntity ownerEntity = association.getParentObject();
        assert ownerEntity != null;
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
                if (constraint instanceof DBSEntityReferrer &&
                    ((hasPrimaryKey && constraint.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY) ||
                    (!hasPrimaryKey && constraint.getConstraintType().isUnique())))
                {
                    List<DBSEntityAttribute> constAttrs = getEntityAttributes(monitor, (DBSEntityReferrer) constraint);

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

    @NotNull
    public static String getDefaultDataTypeName(@NotNull DBSObject objectContainer, DBPDataKind dataKind)
    {
        if (objectContainer instanceof DBPDataTypeProvider) {
            return ((DBPDataTypeProvider) objectContainer).getDefaultDataTypeName(dataKind);
        } else {
            // Unsupported data kind
            return "?";
        }
    }

    @Nullable
    public static DBDAttributeBinding findBinding(@NotNull Collection<DBDAttributeBinding> bindings, @NotNull DBSAttributeBase attribute)
    {
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
    public static DBDAttributeBinding findBinding(@NotNull DBDAttributeBinding[] bindings, @Nullable DBSAttributeBase attribute)
    {
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
        throws DBException
    {
        DBSEntity entity = entityAttribute.getParentObject();
        Collection<? extends DBSEntityAssociation> associations = includeVirtual ? DBVUtils.getAllAssociations(monitor, entity) : entity.getAssociations(monitor);
        if (associations != null) {
            List<DBSEntityReferrer> refs = new ArrayList<>();
            for (DBSEntityAssociation fk : associations) {
                if (fk instanceof DBSEntityReferrer && DBUtils.getConstraintAttribute(monitor, (DBSEntityReferrer) fk, entityAttribute) != null) {
                    refs.add((DBSEntityReferrer)fk);
                }
            }
            return refs;
        }
        return Collections.emptyList();
    }

    @NotNull
    public static List<? extends DBSEntityAttribute> getBestTableIdentifier(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntity entity)
        throws DBException
    {
        if (entity instanceof DBSTable && ((DBSTable) entity).isView()) {
            return Collections.emptyList();
        }
        if (CommonUtils.isEmpty(entity.getAttributes(monitor))) {
            return Collections.emptyList();
        }

        List<DBSEntityConstraint> identifiers = new ArrayList<>();
        //List<DBSEntityConstraint> nonIdentifyingConstraints = null;

        // Check indexes
        if (entity instanceof DBSTable) {
            try {
                Collection<? extends DBSTableIndex> indexes = ((DBSTable)entity).getIndexes(monitor);
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
                if (id instanceof DBSEntityReferrer && id.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY) {
                    return getEntityAttributes(monitor, (DBSEntityReferrer) id);
                } else if (id.getConstraintType().isUnique()) {
                    uniqueId = id;
                } else if (id instanceof DBSTableIndex && ((DBSTableIndex) id).isUnique()) {
                    uniqueId = id;
                }
            }
            return uniqueId instanceof DBSEntityReferrer ?
                getEntityAttributes(monitor, (DBSEntityReferrer)uniqueId)
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
            if (constraint instanceof DBSEntityReferrer) {
                List<? extends DBSEntityAttributeRef> attrs = ((DBSEntityReferrer) constraint).getAttributeReferences(monitor);
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
        throws DBException
    {
        // Check constraints
        Collection<? extends DBSEntityConstraint> constraints = entity.getConstraints(monitor);
        if (!CommonUtils.isEmpty(constraints)) {
            for (DBSEntityConstraint constraint : constraints) {
                if (constraint instanceof DBSEntityReferrer && referrerMatches(monitor, (DBSEntityReferrer)constraint, attributes)) {
                    return constraint;
                }
            }
        }
        if (entity instanceof DBSTable) {
            Collection<? extends DBSTableIndex> indexes = ((DBSTable) entity).getIndexes(monitor);
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
        if (refs != null && !refs.isEmpty()) {
            Iterator<? extends DBSEntityAttribute> attrIterator = attributes.iterator();
            for (DBSEntityAttributeRef ref : refs) {
                if (!attrIterator.hasNext()) {
                    return false;
                }
                if (ref.getAttribute() == null ||
                    !CommonUtils.equalObjects(ref.getAttribute().getName(), attrIterator.next().getName()))
                {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @NotNull
    public static List<DBSEntityAttribute> getEntityAttributes(@NotNull DBRProgressMonitor monitor, @Nullable DBSEntityReferrer referrer)
    {
        try {
            if (referrer instanceof DBVEntityConstraint && ((DBVEntityConstraint) referrer).isUseAllColumns()) {
                DBSEntity realEntity = ((DBVEntityConstraint) referrer).getEntity().getRealEntity(monitor);
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
    public static DBSEntityAttributeRef getConstraintAttribute(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntityReferrer constraint, @NotNull DBSEntityAttribute tableColumn) throws DBException
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

    @Nullable
    public static DBSEntityAttributeRef getConstraintAttribute(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntityReferrer constraint, @NotNull String columnName) throws DBException
    {
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
        boolean reference) throws DBException
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
                DBSEntityAttributeRef ownAttr = ownIterator.next();
                if (reference) {
                    if (ownAttr instanceof DBSTableForeignKeyColumn && ((DBSTableForeignKeyColumn) ownAttr).getReferencedColumn() == tableColumn) {
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
        @NotNull DBCExecutionSource executionSource,
        @NotNull DBCSession session,
        @NotNull DBCStatementType statementType,
        @NotNull String query,
        long offset,
        long maxRows) throws DBCException
    {
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
        @NotNull DBCExecutionSource executionSource,
        @NotNull DBCSession session,
        @NotNull DBCStatementType statementType,
        @NotNull SQLQuery sqlQuery,
        long offset,
        long maxRows)
        throws DBCException
    {
        // We need to detect whether it is a plain select statement
        // or some DML. For DML statements we mustn't set limits
        // because it sets update rows limit [SQL Server]
        boolean selectQuery = sqlQuery.getType() == SQLQueryType.SELECT && sqlQuery.isPlainSelect();
        final boolean hasLimits = (offset > 0 || selectQuery) && maxRows > 0;
        // This is a flag for any potential SELECT query
        boolean possiblySelect = sqlQuery.getType() == SQLQueryType.SELECT || sqlQuery.getType() == SQLQueryType.UNKNOWN;
        boolean limitAffectsDML = Boolean.TRUE.equals(session.getDataSource().getDataSourceFeature(DBConstants.FEATURE_LIMIT_AFFECTS_DML));

        DBCQueryTransformer limitTransformer = null, fetchAllTransformer = null;
        if (selectQuery) {
            DBCQueryTransformProvider transformProvider = DBUtils.getAdapter(DBCQueryTransformProvider.class, session.getDataSource());
            if (transformProvider != null) {
                if (hasLimits) {
                    if (session.getDataSource().getContainer().getPreferenceStore().getBoolean(ModelPreferences.RESULT_SET_MAX_ROWS_USE_SQL)) {
                        limitTransformer = transformProvider.createQueryTransformer(DBCQueryTransformType.RESULT_SET_LIMIT);
                    }
                } else {
                    fetchAllTransformer = transformProvider.createQueryTransformer(DBCQueryTransformType.FETCH_ALL_TABLE);
                }
            }
        }

        String queryText;
        try {
            if (hasLimits && limitTransformer != null) {
                limitTransformer.setParameters(offset, maxRows);
                queryText = limitTransformer.transformQueryString(sqlQuery);
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
            createStatement(session, queryText, hasLimits) :
            makeStatement(session, queryText, hasLimits);
        dbStat.setStatementSource(executionSource);

        if (offset > 0 || hasLimits || (possiblySelect && maxRows > 0 && !limitAffectsDML)) {
            if (limitTransformer == null) {
                // Set explicit limit - it is safe because we pretty sure that this is a plain SELECT query
                dbStat.setLimit(offset, maxRows);
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
        boolean scrollable) throws DBCException
    {
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
        boolean scrollable) throws DBCException
    {
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

    public static void fireObjectUpdate(@NotNull DBSObject object)
    {
        fireObjectUpdate(object, null, null);
    }

    public static void fireObjectUpdate(DBSObject object, boolean enabled)
    {
        final DBPDataSourceContainer container = getContainer(object);
        if (container != null) {
            container.fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, object, enabled));
        }
    }

    public static void fireObjectUpdate(DBSObject object, @Nullable Map<String, Object> options, @Nullable Object data)
    {
        final DBPDataSourceContainer container = getContainer(object);
        if (container != null) {
            DBPEvent event = new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, object, data);
            event.setOptions(options);
            container.fireEvent(event);
        }
    }

    public static void fireObjectUpdate(DBSObject object, @Nullable Object data)
    {
        fireObjectUpdate(object, null, data);
    }

    public static void fireObjectAdd(DBSObject object, Map<String, Object> options)
    {
        final DBPDataSourceContainer container = getContainer(object);
        if (container != null) {
            DBPEvent event = new DBPEvent(DBPEvent.Action.OBJECT_ADD, object);
            event.setOptions(options);
            container.fireEvent(event);
        }
    }

    public static void fireObjectRemove(DBSObject object)
    {
        final DBPDataSourceContainer container = getContainer(object);
        if (container != null) {
            container.fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_REMOVE, object));
        }
    }

    public static void fireObjectSelect(DBSObject object, boolean select)
    {
        final DBPDataSourceContainer container = getContainer(object);
        if (container != null) {
            container.fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_SELECT, object, select));
        }
    }

    /**
     * Refresh object in UI
     */
    public static void fireObjectRefresh(DBSObject object)
    {
        // Select with true parameter is the same as refresh
        fireObjectSelect(object, true);
    }

    @NotNull
    public static String getObjectUniqueName(@NotNull DBSObject object)
    {
        if (object instanceof DBPUniqueObject) {
            return ((DBPUniqueObject) object).getUniqueName();
        } else {
            return object.getName();
        }
    }

    @Nullable
    public static DBSDataType findBestDataType(@NotNull Collection<? extends DBSDataType> allTypes, @NotNull String ... typeNames)
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

    @Nullable
    public static DBSDataType resolveDataType(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @NotNull String fullTypeName)
        throws DBException
    {
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
        @NotNull String fullTypeName)
    {
        DBPDataTypeProvider dataTypeProvider = getAdapter(DBPDataTypeProvider.class, dataSource);
        if (dataTypeProvider == null) {
            return null;
        }
        return dataTypeProvider.getLocalDataType(fullTypeName);
    }

    public static DBSObject getPublicObject(@Nullable DBSObject object)
    {
        if (object instanceof DBPDataSourceContainer) {
            return ((DBPDataSourceContainer) object).getDataSource();
        } else {
            return object;
        }
    }

    /**
     * Returns DBPDataSourceContainer fro DBPDataSource or object itself otherwise
     */
    public static DBSObject getPublicObjectContainer(@NotNull DBSObject object)
    {
        if (object instanceof DBPDataSource) {
            return ((DBPDataSource) object).getContainer();
        } else {
            return object;
        }
    }

    @Nullable
    public static DBPDataSourceContainer getContainer(@Nullable DBSObject object)
    {
        if (object == null) {
            return null;
        }
        if (object instanceof DBPDataSourceContainer) {
            return (DBPDataSourceContainer) object;
        }
        final DBPDataSource dataSource = object.getDataSource();
        return dataSource == null ? null : dataSource.getContainer();
    }

    @NotNull
    public static DBPDataSourceRegistry getObjectRegistry(@NotNull DBSObject object)
    {
        DBPDataSourceContainer container;
        if (object instanceof DBPDataSourceContainer) {
            container = (DBPDataSourceContainer) object;
        } else {
            DBPDataSource dataSource = object.getDataSource();
            container = dataSource.getContainer();
        }
        return container.getRegistry();
    }


    @NotNull
    public static DBPProject getObjectOwnerProject(DBSObject object) {
        return getObjectRegistry(object).getProject();
    }

    @NotNull
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

    @NotNull
    public static String getObjectFullName(@NotNull DBPNamedObject object, DBPEvaluationContext context)
    {
        if (object instanceof DBPQualifiedObject) {
            return ((DBPQualifiedObject) object).getFullyQualifiedName(context);
        } else if (object instanceof DBSObject) {
            return getObjectFullName(((DBSObject) object).getDataSource(), object, context);
        } else {
            return object.getName();
        }
    }

    @NotNull
    public static String getObjectFullName(@NotNull DBPDataSource dataSource, @NotNull DBPNamedObject object, DBPEvaluationContext context)
    {
        if (object instanceof DBPQualifiedObject) {
            return ((DBPQualifiedObject) object).getFullyQualifiedName(context);
        } else {
            return getQuotedIdentifier(dataSource, object.getName());
        }
    }

    @NotNull
    public static String getFullTypeName(@NotNull DBSTypedObject typedObject)
    {
        DBSObject structObject = getFromObject(typedObject);
        DBPDataSource dataSource = structObject == null ? null : structObject.getDataSource();
        String typeName = typedObject.getTypeName();
        String typeModifiers = SQLUtils.getColumnTypeModifiers(dataSource, typedObject, typeName, typedObject.getDataKind());
        return typeModifiers == null ? typeName : (typeName + typeModifiers);
    }

    public static void releaseValue(@Nullable Object value)
    {
        if (value instanceof DBDValue) {
            ((DBDValue)value).release();
        }
    }

    public static void resetValue(@Nullable Object value)
    {
        if (value instanceof DBDContent) {
            ((DBDContent)value).resetContents();
        }
    }

    @NotNull
    public static DBCLogicalOperator[] getAttributeOperators(DBSTypedObject attribute) {
        if (attribute instanceof DBSTypedObjectEx) {
            DBSDataType dataType = ((DBSTypedObjectEx) attribute).getDataType();
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
        if (attribute instanceof DBSAttributeBase && !((DBSAttributeBase)attribute).isRequired()) {
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
            return ((DBDValue)value).getRawValue();
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
    public static <T extends DBCSession> T openMetaSession(@NotNull DBRProgressMonitor monitor, @NotNull DBSObject object, @NotNull String task) {
        return (T) getDefaultContext(object, true).openSession(monitor, DBCExecutionPurpose.META, task);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T extends DBCSession> T openMetaSession(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSource dataSource, @NotNull String task) {
        return (T) dataSource.getDefaultInstance().getDefaultContext(monitor, true).openSession(monitor, DBCExecutionPurpose.META, task);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T extends DBCSession> T openUtilSession(@NotNull DBRProgressMonitor monitor, @NotNull DBSObject object, @NotNull String task) {
        return (T) getDefaultContext(object, false).openSession(monitor, DBCExecutionPurpose.UTIL, task);
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
    public static DBSObject getDefaultOrActiveObject(@NotNull DBSInstance object)
    {
        DBCExecutionContext defaultContext = getDefaultContext(object, true);
        DBSObject activeObject = defaultContext == null ? null : getActiveInstanceObject(defaultContext);
        return activeObject == null ? object.getDataSource() : activeObject;
    }

    @Nullable
    public static DBSObject getActiveInstanceObject(@NotNull DBCExecutionContext executionContext) {
        return getSelectedObject(executionContext);
    }

    @Nullable
    public static DBSObject getSelectedObject(@NotNull DBCExecutionContext context)
    {
        DBCExecutionContextDefaults contextDefaults = context.getContextDefaults();
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

    @NotNull
    public static DBSObject[] getSelectedObjects(DBRProgressMonitor monitor, @NotNull DBCExecutionContext context) {
        DBCExecutionContextDefaults contextDefaults = context.getContextDefaults();
        if (contextDefaults != null) {
            DBSCatalog defaultCatalog = contextDefaults.getDefaultCatalog();
            DBSSchema defaultSchema = contextDefaults.getDefaultSchema();
            if (defaultCatalog != null && defaultSchema != null) {
                return new DBSObject[] { defaultCatalog, defaultSchema };
            } else if (defaultCatalog != null) {
                return new DBSObject[] { defaultCatalog };
            } else if (defaultSchema != null) {
                return new DBSObject[] { defaultSchema };
            }
        }
        return new DBSObject[0];
    }

    public static void refreshContextDefaultsAndReflect(DBRProgressMonitor monitor, DBCExecutionContextDefaults contextDefaults) {
        try {
            DBSCatalog defaultCatalog = contextDefaults.getDefaultCatalog();
            DBSSchema defaultSchema = contextDefaults.getDefaultSchema();
            if (contextDefaults.refreshDefaults(monitor, false)) {
                fireObjectSelectionChange(defaultCatalog, contextDefaults.getDefaultCatalog());
                fireObjectSelectionChange(defaultSchema, contextDefaults.getDefaultSchema());
            }
        } catch (Exception e) {
            log.debug(e);
        }
    }

    public static void fireObjectSelectionChange(DBSObject oldDefaultObject, DBSObject newDefaultObject) {
        if (oldDefaultObject != newDefaultObject) {
            if (oldDefaultObject != null) {
                DBUtils.fireObjectSelect(oldDefaultObject, false);
            }
            if (newDefaultObject != null) {
                DBUtils.fireObjectSelect(newDefaultObject, true);
            }
        }
    }

    public static DBSObjectContainer getChangeableObjectContainer(DBCExecutionContextDefaults contextDefaults, DBSObjectContainer root, Class<? extends DBSObject> childType) {
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
        return object instanceof DBPHiddenObject && ((DBPHiddenObject) object).isHidden();
    }

    public static boolean isSystemObject(Object object) {
        return object instanceof DBPSystemObject && ((DBPSystemObject) object).isSystem();
    }

    public static boolean isVirtualObject(Object object) {
        return object instanceof DBPVirtualObject && ((DBPVirtualObject) object).isVirtual();
    }

    public static boolean isInheritedObject(Object object) {
        return object instanceof DBPInheritedObject && ((DBPInheritedObject) object).isInherited();
    }

    public static DBDPseudoAttribute getRowIdAttribute(DBSEntity entity) {
        if (entity instanceof DBDPseudoAttributeContainer) {
            try {
                return DBDPseudoAttribute.getAttribute(
                    ((DBDPseudoAttributeContainer) entity).getPseudoAttributes(),
                    DBDPseudoAttributeType.ROWID);
            } catch (DBException e) {
                log.warn("Can't get pseudo attributes for '" + entity.getName() + "'", e);
            }
        }
        return null;
    }

    public static boolean isRowIdAttribute(DBSEntityAttribute attr) {
        DBDPseudoAttribute rowIdAttribute = getRowIdAttribute(attr.getParentObject());
        return rowIdAttribute != null && rowIdAttribute.getName().equals(attr.getName());
    }

    public static DBDPseudoAttribute getPseudoAttribute(DBSEntity entity, String attrName) {
        if (entity instanceof DBDPseudoAttributeContainer) {
            try {
                DBDPseudoAttribute[] pseudoAttributes = ((DBDPseudoAttributeContainer) entity).getPseudoAttributes();
                if (pseudoAttributes != null && pseudoAttributes.length > 0) {
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
        return attr instanceof DBDAttributeBinding && ((DBDAttributeBinding) attr).isPseudoAttribute();
    }

    public static <TYPE extends DBPNamedObject> Comparator<TYPE> nameComparator() {
        return Comparator.comparing(DBPNamedObject::getName);
    }

    public static <TYPE extends DBPNamedObject> Comparator<TYPE> nameComparatorIgnoreCase() {
        return (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName());
    }

    public static Comparator<? super DBSAttributeBase> orderComparator() {
        return Comparator.comparingInt(DBSAttributeBase::getOrdinalPosition);
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

    public static String getClientApplicationName(DBPDataSourceContainer container, DBCExecutionContext context, String purpose) {
        if (container.getPreferenceStore().getBoolean(ModelPreferences.META_CLIENT_NAME_OVERRIDE)) {
            String appName = container.getPreferenceStore().getString(ModelPreferences.META_CLIENT_NAME_VALUE);
            IVariableResolver cVarResolver = container.getVariablesResolver(false);
            return GeneralUtils.replaceVariables(appName, name -> {
                switch (name) {
                    case DBConstants.VAR_CONTEXT_NAME:
                        return context == null ? null : context.getContextName();
                    case DBConstants.VAR_CONTEXT_ID:
                        return context == null ? null : String.valueOf(context.getContextId());
                    default:
                        return cVarResolver.get(name);
                }
            });
        }
        final String productTitle = GeneralUtils.getProductTitle();
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
        return dataSource == null ? null : dataSource.getDefaultInstance();
    }

    public static DBCExecutionContext getDefaultContext(DBSObject object, boolean meta) {
        if (object == null) {
            return null;
        }
        DBSInstance instance = getObjectOwnerInstance(object);
        return instance == null || (instance instanceof DBSInstanceLazy && !((DBSInstanceLazy) instance).isInstanceConnected()) ?
            null :
            instance.getDefaultContext(new VoidProgressMonitor(), meta);
    }

    public static DBCExecutionContext getOrOpenDefaultContext(DBSObject object, boolean meta) {
        DBCExecutionContext context = DBUtils.getDefaultContext(object, meta);
        if (context == null) {
            // Not connected - try to connect
            DBSInstance ownerInstance = DBUtils.getObjectOwnerInstance(object);
            if (ownerInstance instanceof DBSInstanceLazy && !((DBSInstanceLazy)ownerInstance).isInstanceConnected()) {
                if (!RuntimeUtils.runTask(monitor -> {
                        try {
                            ((DBSInstanceLazy) ownerInstance).checkInstanceConnection(monitor);
                        } catch (DBException e) {
                            throw new InvocationTargetException(e);
                        }
                    }, "Initiate instance connection",
                    object.getDataSource().getContainer().getPreferenceStore().getInt(ModelPreferences.CONNECTION_OPEN_TIMEOUT))) {
                    return null;
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
     * Find data source in all available registries
     */
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

    /**
     * Compares two values read from database.
     * Main difference with regular compare is that all numbers are compared as doubles (i.e. data type oesn't matter).
     * Also checks DBValue for nullability
     */
    public static int compareDataValues(Object cell1, Object cell2) {
        if (cell1 == cell2) {
            return 0;
        } else if (isNullValue(cell1)) {
            return 1;
        } else if (isNullValue(cell2)) {
            return -1;
        } else if (cell1 instanceof Number && cell2 instanceof Number) {
            // Actual data type for the same column may differ (e.g. partially read from server, partially added on client side)
            return CommonUtils.compareNumbers((Number) cell1, (Number) cell2);
        } else if (cell1 instanceof Comparable && cell1.getClass() == cell2.getClass()) {
            return ((Comparable) cell1).compareTo(cell2);
        } else {
            if (cell1 instanceof Number) {
                Number num2 = (Number) GeneralUtils.convertString(String.valueOf(cell2), cell1.getClass());
                if (num2 == null) {
                    return -1;
                }
                return CommonUtils.compareNumbers((Number) cell1, num2);
            } else if (cell2 instanceof Number) {
                Number num1 = (Number) GeneralUtils.convertString(String.valueOf(cell1), cell2.getClass());
                if (num1 == null) {
                    return 1;
                }
                return CommonUtils.compareNumbers(num1, (Number) cell2);
            }
            String str1 = String.valueOf(cell1);
            String str2 = String.valueOf(cell2);
            return str1.compareTo(str2);
        }
    }

    public static DBSEntity getEntityFromMetaData(DBRProgressMonitor monitor, DBCExecutionContext executionContext, DBCEntityMetaData entityMeta) throws DBException {
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

    public static DBSEntity getEntityFromMetaData(DBRProgressMonitor monitor, DBCExecutionContext executionContext, DBSObjectContainer objectContainer, DBCEntityMetaData entityMeta, boolean transformName) throws DBException {
        final DBPDataSource dataSource = objectContainer.getDataSource();
        String catalogName = entityMeta.getCatalogName();
        String schemaName = entityMeta.getSchemaName();
        String entityName = entityMeta.getEntityName();
        if (transformName) {
            catalogName = DBObjectNameCaseTransformer.transformName(dataSource, catalogName);
            schemaName = DBObjectNameCaseTransformer.transformName(dataSource, schemaName);
            entityName = DBObjectNameCaseTransformer.transformName(dataSource, entityName);
        }
        DBSObject entityObject = getObjectByPath(monitor, executionContext, objectContainer, catalogName, schemaName, entityName);
        if (entityObject instanceof DBSAlias && !(entityObject instanceof DBSEntity)) {
            entityObject = ((DBSAlias) entityObject).getTargetObject(monitor);
        }
        if (entityObject == null) {
            return null;
        } else if (entityObject instanceof DBSEntity) {
            return (DBSEntity) entityObject;
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

    public static boolean isReadOnly(DBSObject object)
    {
        if (object == null) {
            return true;
        }
        DBPDataSource dataSource = object.getDataSource();
        return dataSource == null || !dataSource.getContainer().hasModifyPermission(DBPDataSourcePermission.PERMISSION_EDIT_METADATA);
    }

    public static <T> T createNewAttributeValue(DBCExecutionContext context, DBDValueHandler valueHandler, DBSTypedObject valueType, Class<T> targetType) throws DBCException {
        DBRRunnableWithResult<Object> runnable = new DBRRunnableWithResult<Object>() {
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
        return table  instanceof DBSView || table instanceof DBSTable && ((DBSTable) table).isView();
    }

    public static String getEntityScriptName(DBSEntity entity, Map<String, Object> options) {
        return CommonUtils.getOption(options, DBPScriptObject.OPTION_FULLY_QUALIFIED_NAMES, true) && entity instanceof DBPQualifiedObject ?
            ((DBPQualifiedObject)entity).getFullyQualifiedName(DBPEvaluationContext.DDL) : DBUtils.getQuotedIdentifier(entity);
    }

    public static String getObjectTypeName(DBSObject object) {
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
        if (typedObject instanceof DBSDataType) {
            return (DBSDataType) typedObject;
        } else if (typedObject instanceof DBSTypedObjectEx) {
            return ((DBSTypedObjectEx) typedObject).getDataType();
        }
        return null;
    }
}
