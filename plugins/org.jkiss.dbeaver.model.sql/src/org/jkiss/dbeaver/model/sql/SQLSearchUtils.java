/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.LocalCacheProgressMonitor;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionRequest;
import org.jkiss.dbeaver.model.sql.parser.SQLIdentifierDetector;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Metadata search utils
 */
public class SQLSearchUtils {
    private static final Log log = Log.getLog(SQLSearchUtils.class);

    @Nullable
    public static DBSObject findObjectByFQN(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBSObjectContainer objectContainer,
        @Nullable DBCExecutionContext executionContext,
        @NotNull List<String> nameList,
        boolean useAssistant,
        @NotNull SQLIdentifierDetector identifierDetector
    ) {
        return getFirstIfPresented(
            findObjectsByFQN(monitor, objectContainer, executionContext, nameList, useAssistant, identifierDetector, false, false)
        );
    }

    @Nullable
    public static DBSObject findObjectByFQN(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBSObjectContainer objectContainer,
        @Nullable SQLCompletionRequest request,
        @NotNull List<String> nameList
    ) {
        return getFirstIfPresented(findObjectsByFQN(
            monitor,
            objectContainer,
            request.getContext().getExecutionContext(),
            nameList,
            !request.isSimpleMode(),
            request.getWordDetector(),
            request.getContext().isSearchGlobally(),
            false
        ));
    }

