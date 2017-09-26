/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.data.DefaultValueHandler;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.*;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * DBUtils
 */
public final class DBUtils {

    private static final Log log = Log.getLog(DBUtils.class);

    @NotNull
    public static String getQuotedIdentifier(@NotNull DBSObject object)
    {
        return getQuotedIdentifier(object.getDataSource(), object.getName());
    }

    public static boolean isQuotedIdentifier(@NotNull DBPDataSource dataSource, @NotNull String str)
    {
        if (dataSource instanceof SQLDataSource) {
            final String[][] quoteStrings = ((SQLDataSource) dataSource).getSQLDialect().getIdentifierQuoteStrings();
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
        if (dataSource instanceof SQLDataSource) {
            str = getUnQuotedIdentifier(str, ((SQLDataSource) dataSource).getSQLDialect().getIdentifierQuoteStrings());
        }
        return str;
    }

    @NotNull
    public static String getUnQuotedIdentifier(@NotNull String str, String[][] quoteStrings) {
        if (ArrayUtils.isEmpty(quoteStrings)) {
            quoteStrings = BasicSQLDialect.DEFAULT_QUOTE_STRINGS;
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
        if (dataSource instanceof SQLDataSource) {
            return getQuotedIdentifier((SQLDataSource)dataSource, str, true);
        } else {
            return str;
        }
    }

    @NotNull
    public static String getQuotedIdentifier(@NotNull SQLDataSource dataSource, @NotNull String str, boolean caseSensitiveNames)
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
        boolean hasBadChars =
            (keywordType == DBPKeywordType.KEYWORD || keywordType == DBPKeywordType.TYPE) &&
            sqlDialect.isQuoteReservedWords();

        if (!hasBadChars && !str.isEmpty()) {
            hasBadChars = Character.isDigit(str.charAt(0));
        }
        if (caseSensitiveNames) {
            // Check for case of quoted idents. Do not check for unquoted case - we don't need to quote em anyway
            if (!hasBadChars && sqlDialect.supportsQuotedMixedCase()) {
                // See how unquoted idents are stored
                // If passed identifier case differs from unquoted then we need to escape it
                if (sqlDialect.storesUnquotedCase() == DBPIdentifierCase.UPPER) {
                    hasBadChars = !str.equals(str.toUpperCase());
                } else if (sqlDialect.storesUnquotedCase() == DBPIdentifierCase.LOWER) {
                    hasBadChars = !str.equals(str.toLowerCase());
                }
            }
        }

        // Check for bad characters
        if (!hasBadChars && !str.isEmpty()) {
            if (str.charAt(0) == '_') {
                hasBadChars = true;
            } else {
                for (int i = 0; i < str.length(); i++) {
                    if (!sqlDialect.validUnquotedCharacter(str.charAt(i))) {
                        hasBadChars = true;
                        break;
                    }
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
    public static String getFullQualifiedName(@NotNull DBPDataSource dataSource, @NotNull DBPNamedObject ... path)
    {
        StringBuilder name = new StringBuilder(20 * path.length);
        if (!(dataSource instanceof SQLDataSource)) {
            // It is not SQL identifier, let's just make it simple then
            for (DBPNamedObject namePart : path) {
                if (name.length() > 0) { name.append('.'); }
                name.append(namePart.getName());
            }
        } else {
            final SQLDialect sqlDialect = ((SQLDataSource) dataSource).getSQLDialect();

            DBPNamedObject parent = null;
            for (DBPNamedObject namePart : path) {
                if (namePart == null) {
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
        boolean validName = false;
        for (int i = 0; i < name.length(); i++) {
            if (Character.isLetterOrDigit(name.charAt(i))) {
                validName = true;
                break;
            }
        }
        return validName;
    }

    /**
     * Finds catalog, schema or table within specified object container
     * @param monitor progress monitor
     * @param rootSC container
     * @param catalogName catalog name (optional)
     * @param schemaName schema name (optional)
     * @param objectName table name (optional)
     * @return found object or null
     * @throws DBException
     */
    @Nullable
    public static DBSObject getObjectByPath(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObjectContainer rootSC,
        @Nullable String catalogName,
        @Nullable String schemaName,
        @Nullable String objectName)
        throws DBException
    {
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
                DBSObject selectedObject = getSelectedObject(rootSC, false);
                if (selectedObject instanceof DBSObjectContainer) {
                    sc = ((DBSObjectContainer) selectedObject).getChild(monitor, containerName);
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
            DBSObject selectedObject = DBUtils.getSelectedObject(rootSC, true);
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
        @NotNull DBSObjectContainer parent,
        @NotNull List<String> names)
        throws DBException
    {
        for (int i = 0; i < names.size(); i++) {
            String childName = names.get(i);
            DBSObject child = parent.getChild(monitor, childName);
            if (child == null) {
                DBSObjectSelector selector = DBUtils.getAdapter(DBSObjectSelector.class, parent);
                if (selector != null) {
                    DBSObjectContainer container = DBUtils.getAdapter(DBSObjectContainer.class, selector.getDefaultObject());
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
    @Nullable
    public static <T extends DBPNamedObject> T findObject(@Nullable Collection<T> theList, String objectName)
    {
        if (theList != null && !theList.isEmpty()) {
            for (T object : theList) {
                if (object.getName().equalsIgnoreCase(objectName)) {
                    return object;
                }
            }
        }
        return null;
    }

    @Nullable
    public static <T extends DBPNamedObject> T findObject(@Nullable List<T> theList, String objectName)
    {
        if (theList != null) {
            int size = theList.size();
            for (int i = 0; i < size; i++) {
                if (theList.get(i).getName().equalsIgnoreCase(objectName)) {
                    return theList.get(i);
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
            return adapterType.cast(((IAdaptable)object).getAdapter(adapterType));
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
            depth++;
        }
        DBSObject[] path = new DBSObject[depth];
        for (DBSObject obj = root; obj != null; obj = obj.getParentObject()) {
            path[depth-- - 1] = obj;
        }
        return path;
    }

    public static String getObjectFullId(@NotNull DBSObject object) {
        DBSObject[] path = getObjectPath(object, true);
        StringBuilder pathStr = new StringBuilder();
        for (DBSObject obj : path) {
            if (pathStr.length() > 0) {
                pathStr.append('/');
            }
            pathStr.append(getQuotedIdentifier(obj));
        }
        return pathStr.toString();
    }

    public static boolean isNullValue(@Nullable Object value)
    {
        return (value == null || (value instanceof DBDValue && ((DBDValue) value).isNull()));
    }

    @Nullable
    public static Object makeNullValue(@NotNull DBCSession session, @NotNull DBDValueHandler valueHandler, @NotNull DBSTypedObject type) throws DBCException
    {
        return valueHandler.getValueFromObject(session, type, null, false);
    }

    @NotNull
    public static DBDAttributeBindingMeta getAttributeBinding(@NotNull DBCSession session, @NotNull DBCAttributeMetaData attributeMeta)
    {
        return new DBDAttributeBindingMeta(session, attributeMeta);
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
    public static DBDValueHandler findValueHandler(@NotNull DBPDataSource dataSource, @Nullable DBDPreferences preferences, @NotNull DBSTypedObject column)
    {
        DBDValueHandler typeHandler = null;
        // Get handler provider from datasource
        DBDValueHandlerProvider typeProvider = getAdapter(DBDValueHandlerProvider.class, dataSource);
        if (typeProvider != null) {
            typeHandler = typeProvider.getValueHandler(dataSource, preferences, column);
            if (typeHandler != null) {
                return typeHandler;
            }
        }
        // Get handler provider from registry
        typeProvider = dataSource.getContainer().getPlatform().getValueHandlerRegistry().getDataTypeProvider(
            dataSource, column);
        if (typeProvider != null) {
            typeHandler = typeProvider.getValueHandler(dataSource, preferences, column);
        }
        // Use default handler
        if (typeHandler == null) {
            if (preferences == null) {
                typeHandler = DefaultValueHandler.INSTANCE;
            } else {
                typeHandler = preferences.getDefaultValueHandler();
            }
        }
        return typeHandler;
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
    public static String getDefaultDataTypeName(@NotNull DBPDataSource dataSource, DBPDataKind dataKind)
    {
        if (dataSource instanceof DBPDataTypeProvider) {
            return ((DBPDataTypeProvider) dataSource).getDefaultDataTypeName(dataKind);
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
    public static List<DBSEntityReferrer> getAttributeReferrers(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntityAttribute entityAttribute)
        throws DBException
    {
        DBSEntity entity = entityAttribute.getParentObject();
        assert entity != null;
        List<DBSEntityReferrer> refs = null;
        Collection<? extends DBSEntityAssociation> associations = entity.getAssociations(monitor);
        if (associations != null) {
            for (DBSEntityAssociation fk : associations) {
                if (fk instanceof DBSEntityReferrer && DBUtils.getConstraintAttribute(monitor, (DBSEntityReferrer) fk, entityAttribute) != null) {
                    if (refs == null) {
                        refs = new ArrayList<>();
                    }
                    refs.add((DBSEntityReferrer)fk);
                }
            }
        }
        return refs != null ? refs : Collections.<DBSEntityReferrer>emptyList();
    }

    @NotNull
    public static Collection<? extends DBSEntityAttribute> getBestTableIdentifier(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntity entity)
        throws DBException
    {
        if (entity instanceof DBSTable && ((DBSTable) entity).isView()) {
            return Collections.emptyList();
        }
        if (CommonUtils.isEmpty(entity.getAttributes(monitor))) {
            return Collections.emptyList();
        }

        List<DBSEntityConstraint> identifiers = new ArrayList<>();

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
                    }
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
        if (constraint instanceof DBSEntityReferrer && constraint.getConstraintType().isUnique()) {
            List<? extends DBSEntityAttributeRef> attrs = ((DBSEntityReferrer) constraint).getAttributeReferences(monitor);
            if (attrs == null || attrs.isEmpty()) {
                return false;
            }
            for (DBSEntityAttributeRef col : attrs) {
                if (col.getAttribute() == null || !col.getAttribute().isRequired()) {
                    // Do not use constraints with NULL columns (because they are not actually unique: #424)
                    return false;
                }
            }
            return true;
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
    public static DBSEntityAttribute getReferenceAttribute(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntityAssociation association, @NotNull DBSEntityAttribute tableColumn) throws DBException
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
        final boolean hasLimits = selectQuery && offset >= 0 && maxRows > 0;

        DBCQueryTransformer limitTransformer = null, fetchAllTransformer = null;
        if (selectQuery) {
            DBCQueryTransformProvider transformProvider = DBUtils.getAdapter(DBCQueryTransformProvider.class, session.getDataSource());
            if (transformProvider != null) {
                if (hasLimits) {
                    if (session.getDataSource().getContainer().getPreferenceStore().getBoolean(ModelPreferences.RESULT_SET_MAX_ROWS_USE_SQL)) {
                        limitTransformer = transformProvider.createQueryTransformer(DBCQueryTransformType.RESULT_SET_LIMIT);
                    }
                } else if (offset <= 0 && maxRows <= 0) {
                    fetchAllTransformer = transformProvider.createQueryTransformer(DBCQueryTransformType.FETCH_ALL_TABLE);
                }
            }
        }

        String queryText;
        if (hasLimits && limitTransformer != null) {
            limitTransformer.setParameters(offset, maxRows);
            queryText = limitTransformer.transformQueryString(sqlQuery);
        } else if (fetchAllTransformer != null) {
            queryText = fetchAllTransformer.transformQueryString(sqlQuery);
        } else {
            queryText = sqlQuery.getText();
        }

        DBCStatement dbStat = statementType == DBCStatementType.SCRIPT ?
            createStatement(session, queryText, hasLimits) :
            makeStatement(session, queryText, hasLimits);
        dbStat.setStatementSource(executionSource);

        if (hasLimits || offset > 0) {
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
        DBCStatementType statementType = DBCStatementType.SCRIPT;
        query = SQLUtils.makeUnifiedLineFeeds(query);
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

    @NotNull
    public static DBCStatement makeStatement(
        @NotNull DBCSession session,
        @NotNull String query,
        boolean scrollable) throws DBCException
    {
        DBCStatementType statementType = DBCStatementType.QUERY;
        // Normalize query
        query = SQLUtils.makeUnifiedLineFeeds(query);

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
        fireObjectUpdate(object, null);
    }

    public static void fireObjectUpdate(DBSObject object, @Nullable Object data)
    {
        final DBPDataSourceContainer container = getContainer(object);
        if (container != null) {
            container.fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, object, data));
        }
    }

    public static void fireObjectUpdate(DBSObject object, boolean enabled)
    {
        final DBPDataSourceContainer container = getContainer(object);
        if (container != null) {
            container.fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, object, enabled));
        }
    }

    public static void fireObjectAdd(DBSObject object)
    {
        final DBPDataSourceContainer container = getContainer(object);
        if (container != null) {
            container.fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_ADD, object));
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

    public static DBPObject getPublicObject(@NotNull DBPObject object)
    {
        if (object instanceof DBPDataSourceContainer) {
            return ((DBPDataSourceContainer) object).getDataSource();
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
    public static IProject getObjectOwnerProject(DBSObject object) {
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
        return typeModifiers == null ? typeName : (typeName + CommonUtils.notEmpty(typeModifiers));
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
        return operators.toArray(new DBCLogicalOperator[operators.size()]);
    }

    public static Object getRawValue(Object value) {
        if (value instanceof DBDValue) {
            return ((DBDValue)value).getRawValue();
        } else {
            return value;
        }
    }

    public static boolean isIndexedAttribute(DBRProgressMonitor monitor, DBSEntityAttribute attribute) throws DBException {
        DBSEntity entity = attribute.getParentObject();
        if (entity instanceof DBSTable) {
            Collection<? extends DBSTableIndex> indexes = ((DBSTable) entity).getIndexes(monitor);
            if (!CommonUtils.isEmpty(indexes)) {
                for (DBSTableIndex index : indexes) {
                    if (getConstraintAttribute(monitor, index, attribute) != null) {
                        return true;
                    }
                }
            }
        }
        return false;
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
    public static <T extends DBCSession> T openMetaSession(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSource dataSource, @NotNull String task) {
        return (T) dataSource.getDefaultContext(true).openSession(monitor, DBCExecutionPurpose.META, task);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T extends DBCSession> T openUtilSession(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSource dataSource, @NotNull String task) {
        return (T) dataSource.getDefaultContext(false).openSession(monitor, DBCExecutionPurpose.UTIL, task);
    }

    @Nullable
    public static DBSObject getFromObject(Object object) {
        if (object instanceof DBSWrapper) {
            return ((DBSWrapper) object).getObject();
        } else if (object instanceof DBSObject) {
            return (DBSObject) object;
        } else {
            return null;
        }
    }

    public static boolean isAtomicParameter(Object o) {
        return o == null || o instanceof CharSequence || o instanceof Number || o instanceof java.util.Date || o instanceof Boolean;
    }

    @NotNull
    public static DBSObject getDefaultOrActiveObject(@NotNull DBSInstance object)
    {
        DBSObject selectedObject = getActiveInstanceObject(object);
        return selectedObject == null ? object : selectedObject;
    }

    @Nullable
    public static DBSObject getActiveInstanceObject(@NotNull DBSInstance object)
    {
        return getSelectedObject(object, true);
    }

    @Nullable
    public static DBSObject getSelectedObject(@NotNull DBSObject object, boolean searchNested)
    {
        DBSObjectSelector objectSelector = getAdapter(DBSObjectSelector.class, object);
        if (objectSelector != null) {
            DBSObject selectedObject1 = objectSelector.getDefaultObject();
            if (searchNested && selectedObject1 != null) {
                DBSObject nestedObject = getSelectedObject(selectedObject1, true);
                if (nestedObject != null) {
                    return nestedObject;
                }
            }
            return selectedObject1;
        }
        return null;
    }

    @NotNull
    public static DBSObject[] getSelectedObjects(@NotNull DBSObject object)
    {
        DBSObjectSelector objectSelector = getAdapter(DBSObjectSelector.class, object);
        if (objectSelector != null) {
            DBSObject selectedObject1 = objectSelector.getDefaultObject();
            if (selectedObject1 != null) {
                DBSObject nestedObject = getSelectedObject(selectedObject1, true);
                if (nestedObject != null) {
                    return new DBSObject[] { selectedObject1, nestedObject };
                } else {
                    return new DBSObject[] { selectedObject1 };
                }
            }
        }
        return new DBSObject[0];
    }

    public static boolean isHiddenObject(Object object) {
        return object instanceof DBPHiddenObject && ((DBPHiddenObject) object).isHidden();
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

    public static DBDPseudoAttribute getPseudoAttribute(DBSEntity entity, String attrName) {
        if (entity instanceof DBDPseudoAttributeContainer) {
            try {
                DBDPseudoAttribute[] pseudoAttributes = ((DBDPseudoAttributeContainer) entity).getPseudoAttributes();
                if (pseudoAttributes != null && pseudoAttributes.length > 0) {
                    for (int i = 0; i < pseudoAttributes.length; i++) {
                        DBDPseudoAttribute pa = pseudoAttributes[i];
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

    public static <TYPE extends DBPNamedObject> Comparator<TYPE> nameComparator()
    {
        return new Comparator<TYPE>() {
            @Override
            public int compare(DBPNamedObject o1, DBPNamedObject o2)
            {
                return o1.getName().compareTo(o2.getName());
            }
        };
    }

    public static Comparator<? super DBSAttributeBase> orderComparator() {
        return new Comparator<DBSAttributeBase>() {
            @Override
            public int compare(DBSAttributeBase o1, DBSAttributeBase o2) {
                return o1.getOrdinalPosition() - o2.getOrdinalPosition();
            }
        };
    }

    public static <T extends DBPNamedObject> void orderObjects(@NotNull List<T> objects)
    {
        Collections.sort(objects, new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                String name1 = o1.getName();
                String name2 = o2.getName();
                return name1 == null && name2 == null ? 0 :
                    (name1 == null ? -1 :
                        (name2 == null ? 1 : name1.compareTo(name2)));
            }
        });
    }

    public static String getClientApplicationName(DBPDataSourceContainer container, String purpose) {
        if (container.getPreferenceStore().getBoolean(ModelPreferences.META_CLIENT_NAME_OVERRIDE)) {
            return container.getPreferenceStore().getString(ModelPreferences.META_CLIENT_NAME_VALUE);
        }
        final String productTitle = GeneralUtils.getProductTitle();
        return purpose == null ? productTitle : productTitle + " - " + purpose;
    }

    @NotNull
    public static DBPErrorAssistant.ErrorType discoverErrorType(@NotNull DBPDataSource dataSource, @NotNull Throwable error) {
        DBPErrorAssistant errorAssistant = getAdapter(DBPErrorAssistant.class, dataSource);
        if (errorAssistant != null) {
            return ((DBPErrorAssistant) dataSource).discoverErrorType(error);
        }

        return DBPErrorAssistant.ErrorType.NORMAL;
    }
}
