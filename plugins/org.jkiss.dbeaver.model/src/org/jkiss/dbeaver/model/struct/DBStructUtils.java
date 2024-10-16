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
package org.jkiss.dbeaver.model.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeBindingMeta;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.DBERegistry;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.SubTaskProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDataTypeConverter;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSView;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * DBUtils
 */
public final class DBStructUtils {

    private static final Log log = Log.getLog(DBStructUtils.class);

    private static final String INT_DATA_TYPE = "int";
    private static final String INTEGER_DATA_TYPE = "integer";
    private static final String FLOAT_DATA_TYPE = "float";
    private static final String REAL_DATA_TYPE = "real";
    private static final String DOUBLE_DATA_TYPE = "double";
    private static final String TEXT_DATA_TYPE = "text";
    private static final String VARCHAR_DATA_TYPE = "varchar";
    private static final String VARCHAR2_DATA_TYPE = "varchar2";
    private static final int DEFAULT_VARCHAR_LENGTH = 100;

    @Nullable
    public static DBSEntityReferrer getEnumerableConstraint(@NotNull DBRProgressMonitor monitor, @NotNull DBDAttributeBinding attribute) throws DBException {
        DBSEntityAttribute entityAttribute = attribute.getEntityAttribute();
        if (entityAttribute != null) {
            return getEnumerableConstraint(monitor, entityAttribute);
        }
        return null;
    }

    @Nullable
    public static DBSEntityReferrer getEnumerableConstraint(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntityAttribute entityAttribute) throws DBException {
        List<DBSEntityReferrer> refs = DBUtils.getAttributeReferrers(monitor, entityAttribute, true);
        DBSEntityReferrer constraint = refs.isEmpty() ? null : refs.get(0);
        if (constraint != null) {
            DBSEntity associatedEntity = getAssociatedEntity(monitor, constraint);
            if (associatedEntity instanceof DBSDictionary dictionary) {
                if (dictionary.supportsDictionaryEnumeration()) {
                    return constraint;
                }
            }
        }
        return null;
    }

    @Nullable
    public static DBSEntity getAssociatedEntity(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntityConstraint constraint) throws DBException {
        if (constraint instanceof DBSEntityAssociationLazy associationLazy) {
            return associationLazy.getAssociatedEntity(monitor);
        } else if (constraint instanceof DBSEntityAssociation association) {
            return association.getAssociatedEntity();
        }
        return null;
    }

    public static String generateTableDDL(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntity table, Map<String, Object> options, boolean addComments) throws DBException {
        final DBERegistry editorsRegistry = DBWorkbench.getPlatform().getEditorsRegistry();
        final SQLObjectEditor<?, ?> entityEditor = editorsRegistry.getObjectManager(table.getClass(), SQLObjectEditor.class);
        if (entityEditor instanceof SQLTableManager tableManager) {
            DBEPersistAction[] ddlActions = tableManager.getTableDDL(monitor, table, options);
            return SQLUtils.generateScript(table.getDataSource(), ddlActions, addComments);
        }
        log.debug("Table editor not found for " + table.getClass().getName());
        return SQLUtils.generateCommentLine(table.getDataSource(), "Can't generate DDL: table editor not found for " + table.getClass().getName());
    }

    public static String generateObjectDDL(@NotNull DBRProgressMonitor monitor, @NotNull DBSObject object, Map<String, Object> options, boolean addComments) throws DBException {
        final DBERegistry editorsRegistry = DBWorkbench.getPlatform().getEditorsRegistry();
        final SQLObjectEditor entityEditor = editorsRegistry.getObjectManager(object.getClass(), SQLObjectEditor.class);
        if (entityEditor != null) {
            SQLObjectEditor.ObjectCreateCommand createCommand = entityEditor.makeCreateCommand(object, options);
            DBEPersistAction[] ddlActions = createCommand.getPersistActions(monitor, DBUtils.getDefaultContext(object, true), options);

            return SQLUtils.generateScript(object.getDataSource(), ddlActions, addComments);
        }
        log.debug("Object editor not found for " + object.getClass().getName());
        return SQLUtils.generateCommentLine(object.getDataSource(), "Can't generate DDL: object editor not found for " + object.getClass().getName());
    }