    @Nullable
    private static <T> T getFirstIfPresented(@Nullable List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        } else {
            return list.getFirst();
        }
    }

    @NotNull
    public static List<? extends DBSObject> findObjectsByFQN(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBSObjectContainer objectContainer,
        @Nullable DBCExecutionContext executionContext,
        @NotNull List<String> nameList,
        boolean useAssistant,
        @NotNull SQLIdentifierDetector identifierDetector,
        boolean anyObject
    ) {
        return findObjectsByFQN(monitor, objectContainer, executionContext, nameList, useAssistant, identifierDetector, false, anyObject);
    }

    @NotNull
    public static List<? extends DBSObject> findObjectsByFQN(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBSObjectContainer objectContainer,
        @Nullable DBCExecutionContext executionContext,
        @NotNull List<String> nameList,
        boolean useAssistant,
        @NotNull SQLIdentifierDetector identifierDetector,
        boolean isGlobalSearch,
        boolean anyObject
    ) {
        if (nameList.isEmpty()) {
            return Collections.emptyList();
        }
        DBPDataSource dataSource = objectContainer == null ? null : objectContainer.getDataSource();
        if (executionContext == null && dataSource != null) {
            executionContext = DBUtils.getDefaultContext(dataSource, true);
        }
        if (dataSource == null && executionContext != null) {
            dataSource = executionContext.getDataSource();
        }
        if (dataSource == null) {
            return Collections.emptyList();
        }
        DBRProgressMonitor mdMonitor = dataSource.getContainer().isExtraMetadataReadEnabled() ?
            monitor : new LocalCacheProgressMonitor(monitor);
        if (!mdMonitor.isForceCacheUsage()) {
            List<String> unquotedNames = new ArrayList<>(nameList.size());
            for (String name : nameList) {
                unquotedNames.add(DBUtils.getUnQuotedIdentifier(dataSource, name));
            }

            List<? extends DBSObject> result = findObjectsByPath(
                mdMonitor, executionContext, objectContainer, unquotedNames, identifierDetector, useAssistant, isGlobalSearch, anyObject
            );
            if (!result.isEmpty()) {
                return result;
            }
        }
        {
            // Fix names (convert case or remove quotes)
            List<String> transformedNameList = new ArrayList<>(nameList);
            for (int i = 0; i < nameList.size(); i++) {
                String name = nameList.get(i);
                String unquotedName = DBUtils.getUnQuotedIdentifier(dataSource, name);
                if (!unquotedName.equals(name)) {
                    name = unquotedName;
                } else {
                    name = DBObjectNameCaseTransformer.transformName(objectContainer.getDataSource(), name);
                }
                transformedNameList.set(i, name);
            }
            return findObjectsByPath(mdMonitor, executionContext, objectContainer, transformedNameList, identifierDetector, useAssistant, isGlobalSearch, anyObject);
        }
    }

    @Nullable
    public static DBSObject findObjectByPath(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull DBSObjectContainer objectContainer,
        @NotNull List<String> nameList,
        @NotNull SQLIdentifierDetector identifierDetector,
        boolean useAssistant
    ) {
        return getFirstIfPresented(
            findObjectsByPath(monitor, executionContext, objectContainer, nameList, identifierDetector, useAssistant, false, false)
        );
    }

    @NotNull
    public static List<? extends DBSObject> findObjectsByPath(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull DBSObjectContainer sc,
        @NotNull List<String> nameList,
        @NotNull SQLIdentifierDetector identifierDetector,
        boolean useAssistant,
        boolean isGlobalSearch,
        boolean anyObject
    ) {
        try {
            // Find using context defaults
            if (!nameList.isEmpty()) {
                DBCExecutionContextDefaults<?, ?> contextDefaults = executionContext.getContextDefaults();
                if (contextDefaults != null) {
                    if (nameList.size() == 1) {
                        DBSObjectContainer defaultSchema = contextDefaults.getDefaultSchema();
                        if (defaultSchema != null) {
                            DBSObject entity = defaultSchema.getChild(monitor, nameList.get(0));
                            if (entity != null) {
                                return List.of(entity);
                            } else if (anyObject && defaultSchema instanceof DBSProcedureContainer procsContainer) {
                                List<? extends DBSObject> objs = findProcedures(monitor, procsContainer, nameList.get(0));
                                if (objs.size() > 0) {
                                    return objs;
                                }
                            }
                        }
                    }
                    if (!nameList.isEmpty()) {
                        DBSObjectContainer catalog = contextDefaults.getDefaultCatalog();
                        if (catalog != null) {
                            DBSObject childObject = catalog.getChild(monitor, nameList.get(0));
                            if (childObject != null) {
                                if (nameList.size() == 1) {
                                    return List.of(childObject);
                                }
                                if (nameList.size() == 2) {
                                    if (childObject instanceof DBSObjectContainer schema) {
                                        DBSObject entity = schema.getChild(monitor, nameList.get(1));
                                        if (entity != null) {
                                            return List.of(entity);
                                        }
                                    }
                                    if (anyObject && childObject instanceof DBSProcedureContainer childProcsContainer) {
                                        List<? extends DBSObject> objs = findProcedures(monitor, childProcsContainer, nameList.get(1));
                                        if (objs.size() > 0) {
                                            return objs;
                                        }
                                    }
                                }
                            }
                            if (anyObject && catalog instanceof DBSProcedureContainer childProcsContainer) {
                                List<? extends DBSObject> objs = findProcedures(monitor, childProcsContainer, nameList.get(0));
                                if (!objs.isEmpty()) {
                                    return objs;
                                }
                            }
                        }
                    }
                }
            }

            // Find structu containers
            List<? extends DBSObject> childObject = null;
            while (childObject == null) {
                childObject = findNestedObjects(monitor, executionContext, sc, nameList, anyObject);
                if (childObject.isEmpty()) {
                    DBSObjectContainer parentSc = DBUtils.getParentAdapter(DBSObjectContainer.class, sc);
                    if (parentSc == null) {
                        break;
                    }
                    sc = parentSc;
                }
            }
            if (childObject.isEmpty() && nameList.size() <= 1) {
                if (useAssistant && !monitor.isForceCacheUsage()) {
                    // No such object found - may be it's start of table name
                    DBSStructureAssistant structureAssistant = DBUtils.getAdapter(DBSStructureAssistant.class, sc);
                    if (structureAssistant != null) {
                        String objectNameMask = nameList.get(0);
                        DBSStructureAssistant.ObjectsSearchParams params = new DBSStructureAssistant.ObjectsSearchParams(
                                structureAssistant.getAutoCompleteObjectTypes(),
                                identifierDetector.removeQuotes(objectNameMask)
                        );
                        params.setParentObject(sc);
                        params.setCaseSensitive(identifierDetector.isQuoted(objectNameMask));
                        params.setMaxResults(2);
                        params.setGlobalSearch(isGlobalSearch);
                        Collection<DBSObjectReference> tables = structureAssistant.findObjectsByMask(monitor, executionContext, params);
                        if (!tables.isEmpty()) {
                            return List.of(tables.iterator().next().resolveObject(monitor)); // consider all matching tables
                        }
                    }
                }
                return Collections.emptyList();
            } else {
                return childObject;
            }
        } catch (DBException e) {
            log.error(e);
            return Collections.emptyList();
        }
    }

    @NotNull
    public static List<? extends DBSObject> findNestedObjects(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull DBSObjectContainer parent,
        @NotNull List<String> names,
        boolean anyObject
    ) throws DBException {
        for (int i = 0; i < names.size(); i++) {
            if (monitor.isCanceled()) {
                break;
            }
            String childName = names.get(i);
            parent.cacheStructure(monitor, DBSObjectContainer.STRUCT_ENTITIES);
            DBSObject child = parent.getChild(monitor, childName);
            if (!DBStructUtils.isConnectedContainer(child)) {
                child = null;
            }
            if (anyObject && child == null && parent instanceof DBSProcedureContainer procsContainer
                && parent.getDataSource().getInfo().supportsStoredCode()
            ) {
                List<? extends DBSObject> objs = findProcedures(monitor, procsContainer, childName);
                if (objs.size() > 0) {
                    return objs;
                }
            }
            if (child == null) {
                break;
            }
            if (i == names.size() - 1) {
                return List.of(child);
            }
            if (child instanceof DBSObjectContainer oc) {
                parent = oc;
            } else {
                break;
            }
        }
        return Collections.emptyList();
    }

    @NotNull
    private static List<? extends DBSProcedure> findProcedures(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSProcedureContainer procsContainer,
        @NotNull String procedureName
    ) throws DBException {
        try {
            DBSProcedure child = procsContainer.getProcedure(monitor, procedureName);
            if (child != null) {
                return List.of(child);
            } else {
                Collection<? extends DBSProcedure> procs = procsContainer.getProcedures(monitor);
                if (procs != null) {
                    List<? extends DBSProcedure> matchedProcs = procs.stream().filter(p -> p.getName().equals(procedureName)).toList();
                    if (!matchedProcs.isEmpty()) {
                        return matchedProcs;
                    } else {
                        return Collections.emptyList();
                    }
                } else {
                    return Collections.emptyList();
                }
            }
        } catch (DBException e) {
            log.debug("Error loading procedures for semantic analysis", e);
            return Collections.emptyList();
        }
    }
}