/*
 * Copyright (C) 2010-2014 Serge Rieder
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

import org.jkiss.dbeaver.core.Log;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.BaseEntityIdentifier;
import org.jkiss.dbeaver.model.impl.data.DefaultValueHandler;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.parser.SQLSemanticProcessor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.*;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVEntityConstraint;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.DataTypeProviderDescriptor;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceInvalidateHandler;
import org.jkiss.dbeaver.ui.editors.sql.SQLConstants;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.regex.Pattern;

/**
 * DBUtils
 */
public final class DBUtils {

    static final Log log = Log.getLog(DBUtils.class);

    static final Pattern SELECT_QUERY_PATTERN = Pattern.compile("\\s*SELECT.+");
    static final Pattern SELECT_INTO_PATTERN = Pattern.compile("\\s*SELECT.+\\s+INTO\\s+");

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
        if (dataSource instanceof SQLDataSource) {
            final String quote = ((SQLDataSource) dataSource).getSQLDialect().getIdentifierQuoteString();
            return quote != null && str.startsWith(quote) && str.endsWith(quote);
        } else {
            return false;
        }
    }

    public static String getUnQuotedIdentifier(DBPDataSource dataSource, String str)
    {
        if (dataSource instanceof SQLDataSource) {
            final String quote = ((SQLDataSource) dataSource).getSQLDialect().getIdentifierQuoteString();
            if (quote != null && str.startsWith(quote) && str.endsWith(quote)) {
                return str.substring(quote.length(), str.length() - quote.length());
            }
        }
        return str;
    }

    public static String getUnQuotedIdentifier(String str, String quote)
    {
        if (quote != null && str.startsWith(quote) && str.endsWith(quote)) {
            return str.substring(quote.length(), str.length() - quote.length());
        }
        return str;
    }

    public static String getQuotedIdentifier(DBPDataSource dataSource, String str)
    {
        if (dataSource instanceof SQLDataSource) {
            return getQuotedIdentifier((SQLDataSource)dataSource, str, true);
        } else {
            return str;
        }
    }

    public static String getQuotedIdentifier(SQLDataSource dataSource, String str, boolean caseSensitiveNames)
    {
        final SQLDialect sqlDialect = dataSource.getSQLDialect();
        String quoteString = sqlDialect.getIdentifierQuoteString();
        if (quoteString == null) {
            return str;
        }
        if (str.startsWith(quoteString) && str.endsWith(quoteString)) {
            // Already quoted
            return str;
        }

        // Check for keyword conflict
        boolean hasBadChars = sqlDialect.getKeywordType(str) == DBPKeywordType.KEYWORD;

        if (!hasBadChars) {
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
        if (!hasBadChars) {
            for (int i = 0; i < str.length(); i++) {
                if (!sqlDialect.validUnquotedCharacter(str.charAt(i))) {
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

    public static String getFullQualifiedName(DBPDataSource dataSource, DBPNamedObject ... path)
    {
        StringBuilder name = new StringBuilder(20);
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

    public static String getSimpleQualifiedName(Object... names)
    {
        StringBuilder name = new StringBuilder(names.length * 16);
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
            DBRProgressMonitor monitor,
            DBSObjectContainer rootSC,
            String catalogName,
            String schemaName,
            String objectName)
        throws DBException
    {
        if (!CommonUtils.isEmpty(catalogName)) {
            DBSObject catalog = rootSC.getChild(monitor, catalogName);
            if (!(catalog instanceof DBSObjectContainer)) {
                return null;
            }
            rootSC = (DBSObjectContainer) catalog;
        }
        if (!CommonUtils.isEmpty(schemaName)) {
            DBSObject schema = rootSC.getChild(monitor, schemaName);
            if (!(schema instanceof DBSObjectContainer)) {
                return null;
            }
            rootSC = (DBSObjectContainer) schema;
        }
        if (objectName == null) {
            return rootSC;
        }
        Class<? extends DBSObject> childType = rootSC.getChildType(monitor);
        if (DBSTable.class.isAssignableFrom(childType)) {
            return rootSC.getChild(monitor, objectName);
        } else {
            // Child is not a table. May be catalog/schema names was omitted.
            // Try to use active child
            DBSObjectSelector objectSelector = DBUtils.getAdapter(DBSObjectSelector.class, rootSC);
            if (objectSelector != null) {
                DBSObjectContainer objectContainer = DBUtils.getAdapter(DBSObjectContainer.class, objectSelector.getSelectedObject());
                if (objectContainer != null) {
                    return objectContainer.getChild(monitor, objectName);
                }
            }

            // Table container not found
            return null;
        }
    }

    @Nullable
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
    @Nullable
    public static <T extends DBPNamedObject> T findObject(@Nullable Collection<T> theList, String objectName)
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
    @Nullable
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

    @Nullable
    public static <T> T getAdapter(Class<T> adapterType, @Nullable DBPObject object)
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

    @Nullable
    public static <T> T getParentAdapter(Class<T> i, DBSObject object)
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
    public static List<DBSObject> getObjectPath(DBSObject object, boolean includeSelf)
    {
        List<DBSObject> path = new ArrayList<DBSObject>();
        for (DBSObject obj = includeSelf ? object : object.getParentObject(); obj != null; obj = obj.getParentObject()) {
            path.add(0, obj);
        }
        return path;
    }

    public static boolean isNullValue(@Nullable Object value)
    {
        return (value == null || (value instanceof DBDValue && ((DBDValue) value).isNull()));
    }

    @Nullable
    public static Object makeNullValue(DBCSession session, DBDValueHandler valueHandler, DBSTypedObject type) throws DBCException
    {
        return valueHandler.getValueFromObject(session, type, null, false);
    }

    @Nullable
    public static Object makeNullValue(@NotNull final DBDValueController valueController)
    {
        try {
            DBPDataSource dataSource = valueController.getDataSource();
            if (dataSource == null) {
                throw new DBCException(CoreMessages.editors_sql_status_not_connected_to_database);
            }
            // We are going to create NULL value - it shouldn't result in any DB roundtrips so let's use dummy monitor
            DBCSession session = dataSource.openSession(VoidProgressMonitor.INSTANCE, DBCExecutionPurpose.UTIL, "Set NULL value");
            try {
                return DBUtils.makeNullValue(
                    session,
                    valueController.getValueHandler(),
                    valueController.getValueType());
            } finally {
                session.close();
            }
        } catch (DBCException e) {
            log.error("Can't make NULL value", e);
            return null;
        }
    }

    @NotNull
    public static DBDAttributeBindingMeta getColumnBinding(DBCSession session, DBCAttributeMetaData attributeMeta)
    {
        return new DBDAttributeBindingMeta(session.getDataSource(), attributeMeta);
    }

    @NotNull
    public static DBDValueHandler findValueHandler(DBCSession session, DBSTypedObject column)
    {
        return findValueHandler(session.getDataSource(), session, column);
    }

    @NotNull
    public static DBDValueHandler findValueHandler(DBPDataSource dataSource, DBSTypedObject column)
    {
        return findValueHandler(dataSource, dataSource.getContainer(), column);
    }

    @NotNull
    public static DBDValueHandler findValueHandler(DBPDataSource dataSource, DBDPreferences preferences, DBSTypedObject column)
    {
        DBDValueHandler typeHandler = null;
        DataTypeProviderDescriptor typeProvider = DataSourceProviderRegistry.getDefault().getDataTypeProvider(
            dataSource, column);
        if (typeProvider != null) {
            typeHandler = typeProvider.getInstance().getHandler(preferences, column);
        }
        if (typeHandler == null) {
            if (preferences == null) {
                typeHandler = DefaultValueHandler.INSTANCE;
            } else {
                typeHandler = preferences.getDefaultValueHandler();
            }
        }
        return typeHandler;
    }

    public static void findValueLocators(
        DBCSession session,
        DBDAttributeBindingMeta[] bindings,
        List<Object[]> rows)
    {
        Map<DBSEntity, DBDRowIdentifier> locatorMap = new HashMap<DBSEntity, DBDRowIdentifier>();
        try {
            for (DBDAttributeBindingMeta binding : bindings) {
                DBCAttributeMetaData attrMeta = binding.getMetaAttribute();
                DBCEntityMetaData entityMeta = attrMeta.getEntityMetaData();
                Object metaSource = attrMeta.getSource();
                if (entityMeta == null && metaSource instanceof SQLQuery) {
                    entityMeta = ((SQLQuery)metaSource).getSingleSource();
                }
                DBSEntity entity = null;
                if (metaSource instanceof DBSEntity) {
                    entity = (DBSEntity)metaSource;
                } else if (entityMeta != null) {

                    final DBSObjectContainer objectContainer = DBUtils.getAdapter(DBSObjectContainer.class, session.getDataSource());
                    if (objectContainer != null) {
                        String catalogName = entityMeta.getCatalogName();
                        String schemaName = entityMeta.getSchemaName();
                        String entityName = entityMeta.getEntityName();
                        Class<? extends DBSObject> scChildType = objectContainer.getChildType(session.getProgressMonitor());
                        DBSObject entityObject;
                        if (!CommonUtils.isEmpty(catalogName) && scChildType != null && DBSSchema.class.isAssignableFrom(scChildType)) {
                            // Do not use catalog name
                            // Some data sources do not load catalog list but result set meta data contains one (e.g. DB2)
                            entityObject = DBUtils.getObjectByPath(session.getProgressMonitor(), objectContainer, null, schemaName, entityName);
                        } else {
                            entityObject = DBUtils.getObjectByPath(session.getProgressMonitor(), objectContainer, catalogName, schemaName, entityName);
                        }
                        if (entityObject == null) {
                            log.debug("Table '" + DBUtils.getSimpleQualifiedName(catalogName, schemaName, entityName) + "' not found in metadata catalog");
                        } else if (entityObject instanceof DBSEntity) {
                            entity = (DBSEntity) entityObject;
                        } else {
                            log.debug("Unsupported table class: " + entityObject.getClass().getName());
                        }
                    }
                }
                // We got table name and column name
                // To be editable we need this result   set contain set of columns from the same table
                // which construct any unique key
                if (entity != null) {
                    DBSEntityAttribute tableColumn;
                    if (attrMeta.getPseudoAttribute() != null) {
                        tableColumn = attrMeta.getPseudoAttribute().createFakeAttribute(entity, attrMeta);
                    } else {
                        tableColumn = entity.getAttribute(session.getProgressMonitor(), attrMeta.getName());
                    }

                    binding.setEntityAttribute(tableColumn);
                }
            }

            // Init row identifiers
            for (DBDAttributeBindingMeta binding : bindings) {
                DBSEntityAttribute attr = binding.getEntityAttribute();
                if (attr == null) {
                    continue;
                }
                DBSEntity entity = attr.getParentObject();
                DBDRowIdentifier rowIdentifier = locatorMap.get(entity);
                if (rowIdentifier == null) {
                    DBCEntityIdentifier entityIdentifier = getBestIdentifier(session.getProgressMonitor(), entity, bindings);
                    if (entityIdentifier != null) {
                        rowIdentifier = new DBDRowIdentifier(
                            entity,
                            entityIdentifier);
                        locatorMap.put(entity, rowIdentifier);
                    }
                }
                binding.setRowIdentifier(rowIdentifier);
            }

            // Read nested bindings
            for (DBDAttributeBinding binding : bindings) {
                binding.lateBinding(session, rows);
            }
        }
        catch (DBException e) {
            log.error("Can't extract column identifier info", e);
        }
    }

    private static DBCEntityIdentifier getBestIdentifier(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntity table, DBDAttributeBinding[] bindings)
        throws DBException
    {
        List<DBCEntityIdentifier> identifiers = new ArrayList<DBCEntityIdentifier>(2);
        // Check for pseudo attrs (ROWID)
        for (DBDAttributeBinding column : bindings) {
            DBDPseudoAttribute pseudoAttribute = column.getMetaAttribute().getPseudoAttribute();
            if (pseudoAttribute != null && pseudoAttribute.getType() == DBDPseudoAttributeType.ROWID) {
                identifiers.add(new BaseEntityIdentifier(monitor, new DBDPseudoReferrer(table, column), bindings));
                break;
            }
        }

        if (table instanceof DBSTable && ((DBSTable) table).isView()) {
            // Skip physical identifiers for views. There are nothing anyway

        } else if (identifiers.isEmpty()) {

            // Check constraints
            Collection<? extends DBSEntityConstraint> constraints = table.getConstraints(monitor);
            if (!CommonUtils.isEmpty(constraints)) {
                for (DBSEntityConstraint constraint : constraints) {
                    if (constraint instanceof DBSEntityReferrer && constraint.getConstraintType().isUnique()) {
                        identifiers.add(
                            new BaseEntityIdentifier(monitor, (DBSEntityReferrer)constraint, bindings));
                    }
                }
            }
            if (identifiers.isEmpty() && table instanceof DBSTable) {
                try {
                    // Check indexes only if no unique constraints found
                    Collection<? extends DBSTableIndex> indexes = ((DBSTable)table).getIndexes(monitor);
                    if (!CommonUtils.isEmpty(indexes)) {
                        for (DBSTableIndex index : indexes) {
                            if (index.isUnique()) {
                                identifiers.add(
                                    new BaseEntityIdentifier(monitor, index, bindings));
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    // Indexes are not supported or not available
                    // Just skip them
                    log.debug(e);
                }
            }
        }
        if (CommonUtils.isEmpty(identifiers)) {
            // No physical identifiers
            // Make new or use existing virtual identifier
            DBVEntity virtualEntity = table.getDataSource().getContainer().getVirtualModel().findEntity(table, true);
            identifiers.add(new BaseEntityIdentifier(monitor, virtualEntity.getBestIdentifier(), bindings));
        }
        if (!CommonUtils.isEmpty(identifiers)) {
            // Find PK or unique key
            DBCEntityIdentifier uniqueId = null;
            for (DBCEntityIdentifier id : identifiers) {
                DBSEntityReferrer referrer = id.getReferrer();
                if (referrer != null && isGoodReferrer(monitor, bindings, referrer)) {
                    if (referrer.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY) {
                        return id;
                    } else if (referrer.getConstraintType().isUnique() ||
                        (referrer instanceof DBSTableIndex && ((DBSTableIndex) referrer).isUnique()))
                    {
                        uniqueId = id;
                    }
                }
            }
            return uniqueId;
        }
        return null;
    }

    private static boolean isGoodReferrer(DBRProgressMonitor monitor, DBDAttributeBinding[] bindings, DBSEntityReferrer referrer) throws DBException
    {
        if (referrer instanceof DBDPseudoReferrer) {
            return true;
        }
        Collection<? extends DBSEntityAttributeRef> references = referrer.getAttributeReferences(monitor);
        if (CommonUtils.isEmpty(references)) {
            return referrer instanceof DBVEntityConstraint;
        }
        for (DBSEntityAttributeRef ref : references) {
            for (DBDAttributeBinding binding : bindings) {
                if (binding.matches(ref.getAttribute(), false)) {
                    return true;
                }
            }
        }
        return true;
    }

    /**
     * Identifying association is an association which associated entity's attributes are included into owner entity primary key. I.e. they
     * identifies entity.
     */
    public static boolean isIdentifyingAssociation(DBRProgressMonitor monitor, DBSEntityAssociation association) throws DBException
    {
        if (!(association instanceof DBSEntityReferrer)) {
            return false;
        }
        DBSEntityReferrer referrer = (DBSEntityReferrer)association;
        DBSEntity refEntity = association.getAssociatedEntity();
        if (refEntity == association.getParentObject()) {
            // Can't migrate into itself
            return false;
        }
        Collection<? extends DBSEntityConstraint> constraints = association.getParentObject().getConstraints(monitor);
        if (!CommonUtils.isEmpty(constraints)) {
            for (DBSEntityConstraint constraint : constraints) {
                if (constraint.getConstraintType().isUnique() && constraint instanceof DBSEntityReferrer) {
                    List<DBSEntityAttribute> ownAttrs = getEntityAttributes(monitor, referrer);
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
    public static String getDefaultDataTypeName(DBPDataSource dataSource, DBPDataKind dataKind)
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
            if (binding.matches(attribute, false)) {
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
    public static DBDAttributeBinding findBinding(@NotNull DBDAttributeBinding[] bindings, @NotNull DBSAttributeBase attribute)
    {
        for (DBDAttributeBinding binding : bindings) {
            if (binding.matches(attribute, false)) {
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
        List<DBSEntityReferrer> refs = null;
        Collection<? extends DBSEntityAssociation> associations = entity.getAssociations(monitor);
        if (associations != null) {
            for (DBSEntityAssociation fk : associations) {
                if (fk instanceof DBSEntityReferrer && DBUtils.getConstraintColumn(monitor, (DBSEntityReferrer) fk, entityAttribute) != null) {
                    if (refs == null) {
                        refs = new ArrayList<DBSEntityReferrer>();
                    }
                    refs.add((DBSEntityReferrer)fk);
                }
            }
        }
        return refs != null ? refs : Collections.<DBSEntityReferrer>emptyList();
    }

    @NotNull
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

    @NotNull
    public static List<DBSEntityAttribute> getEntityAttributes(DBRProgressMonitor monitor, DBSEntityReferrer referrer)
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
        List<DBSEntityAttribute> attributes = new ArrayList<DBSEntityAttribute>(constraintColumns.size());
        for (DBSEntityAttributeRef column : constraintColumns) {
            attributes.add(column.getAttribute());
        }
        return attributes;
    }

    @Nullable
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

    @Nullable
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
    @Nullable
    public static DBSEntityAttribute getReferenceAttribute(DBRProgressMonitor monitor, DBSEntityAssociation association, DBSEntityAttribute tableColumn) throws DBException
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
    public static DBCStatement prepareStatement(
        DBCSession session,
        DBCStatementType statementType,
        String query,
        long offset,
        long maxRows) throws DBCException
    {
        // We need to detect whether it is a plain select statement
        // or some DML. For DML statements we mustn't set limits
        // because it sets update rows limit [SQL Server]
        boolean dataModifyQuery = !SQLSemanticProcessor.isSelectQuery(query);
        String testQuery = query.replace("\\n", "").toUpperCase();
        if (SELECT_QUERY_PATTERN.matcher(testQuery).find()) {
            dataModifyQuery = SELECT_INTO_PATTERN.matcher(testQuery).find();
        }
        final boolean hasLimits = !dataModifyQuery && offset >= 0 && maxRows > 0;

        DBCQueryTransformer limitTransformer = null, fetchAllTransformer = null;
        if (!dataModifyQuery) {
            DBCQueryTransformProvider transformProvider = DBUtils.getAdapter(DBCQueryTransformProvider.class, session.getDataSource());
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
            createStatement(session, query) :
            prepareStatement(session, query);

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

    @NotNull
    public static DBCStatement createStatement(
        DBCSession session,
        String query) throws DBCException
    {
        query = SQLUtils.makeUnifiedLineFeeds(query);
        return session.prepareStatement(
            DBCStatementType.SCRIPT,
            query,
            session.getDataSource().getInfo().supportsResultSetScroll(),
            false,
            false);
    }

    @NotNull
    public static DBCStatement prepareStatement(
        DBCSession session,
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
        DBPDataSource dataSource = session.getDataSource();
        if (dataSource instanceof SQLDataSource) {
            // Check for EXEC query
            final Collection<String> executeKeywords = ((SQLDataSource) dataSource).getSQLDialect().getExecuteKeywords();
            if (!CommonUtils.isEmpty(executeKeywords)) {
                final String queryStart = SQLUtils.getFirstKeyword(query);
                for (String keyword : executeKeywords) {
                    if (keyword.equalsIgnoreCase(queryStart)) {
                        statementType = DBCStatementType.EXEC;
                        break;
                    }
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
        return session.prepareStatement(
            statementType,
            query,
            dataSource.getInfo().supportsResultSetScroll(),
            false,
            false);
    }

    public static void fireObjectUpdate(DBSObject object)
    {
        fireObjectUpdate(object, null);
    }

    public static void fireObjectUpdate(DBSObject object, @Nullable Object data)
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

    @Nullable
    public static DBSDataSourceContainer getContainer(DBSObject object)
    {
        if (object == null) {
            log.warn("Null object passed");
            return null;
        }
        return object.getDataSource().getContainer();
    }

    @NotNull
    public static String getObjectUniqueName(DBSObject object)
    {
        if (object instanceof DBSObjectUnique) {
            return ((DBSObjectUnique) object).getUniqueName();
        } else {
            return object.getName();
        }
    }

    @Nullable
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

    @Nullable
    public static DBSDataType resolveDataType(
        DBRProgressMonitor monitor,
        DBPDataSource dataSource,
        String fullTypeName)
        throws DBException
    {
        DBPDataTypeProvider dataTypeProvider = getAdapter(DBPDataTypeProvider.class, dataSource);
        if (dataTypeProvider == null) {
            // NoSuchElementException data type provider
            return null;
        }
        DBSDataType dataType = dataTypeProvider.resolveDataType(monitor, fullTypeName);
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

    public static String getDefaultValueDisplayString(@Nullable Object value, DBDDisplayFormat format)
    {
        if (isNullValue(value)) {
            if (format == DBDDisplayFormat.UI) {
                return DBConstants.NULL_VALUE_LABEL;
            } else {
                return "";
            }
        }
        if (value instanceof CharSequence) {
            return value.toString();
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
            return ((DBSDataSourceContainer) object).getDataSource();
        } else {
            return object;
        }
    }

    @NotNull
    public static DBPDataSourceRegistry getRegistry(@NotNull DBSObject object)
    {
        DBSDataSourceContainer container;
        if (object instanceof DBSDataSourceContainer) {
            container = (DBSDataSourceContainer) object;
        } else {
            DBPDataSource dataSource = object.getDataSource();
            container = dataSource.getContainer();
        }
        return container.getRegistry();
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
    public static String getObjectFullName(@NotNull DBPNamedObject object)
    {
        if (object instanceof DBPQualifiedObject) {
            return ((DBPQualifiedObject) object).getFullQualifiedName();
        } else if (object instanceof DBSObject) {
            return getObjectFullName(((DBSObject) object).getDataSource(), object);
        } else {
            return object.getName();
        }
    }

    @NotNull
    public static String getObjectFullName(@NotNull DBPDataSource dataSource, @NotNull DBPNamedObject object)
    {
        if (object instanceof DBPQualifiedObject) {
            return ((DBPQualifiedObject) object).getFullQualifiedName();
        } else {
            return getQuotedIdentifier(dataSource, object.getName());
        }
    }

    @NotNull
    public static String getFullTypeName(@NotNull DBSTypedObject typedObject)
    {
        String typeName = typedObject.getTypeName();
        switch (typedObject.getDataKind()) {
            case STRING:
            case LOB:
                return typeName + "(" + typedObject.getMaxLength() + ")";
            default: return typeName;
        }
    }

    @NotNull
    public static String generateScript(IDatabasePersistAction[] persistActions)
    {
        String lineSeparator = ContentUtils.getDefaultLineSeparator();
        StringBuilder script = new StringBuilder(512);
        for (IDatabasePersistAction action : ArrayUtils.safeArray(persistActions)) {
            if (script.length() > 0) {
                script.append(lineSeparator);
            }
            script.append(action.getScript());
            script.append(SQLConstants.DEFAULT_STATEMENT_DELIMITER).append(lineSeparator);
        }
        return script.toString();
    }

    @NotNull
    public static DBIcon getDataIcon(@NotNull DBSTypedObject type)
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
            case ROWID:
                return DBIcon.TYPE_ROWID;
            case OBJECT:
                return DBIcon.TYPE_OBJECT;
            case ANY:
                return DBIcon.TYPE_ANY;
            default:
                return DBIcon.TYPE_UNKNOWN;
        }
    }

    @NotNull
    public static DBDBinaryFormatter getBinaryPresentation(@NotNull DBPDataSource dataSource)
    {
        String id = dataSource.getContainer().getPreferenceStore().getString(DBeaverPreferences.RESULT_SET_BINARY_PRESENTATION);
        if (id != null) {
            DBDBinaryFormatter formatter = getBinaryPresentation(id);
            if (formatter != null) {
                return formatter;
            }
        }
        return DBDBinaryFormatter.FORMATS[0];
    }

    @Nullable
    public static DBDBinaryFormatter getBinaryPresentation(String id)
    {
        for (DBDBinaryFormatter formatter : DBDBinaryFormatter.FORMATS) {
            if (formatter.getId().equals(id)) {
                return formatter;
            }
        }
        return null;
    }

    public static boolean showDatabaseError(Shell shell, String title, String message, DBException error)
    {
        DBPDataSource dataSource = error.getDataSource();
        if (dataSource instanceof DBPErrorAssistant) {
            DBPErrorAssistant.ErrorType errorType = ((DBPErrorAssistant) dataSource).discoverErrorType(error);
            if (errorType == DBPErrorAssistant.ErrorType.CONNECTION_LOST) {
                DataSourceInvalidateHandler.showConnectionLostDialog(shell, message, error);
                return true;
            }
        }
        return false;
    }

    public static boolean equalAttributes(DBSAttributeBase attr1, DBSAttributeBase attr2) {
        return
            attr1.getOrdinalPosition() == attr2.getOrdinalPosition() &&
            attr1.isRequired() == attr2.isRequired() &&
            attr1.getMaxLength() == attr2.getMaxLength() &&
            CommonUtils.equalObjects(attr1.getName(), attr2.getName()) &&
            attr1.getPrecision() == attr2.getPrecision() &&
            attr1.getScale() == attr2.getScale() &&
            attr1.getTypeID() == attr2.getTypeID() &&
            CommonUtils.equalObjects(attr1.getTypeName(), attr2.getTypeName());
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

    public static DBCLogicalOperator[] getDefaultOperators(DBSAttributeBase attribute) {
        List<DBCLogicalOperator> operators = new ArrayList<DBCLogicalOperator>();
        DBPDataKind dataKind = attribute.getDataKind();
        if (!attribute.isRequired()) {
            operators.add(DBCLogicalOperator.IS_NULL);
            operators.add(DBCLogicalOperator.IS_NOT_NULL);
        }
        if (dataKind == DBPDataKind.BOOLEAN || dataKind == DBPDataKind.ROWID) {
            operators.add(DBCLogicalOperator.EQUALS);
            operators.add(DBCLogicalOperator.NOT_EQUALS);
        }
        if (dataKind == DBPDataKind.NUMERIC || dataKind == DBPDataKind.DATETIME || dataKind == DBPDataKind.STRING) {
            operators.add(DBCLogicalOperator.EQUALS);
            operators.add(DBCLogicalOperator.NOT_EQUALS);
            operators.add(DBCLogicalOperator.GREATER);
            //operators.add(DBCLogicalOperator.GREATER_EQUALS);
            operators.add(DBCLogicalOperator.LESS);
            //operators.add(DBCLogicalOperator.LESS_EQUALS);
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
                    if (getConstraintColumn(monitor, index, attribute) != null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