    public static String getTableDDL(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntity table, Map<String, Object> options, boolean addComments) throws DBException {
        if (table instanceof DBPScriptObject scriptObject) {
            String definitionText = scriptObject.getObjectDefinitionText(monitor, options);
            if (!CommonUtils.isEmpty(definitionText)) {
                return definitionText;
            }
        }
        return generateTableDDL(monitor, table, options, addComments);
    }

    public static <T extends DBSEntity> void generateTableListDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull StringBuilder sql,
        @NotNull Collection<T> tablesOrViews,
        Map<String, Object> options,
        boolean addComments
    ) throws DBException {
        List<T> goodTableList = new ArrayList<>();
        List<T> cycleTableList = new ArrayList<>();
        List<T> viewList = new ArrayList<>();

        DBStructUtils.sortTableList(monitor, tablesOrViews, goodTableList, cycleTableList, viewList);

        // Good tables: generate full DDL
        for (T table : goodTableList) {
            sql.append(getObjectNameComment(table, ModelMessages.struct_utils_object_ddl_definition));
            addDDLLine(sql, DBStructUtils.getTableDDL(monitor, table, options, addComments));
        }
        {
            // Cycle tables: generate CREATE TABLE and CREATE FOREIGN KEY separately
            // This doesn't work if table implementation doesn't support DDL restructure
            List<T> goodCycleTableList = new ArrayList<>();
            for (T table : cycleTableList) {
                if (
                    table instanceof DBPScriptObjectExt2 so2 &&
                    so2.supportsObjectDefinitionOption(DBPScriptObject.OPTION_DDL_SKIP_FOREIGN_KEYS) &&
                    so2.supportsObjectDefinitionOption(DBPScriptObject.OPTION_DDL_ONLY_FOREIGN_KEYS))
                {
                    goodCycleTableList.add(table);
                }
            }
            cycleTableList.removeAll(goodCycleTableList);

            if (!CommonUtils.getOption(options, DBPScriptObject.OPTION_DDL_SEPARATE_FOREIGN_KEYS_STATEMENTS, true)) {
                for (T table : goodCycleTableList) {
                    sql.append(getObjectNameComment(table, ModelMessages.struct_utils_object_ddl_definition));
                    addDDLLine(sql, DBStructUtils.getTableDDL(monitor, table, options, addComments));
                }
            } else {
                Map<String, Object> optionsNoFK = new HashMap<>(options);
                optionsNoFK.put(DBPScriptObject.OPTION_DDL_SKIP_FOREIGN_KEYS, true);
                for (T table : goodCycleTableList) {
                    sql.append(getObjectNameComment(table, ModelMessages.struct_utils_object_ddl_definition));
                    addDDLLine(sql, DBStructUtils.getTableDDL(monitor, table, optionsNoFK, addComments));
                }
                Map<String, Object> optionsOnlyFK = new HashMap<>(options);
                optionsOnlyFK.put(DBPScriptObject.OPTION_DDL_ONLY_FOREIGN_KEYS, true);
                for (T table : goodCycleTableList) {
                    sql.append(getObjectNameComment(table, ModelMessages.struct_utils_object_ddl_foreign_keys));
                    addDDLLine(sql, DBStructUtils.getTableDDL(monitor, table, optionsOnlyFK, addComments));
                }
            }

            // the rest - tables which can't split their DDL
            for (T table : cycleTableList) {
                sql.append(getObjectNameComment(table, ModelMessages.struct_utils_object_ddl_definition));
                addDDLLine(sql, DBStructUtils.getTableDDL(monitor, table, options, addComments));
            }
        }
        // Views: generate them after all tables.
        // TODO: find view dependencies and generate them in right order
        for (T table : viewList) {
            sql.append(getObjectNameComment(table, ModelMessages.struct_utils_object_ddl_source));
            addDDLLine(sql, DBStructUtils.getTableDDL(monitor, table, options, addComments));
        }
        monitor.done();
    }

    private static String getObjectNameComment(@NotNull DBSObject object, @NotNull String comment) {
        DBPDataSource dataSource = object.getDataSource();
        if (dataSource == null) {
            return "";
        }
        if (!dataSource.getContainer().getPreferenceStore().getBoolean(ModelPreferences.META_EXTRA_DDL_INFO)) {
            // Skip this step, then
            return "";
        }
        String[] singleLineComments = dataSource.getSQLDialect().getSingleLineComments();
        if (ArrayUtils.isEmpty(singleLineComments)) {
            return "";
        }
        String lf = GeneralUtils.getDefaultLineSeparator();
        return singleLineComments[0].trim() + " " + DBUtils.getObjectFullName(object, DBPEvaluationContext.DDL) +
            " " + comment + lf + lf;
    }

    private static void addDDLLine(StringBuilder sql, String ddl) {
        ddl = ddl.trim();
        if (!CommonUtils.isEmpty(ddl)) {
            sql.append(ddl);
            if (!ddl.endsWith(SQLConstants.DEFAULT_STATEMENT_DELIMITER)) {
                sql.append(SQLConstants.DEFAULT_STATEMENT_DELIMITER);
            }
            String lf = GeneralUtils.getDefaultLineSeparator();
            sql.append(lf).append(lf).append(lf);
        }
    }

    public static <T extends DBSEntity> void sortTableList(DBRProgressMonitor monitor, Collection<T> input, List<T> simpleTables, List<T> cyclicTables, List<T> views) throws DBException {
        monitor.beginTask("Sorting table list", input.size());
        List<T> realTables = new ArrayList<>();
        for (T entity : input) {
            if (entity instanceof DBSView || (entity instanceof DBSTable table && table.isView())) {
                views.add(entity);
            } else {
                realTables.add(entity);
            }
        }
        DBRProgressMonitor proxyMonitor = new SubTaskProgressMonitor(monitor);

        // 1. Get tables without FKs
        for (Iterator<T> iterator = realTables.iterator(); iterator.hasNext(); ) {
            if (monitor.isCanceled()) {
                break;
            }
            T table = iterator.next();
            try {
                if (CommonUtils.isEmpty(table.getAssociations(proxyMonitor))) {
                    simpleTables.add(table);
                    iterator.remove();
                }
            } catch (DBException e) {
                log.debug(e);
            }
            monitor.worked(1);
        }

        // 2. Get tables referring tables from p.1 only
        // 3. Repeat p.2 until something is found
        boolean refsFound = true;
        while (refsFound) {
            if (monitor.isCanceled()) {
                break;
            }
            refsFound = false;
            for (Iterator<T> iterator = realTables.iterator(); iterator.hasNext(); ) {
                if (monitor.isCanceled()) {
                    break;
                }
                T table = iterator.next();
                try {
                    boolean allGood = true;
                    for (DBSEntityAssociation ref : CommonUtils.safeCollection(table.getAssociations(proxyMonitor))) {
                        monitor.worked(1);
                        DBSEntity refEntity = ref.getAssociatedEntity();
                        if (refEntity == null || (!simpleTables.contains(refEntity) && refEntity != table)) {
                            allGood = false;
                            break;
                        }
                    }
                    if (allGood) {
                        simpleTables.add(table);
                        iterator.remove();
                        refsFound = true;
                    }
                } catch (DBException e) {
                    log.error(e);
                }
            }
        };

        // 4. The rest is cycled tables
        cyclicTables.addAll(realTables);
        monitor.done();
    }

    public static String mapTargetDataType(
        @Nullable DBSObject objectContainer,
        @NotNull DBSTypedObject srcTypedObject,
        boolean addModifiers
    ) {
        boolean isBindingWithEntityAttr = false;
        if (srcTypedObject instanceof DBDAttributeBinding attributeBinding) {
            if (attributeBinding.getEntityAttribute() != null) {
                isBindingWithEntityAttr = true;
            }
        }
        if (objectContainer != null && (srcTypedObject instanceof DBSEntityAttribute || isBindingWithEntityAttr || (
                srcTypedObject instanceof DBSObject dbsObject &&  objectContainer.getDataSource() == dbsObject.getDataSource()))) {
            // If source and target datasources have the same type then just return the same type name
            DBPDataSource srcDataSource = ((DBSObject) srcTypedObject).getDataSource();
            assert srcDataSource != null;
            DBPDataSource tgtDataSource = objectContainer.getDataSource();
            assert tgtDataSource != null;
            if (srcDataSource.getClass() == tgtDataSource.getClass() && addModifiers) {
                return srcTypedObject.getFullTypeName();
            }
        }

        {
            SQLDataTypeConverter dataTypeConverter = objectContainer == null || objectContainer.getDataSource() == null ? null :
                DBUtils.getAdapter(SQLDataTypeConverter.class, objectContainer.getDataSource().getSQLDialect());
            if (dataTypeConverter != null && srcTypedObject instanceof DBSObject dbsObject) {
                DBPDataSource srcDataSource = dbsObject.getDataSource();
                assert srcDataSource != null;
                DBPDataSource tgtDataSource = objectContainer.getDataSource();
                String targetTypeName = dataTypeConverter.convertExternalDataType(
                    srcDataSource.getSQLDialect(),
                    srcTypedObject, DBUtils.getAdapter(DBPDataTypeProvider.class, tgtDataSource)
                );
                if (targetTypeName != null) {
                    return targetTypeName;
                }
            }
        }

        String typeName = srcTypedObject.getTypeName();
        String typeNameLower = typeName.toLowerCase(Locale.ENGLISH);
        DBPDataKind dataKind = srcTypedObject.getDataKind();

        DBPDataTypeProvider dataTypeProvider = DBUtils.getParentOfType(DBPDataTypeProvider.class, objectContainer);
        if (dataTypeProvider != null) {
            DBSDataType dataType = dataTypeProvider.getLocalDataType(typeName);
            if (dataType == null && typeName.contains("(")) {
                // It seems this data type has modifiers. Try to find without modifiers
                dataType = dataTypeProvider.getLocalDataType(SQLUtils.stripColumnTypeModifiers(typeName));
                if (dataType != null) {
                    int startPos = typeName.indexOf("(");
                    typeName = dataType + typeName.substring(startPos);
                }
            }
            if (dataType == null && typeNameLower.equals(DOUBLE_DATA_TYPE)) {
                dataType = dataTypeProvider.getLocalDataType("DOUBLE PRECISION");
                if (dataType != null) {
                    typeName = dataType.getTypeName();
                }
            }
            if (dataType != null && !DBPDataKind.canConsume(dataKind, dataType.getDataKind())) {
                // Type mismatch
                dataType = null;
            }
            if (dataType == null) {
                // Type not supported by target database
                // Let's try to find something similar
                Map<String, DBSDataType> possibleTypes = new LinkedHashMap<>();
                for (DBSDataType type : dataTypeProvider.getLocalDataTypes()) {
                    if (DBPDataKind.canConsume(type.getDataKind(), dataKind)) {
                        possibleTypes.put(type.getTypeName().toLowerCase(Locale.ENGLISH), type);
                    }
                }
                DBSDataType targetType = null;
                if (!possibleTypes.isEmpty()) {
                    // Try to get any partial match
                    targetType = possibleTypes.get(typeNameLower);
                    if (targetType == null && dataKind == DBPDataKind.NUMERIC) {
                        // Try to find appropriate type with the same scale/precision
                        for (DBSDataType type : possibleTypes.values()) {
                            if (CommonUtils.equalObjects(type.getScale(), srcTypedObject.getScale()) &&
                                CommonUtils.equalObjects(type.getPrecision(), srcTypedObject.getPrecision()))
                            {
                                targetType = type;
                                break;
                            }
                        }
                        if (targetType == null) {
                            if (typeNameLower.contains(FLOAT_DATA_TYPE) ||
                                typeNameLower.contains(REAL_DATA_TYPE) ||
                                (srcTypedObject.getScale() != null && srcTypedObject.getScale() > 0 && srcTypedObject.getScale() <= 6))
                            {
                                for (String psn : possibleTypes.keySet()) {
                                    if (psn.contains(FLOAT_DATA_TYPE) || psn.contains(REAL_DATA_TYPE)) {
                                        targetType = possibleTypes.get(psn);
                                        break;
                                    }
                                }
                            } else if (typeNameLower.contains(DOUBLE_DATA_TYPE) ||
                                (srcTypedObject.getScale() != null && srcTypedObject.getScale() > 0 && srcTypedObject.getScale() <= 15))
                            {
                                for (String psn : possibleTypes.keySet()) {
                                    if (psn.contains(DOUBLE_DATA_TYPE)) {
                                        targetType = possibleTypes.get(psn);
                                        break;
                                    }
                                }
                            } else if ((INT_DATA_TYPE.equals(typeNameLower) && possibleTypes.get(INTEGER_DATA_TYPE) != null)
                                || (INTEGER_DATA_TYPE.equals(typeNameLower) && possibleTypes.get(INT_DATA_TYPE) != null))
                            {
                                // Let's use the closest int/integer synonym
                                targetType = INT_DATA_TYPE.equals(typeNameLower)
                                    ? possibleTypes.get(INTEGER_DATA_TYPE) : possibleTypes.get(INT_DATA_TYPE);

                            } else if (typeNameLower.contains(INT_DATA_TYPE)) {
                                // Ok, probably we do not have int/integer types, let's find something similar
                                for (String psn : possibleTypes.keySet()) {
                                    if (psn.contains(INT_DATA_TYPE)) {
                                        targetType = possibleTypes.get(psn);
                                        break;
                                    }
                                }
                            }
                        }
                    } else if (targetType == null && dataKind == DBPDataKind.STRING) {
                        if (typeNameLower.contains(TEXT_DATA_TYPE) || srcTypedObject.getMaxLength() <= 0) {
                            // Search data types ending with "text" for the source data type including "text".
                            // Like "longtext", "ntext", "mediumtext".
                            // Other string data types can also be turned into the "text" data type if they have no length.
                            if (possibleTypes.containsKey(TEXT_DATA_TYPE)) {
                                targetType = possibleTypes.get(TEXT_DATA_TYPE);
                            } else {
                                for (Map.Entry<String, DBSDataType> type : possibleTypes.entrySet()) {
                                    if (type.getKey().endsWith(TEXT_DATA_TYPE) && type.getValue().getDataKind() == DBPDataKind.STRING) {
                                        targetType = type.getValue();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                if (targetType == null) {
                    typeName = DBUtils.getDefaultDataTypeName(objectContainer, dataKind);
                    typeNameLower = typeName.toLowerCase(Locale.ENGLISH);
                    if (!possibleTypes.isEmpty()) {
                        targetType = possibleTypes.get(typeNameLower);
                    }
                }
                if (targetType == null && !possibleTypes.isEmpty()) {
                    targetType = possibleTypes.values().iterator().next();
                }
                if (targetType != null) {
                    typeName = targetType.getTypeName();
                }
            }
            if (dataType != null) {
                dataKind = dataType.getDataKind();
                // Datatype caches ignore case, but we probably should use it with the original case
                if (typeName.equalsIgnoreCase(dataType.getTypeName())) {
                    typeName = dataType.getTypeName();
                }
            }
        }

        // Get type modifiers from target datasource
        if (addModifiers && objectContainer != null) {
            DBPDataSource dataSource = objectContainer.getDataSource();
            SQLDialect dialect = dataSource.getSQLDialect();
            String modifiers = dialect.getColumnTypeModifiers(dataSource, srcTypedObject, typeName, dataKind);
            if (modifiers != null) {
                typeName += modifiers;
            } else if (VARCHAR_DATA_TYPE.equals(typeNameLower) || VARCHAR2_DATA_TYPE.equals(typeNameLower)) {
                // Default max length value for varchar column, because many databases do not support varchar without modifiers.
                // VARCHAR2 - is a special Oracle and Oracle-based databases case.
                typeName += "(" + DEFAULT_VARCHAR_LENGTH + ")";
            }
        }
        return typeName;
    }

    /**
     * Get name of the attribute

     * @param attribute to get name of
     * @return attribute name
     */
    public static String getAttributeName(@NotNull DBSAttributeBase attribute) {
        return getAttributeName(attribute, DBPAttributeReferencePurpose.UNSPECIFIED);
    }

    /**
     * Get name of the attribute

     * @param attribute to get name of
     * @param purpose of the name usage
     * @return attribute name
     */
    public static String getAttributeName(@NotNull DBSAttributeBase attribute, DBPAttributeReferencePurpose purpose) {
        if (attribute instanceof DBDAttributeBindingMeta bindingMeta) {
            // For top-level query bindings we need to use table columns name instead of alias.
            // For nested attributes we should use aliases

            // Entity attribute obtain commented because it broke complex attributes full name construction
            // We can't use entity attr because only particular query metadata contains real structure
            DBSEntityAttribute entityAttribute = bindingMeta.getEntityAttribute();
            if (entityAttribute != null) {
                attribute = entityAttribute;
            }
        }
        // Do not quote pseudo attribute name
        return DBUtils.isPseudoAttribute(attribute)
            ? attribute.getName()
            : DBUtils.getObjectFullName(attribute, DBPEvaluationContext.DML);
    }

    public static boolean isConnectedContainer(DBPObject parent) {
        return !(parent instanceof DBSInstanceLazy il) || il.isInstanceConnected();
    }
}
