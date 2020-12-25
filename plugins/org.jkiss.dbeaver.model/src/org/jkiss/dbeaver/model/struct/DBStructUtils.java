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
package org.jkiss.dbeaver.model.struct;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.DBERegistry;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
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

    public static String generateTableDDL(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntity table, Map<String, Object> options, boolean addComments) throws DBException {
        final DBERegistry editorsRegistry = table.getDataSource().getContainer().getPlatform().getEditorsRegistry();
        final SQLObjectEditor entityEditor = editorsRegistry.getObjectManager(table.getClass(), SQLObjectEditor.class);
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
        List<T> realTables = new ArrayList<>();
        for (T entity : input) {
            if (entity instanceof DBSView || (entity instanceof DBSTable && ((DBSTable) entity).isView())) {
                views.add(entity);
            } else {
                realTables.add(entity);
            }
        }

        // 1. Get tables without FKs
        for (Iterator<T> iterator = realTables.iterator(); iterator.hasNext(); ) {
            T table = iterator.next();
            try {
                if (CommonUtils.isEmpty(table.getAssociations(monitor))) {
                    simpleTables.add(table);
                    iterator.remove();
                }
            } catch (DBException e) {
                log.debug(e);
            }
        }

        // 2. Get tables referring tables from p.1 only
        // 3. Repeat p.2 until something is found
        boolean refsFound = true;
        while (refsFound) {
            refsFound = false;
            for (Iterator<T> iterator = realTables.iterator(); iterator.hasNext(); ) {
                T table = iterator.next();
                try {
                    boolean allGood = true;
                    for (DBSEntityAssociation ref : CommonUtils.safeCollection(table.getAssociations(monitor))) {
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
    }

    public static String mapTargetDataType(DBSObject objectContainer, DBSTypedObject typedObject, boolean addModifiers) {
        if (typedObject instanceof DBSObject) {
            // If source and target datasources have the same type then just return the same type name
            if (((DBSObject) typedObject).getDataSource().getClass() == objectContainer.getDataSource().getClass() && addModifiers) {
                return typedObject.getFullTypeName();
            }
        }
        String typeName = typedObject.getTypeName();
        String typeNameLower = typeName.toLowerCase(Locale.ENGLISH);
        DBPDataKind dataKind = typedObject.getDataKind();
        if (objectContainer instanceof DBPDataTypeProvider) {
            DBPDataTypeProvider dataTypeProvider = (DBPDataTypeProvider) objectContainer;
            DBSDataType dataType = dataTypeProvider.getLocalDataType(typeName);
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
                Map<String, DBSDataType> possibleTypes = new HashMap<>();
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
                            if (typeNameLower.contains("float")) {
                                for (String psn : possibleTypes.keySet()) {
                                    if (psn.contains("float")) {
                                        targetType = possibleTypes.get(psn);
                                        break;
                                    }
                                }
                            } else if (typeNameLower.contains("double")) {
                                for (String psn : possibleTypes.keySet()) {
                                    if (psn.contains("double")) {
                                        targetType = possibleTypes.get(psn);
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
        if (addModifiers && objectContainer instanceof DBPDataSource) {
            SQLDialect dialect = ((DBPDataSource) objectContainer).getSQLDialect();
            String modifiers = dialect.getColumnTypeModifiers((DBPDataSource)objectContainer, typedObject, typeName, dataKind);
            if (modifiers != null) {
                typeName += modifiers;
            }
        }
        return typeName;
    }
}
