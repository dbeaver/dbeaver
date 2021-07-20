/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.DBERegistry;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.SubTaskProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSView;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * DBUtils
 */
public final class DBStructUtils {

    private static final Log log = Log.getLog(DBStructUtils.class);

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
            if (associatedEntity instanceof DBSDictionary) {
                final DBSDictionary dictionary = (DBSDictionary) associatedEntity;
                if (dictionary.supportsDictionaryEnumeration()) {
                    return constraint;
                }
            }
        }
        return null;
    }

    @Nullable
    public static DBSEntity getAssociatedEntity(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntityConstraint constraint) throws DBException {
        if (constraint instanceof DBSEntityAssociationLazy) {
            return  ((DBSEntityAssociationLazy) constraint).getAssociatedEntity(monitor);
        } else if (constraint instanceof DBSEntityAssociation) {
            return  ((DBSEntityAssociation) constraint).getAssociatedEntity();
        }
        return null;
    }

    public static String generateTableDDL(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntity table, Map<String, Object> options, boolean addComments) throws DBException {
        final DBERegistry editorsRegistry = table.getDataSource().getContainer().getPlatform().getEditorsRegistry();
        final SQLObjectEditor<?, ?> entityEditor = editorsRegistry.getObjectManager(table.getClass(), SQLObjectEditor.class);
        if (entityEditor instanceof SQLTableManager) {
            DBEPersistAction[] ddlActions = ((SQLTableManager) entityEditor).getTableDDL(monitor, table, options);
            return SQLUtils.generateScript(table.getDataSource(), ddlActions, addComments);
        }
        log.debug("Table editor not found for " + table.getClass().getName());
        return SQLUtils.generateCommentLine(table.getDataSource(), "Can't generate DDL: table editor not found for " + table.getClass().getName());
    }

    public static String generateObjectDDL(@NotNull DBRProgressMonitor monitor, @NotNull DBSObject object, Map<String, Object> options, boolean addComments) throws DBException {
        final DBERegistry editorsRegistry = object.getDataSource().getContainer().getPlatform().getEditorsRegistry();
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
        if (table instanceof DBPScriptObject) {
            String definitionText = ((DBPScriptObject) table).getObjectDefinitionText(monitor, options);
            if (!CommonUtils.isEmpty(definitionText)) {
                return definitionText;
            }
        }
        return generateTableDDL(monitor, table, options, addComments);
    }

    public static <T extends DBSEntity> void generateTableListDDL(@NotNull DBRProgressMonitor monitor, @NotNull StringBuilder sql, @NotNull Collection<T> tablesOrViews, Map<String, Object> options, boolean addComments) throws DBException {
        List<T> goodTableList = new ArrayList<>();
        List<T> cycleTableList = new ArrayList<>();
        List<T> viewList = new ArrayList<>();

        DBStructUtils.sortTableList(monitor, tablesOrViews, goodTableList, cycleTableList, viewList);

        // Good tables: generate full DDL
        for (T table : goodTableList) {
            sql.append(getObjectNameComment(table, "definition"));
            addDDLLine(sql, DBStructUtils.getTableDDL(monitor, table, options, addComments));
        }
        {
            // Cycle tables: generate CREATE TABLE and CREATE FOREIGN KEY separately
            // This doesn't work if table implementation doesn't support DDL restructure
            List<T> goodCycleTableList = new ArrayList<>();
            for (T table : cycleTableList) {
                if (
                    table instanceof DBPScriptObjectExt2 &&
                    ((DBPScriptObjectExt2) table).supportsObjectDefinitionOption(DBPScriptObject.OPTION_DDL_SKIP_FOREIGN_KEYS) &&
                    ((DBPScriptObjectExt2) table).supportsObjectDefinitionOption(DBPScriptObject.OPTION_DDL_ONLY_FOREIGN_KEYS))
                {
                    goodCycleTableList.add(table);
                }
            }
            cycleTableList.removeAll(goodCycleTableList);

            Map<String, Object> optionsNoFK = new HashMap<>(options);
            optionsNoFK.put(DBPScriptObject.OPTION_DDL_SKIP_FOREIGN_KEYS, true);
            for (T table : goodCycleTableList) {
                sql.append(getObjectNameComment(table, "definition"));
                addDDLLine(sql, DBStructUtils.getTableDDL(monitor, table, optionsNoFK, addComments));
            }
            Map<String, Object> optionsOnlyFK = new HashMap<>(options);
            optionsOnlyFK.put(DBPScriptObject.OPTION_DDL_ONLY_FOREIGN_KEYS, true);
            for (T table : goodCycleTableList) {
                sql.append(getObjectNameComment(table, "foreign keys"));
                addDDLLine(sql, DBStructUtils.getTableDDL(monitor, table, optionsOnlyFK, addComments));
            }

            // the rest - tables which can't split their DDL
            for (T table : cycleTableList) {
                sql.append(getObjectNameComment(table, "definition"));
                addDDLLine(sql, DBStructUtils.getTableDDL(monitor, table, options, addComments));
            }
        }
        // Views: generate them after all tables.
        // TODO: find view dependencies and generate them in right order
        for (T table : viewList) {
            sql.append(getObjectNameComment(table, "source"));
            addDDLLine(sql, DBStructUtils.getTableDDL(monitor, table, options, addComments));
        }
        monitor.done();
    }

    private static String getObjectNameComment(DBSObject object, String comment) {
        String[] singleLineComments = object.getDataSource().getSQLDialect().getSingleLineComments();
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
            if (entity instanceof DBSView || (entity instanceof DBSTable && ((DBSTable) entity).isView())) {
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

    public static String mapTargetDataType(DBSObject objectContainer, DBSTypedObject typedObject, boolean addModifiers) {
        boolean isBindingWithEntityAttr = false;
        if (typedObject instanceof DBDAttributeBinding) {
            DBDAttributeBinding attributeBinding = (DBDAttributeBinding) typedObject;
            if (attributeBinding.getEntityAttribute() != null) {
                isBindingWithEntityAttr = true;
            }
        }
        if (objectContainer != null && (typedObject instanceof DBSEntityAttribute || isBindingWithEntityAttr)) {
            // If source and target datasources have the same type then just return the same type name
            DBPDataSource srcDataSource = ((DBSObject) typedObject).getDataSource();
            DBPDataSource tgtDataSource = objectContainer.getDataSource();
            if (srcDataSource.getClass() == tgtDataSource.getClass() && addModifiers) {
                return typedObject.getFullTypeName();
            }
        }

        String typeName = typedObject.getTypeName();
        String typeNameLower = typeName.toLowerCase(Locale.ENGLISH);
        DBPDataKind dataKind = typedObject.getDataKind();
        DBPDataTypeProvider dataTypeProvider = DBUtils.getParentOfType(DBPDataTypeProvider.class, objectContainer);
        if (dataTypeProvider != null) {
            DBSDataType dataType = dataTypeProvider.getLocalDataType(typeName);
            if (dataType == null && typeName.contains("(")) {
                // It seems this data type has modifiers. Try to find without modifiers
                dataType = dataTypeProvider.getLocalDataType(SQLUtils.stripColumnTypeModifiers(typeName));
            }
            if (dataType == null && typeNameLower.equals("double")) {
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
                            if (CommonUtils.equalObjects(type.getScale(), typedObject.getScale()) &&
                                CommonUtils.equalObjects(type.getPrecision(), typedObject.getPrecision()))
                            {
                                targetType = type;
                                break;
                            }
                        }
                        if (targetType == null) {
                            if (typeNameLower.contains("float") ||
                                typeNameLower.contains("real") ||
                                (typedObject.getScale() != null && typedObject.getScale() > 0 && typedObject.getScale() <= 6))
                            {
                                for (String psn : possibleTypes.keySet()) {
                                    if (psn.contains("float") || psn.contains("real")) {
                                        targetType = possibleTypes.get(psn);
                                        break;
                                    }
                                }
                            } else if (typeNameLower.contains("double") ||
                                (typedObject.getScale() != null && typedObject.getScale() > 0 && typedObject.getScale() <= 15))
                            {
                                for (String psn : possibleTypes.keySet()) {
                                    if (psn.contains("double")) {
                                        targetType = possibleTypes.get(psn);
                                        break;
                                    }
                                }
                            } else if (typeNameLower.contains("int")) {
                                for (String psn : possibleTypes.keySet()) {
                                    if (psn.contains("int")) {
                                        targetType = possibleTypes.get(psn);
                                        break;
                                    }
                                }
                            }
                        }
                    } else if (targetType == null && dataKind == DBPDataKind.STRING) {
                        if (typeNameLower.contains("text")) {
                            if (possibleTypes.containsKey("text")) {
                                targetType = possibleTypes.get("text");
                            } else {
                                for (Map.Entry<String, DBSDataType> type : possibleTypes.entrySet()) {
                                    if (type.getKey().contains("text")) {
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
            }
        }

        // Get type modifiers from target datasource
        if (addModifiers && objectContainer != null) {
            SQLDialect dialect = objectContainer.getDataSource().getSQLDialect();
            String modifiers = dialect.getColumnTypeModifiers((DBPDataSource)objectContainer, typedObject, typeName, dataKind);
            if (modifiers != null) {
                typeName += modifiers;
            }
        }
        return typeName;
    }
}
