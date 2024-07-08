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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.GenericMessages;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * GenericEntityContainer
 */
public abstract class GenericObjectContainer implements GenericStructContainer, DBPRefreshableObject {
    private static final Log log = Log.getLog(GenericObjectContainer.class);

    @NotNull
    private final GenericDataSource dataSource;
    private final TableCache tableCache;
    private final IndexCache indexCache;
    private final ForeignKeysCache foreignKeysCache;
    private final ConstraintKeysCache constraintKeysCache;
    private final ContainerTriggerCache containerTriggerCache;
    private final TableTriggerCache tableTriggerCache;
    private final GenericSequenceCache sequenceCache;
    private final GenericSynonymCache synonymCache;
    private List<GenericPackage> packages;
    protected List<GenericProcedure> procedures;
    protected List<? extends GenericSequence> sequences;
    protected List<? extends GenericSynonym> synonyms;

    protected GenericObjectContainer(@NotNull GenericDataSource dataSource) {
        this.dataSource = dataSource;
        this.tableCache = createTableCache(dataSource);
        this.indexCache = new IndexCache(tableCache);
        this.constraintKeysCache = new ConstraintKeysCache(tableCache);
        this.foreignKeysCache = new ForeignKeysCache(tableCache);
        this.containerTriggerCache = new ContainerTriggerCache();
        this.tableTriggerCache = new TableTriggerCache(tableCache);
        this.sequenceCache = new GenericSequenceCache();
        this.synonymCache = new GenericSynonymCache();
    }

    public TableCache createTableCache(GenericDataSource datasource) {
        return new TableCache(datasource);
    }

    @Override
    public final TableCache getTableCache() {
        return tableCache;
    }

    @Override
    public final IndexCache getIndexCache() {
        return indexCache;
    }

    @Override
    public final ConstraintKeysCache getConstraintKeysCache() {
        return constraintKeysCache;
    }

    @Override
    public final ForeignKeysCache getForeignKeysCache() {
        return foreignKeysCache;
    }

    @Override
    public TableTriggerCache getTableTriggerCache() {
        return tableTriggerCache;
    }

    @Override
    public GenericSequenceCache getSequenceCache() {
        return sequenceCache;
    }

    public GenericSynonymCache getSynonymCache() {
        return synonymCache;
    }

    @NotNull
    @Override
    public GenericDataSource getDataSource() {
        return dataSource;
    }


    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public List<? extends GenericView> getViews(DBRProgressMonitor monitor) throws DBException {
        List<? extends GenericTableBase> tables = getTables(monitor);
        if (tables != null) {
            List<GenericView> filtered = new ArrayList<>();
            for (GenericTableBase table : tables) {
                if (table instanceof GenericView) {
                    filtered.add((GenericView) table);
                }
            }
            return filtered;
        }
        return null;
    }

    @Override
    public List<? extends GenericTable> getPhysicalTables(DBRProgressMonitor monitor) throws DBException {
        List<? extends GenericTableBase> tables = getTables(monitor);
        if (tables != null) {
            List<GenericTable> filtered = new ArrayList<>();
            for (GenericTableBase table : tables) {
                if (table.isPhysicalTable()) {
                    filtered.add((GenericTable) table);
                }
            }
            return filtered;
        }
        return null;
    }

    @Override
    public List<? extends GenericTableBase> getTables(@NotNull DBRProgressMonitor monitor) throws DBException {
        return tableCache.getAllObjects(monitor, this);
    }

    @Override
    public GenericTableBase getTable(DBRProgressMonitor monitor, String name) throws DBException {
        return tableCache.getObject(monitor, this, name);
    }

    @Override
    public Collection<GenericTableIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException {
        cacheIndexes(monitor, true);
        return indexCache.getObjects(monitor, this, null);
    }

    private void cacheIndexes(DBRProgressMonitor monitor, boolean readFromTables)
        throws DBException {
        // Cache indexes (read all tables, all columns and all indexes in this container)
        // This doesn't work for generic datasource because metadata facilities
        // allows index query only by certain table name
        //cacheIndexes(monitor, null);
        synchronized (indexCache) {
            if (!indexCache.isFullyCached()) {
                List<GenericTableIndex> oldCache = indexCache.getCachedObjects();
                indexCache.clearCache();

                // First - try to read all indexes. Some drivers can do this
                // If index list is empty then try to read by tables
                List<GenericTableIndex> newIndexCache;
                try {
                    newIndexCache = indexCache.getObjects(monitor, this, null);
                } catch (DBException e) {
                    log.debug("Error reading catalog/schema indexes. Get indexes from tables", e);
                    newIndexCache = new ArrayList<>();
                }

                if (readFromTables && newIndexCache.isEmpty()) {
                    newIndexCache = new ArrayList<>();
                    indexCache.clearCache();
                    // Load indexes for all tables and return copy of them
                    List<? extends GenericTableBase> tables = getTables(monitor);
                    monitor.beginTask("Cache indexes from tables", tables.size());
                    try {
                        for (GenericTableBase table : tables) {
                            if (monitor.isCanceled()) {
                                return;
                            }
                            monitor.subTask("Read indexes for '" + table.getFullyQualifiedName(DBPEvaluationContext.DDL) + "'");
                            Collection<? extends GenericTableIndex> tableIndexes = table.getIndexes(monitor);
                            if (!CommonUtils.isEmpty(tableIndexes)) {
                                newIndexCache.addAll(tableIndexes);
                            }
                            monitor.worked(1);
                        }
                    } finally {
                        monitor.done();
                    }
                }

                for (GenericTableIndex oldIndex : oldCache) {
                    if (!oldIndex.isPersisted()) {
                        newIndexCache.add(oldIndex);
                    } else {
                        // Check for the dups
                        for (int i = 0; i < newIndexCache.size(); i++) {
                            GenericTableIndex newIndex = newIndexCache.get(i);
                            if (oldIndex.getContainer() == newIndex.getContainer() &&
                                CommonUtils.equalObjects(oldIndex.getName(), newIndex.getName())) {
                                newIndexCache.set(i, oldIndex);
                            }
                        }
                    }
                }
                indexCache.setCache(newIndexCache);
            }
        }
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope)
        throws DBException {
        // Cache tables
        if ((scope & STRUCT_ENTITIES) != 0) {
            monitor.subTask("Cache tables");
            tableCache.getAllObjects(monitor, this);
        }

        // Cache attributes
        if ((scope & STRUCT_ATTRIBUTES) != 0 && dataSource.supportsStructCache()) {
            // Try to cache columns
            // Cannot be sure that all jdbc drivers support reading of all catalog columns
            // So error here is not fatal
            try {
                monitor.subTask("Cache tables' columns");
                tableCache.loadChildren(monitor, this, null);
            } catch (Exception e) {
                log.debug(e);
            }
        }
        // Cache associations
        if ((scope & STRUCT_ASSOCIATIONS) != 0 && dataSource.supportsStructCache()) {
            // Try to read all PKs
            // Try to read all FKs
            try {
                monitor.subTask("Cache primary keys");
                Collection<GenericUniqueKey> objects = constraintKeysCache.getObjects(monitor, this, null);
                if (CommonUtils.isEmpty(objects)) {
                    // Nothing was read, Maybe driver doesn't support mass keys reading
                    constraintKeysCache.clearCache();
                }
            } catch (Exception e) {
                // Failed - seems to be unsupported feature
                log.debug(e);
            }

            if (dataSource.getInfo().supportsIndexes()) {
                // Try to read all indexes
                monitor.subTask("Cache indexes");
                cacheIndexes(monitor, false);
            }

            if (dataSource.getInfo().supportsReferentialIntegrity()) {
                // Try to read all FKs
                try {
                    monitor.subTask("Cache foreign keys");
                    Collection<GenericTableForeignKey> foreignKeys = foreignKeysCache.getObjects(monitor, this, null);
                    if (CommonUtils.isEmpty(foreignKeys)) {
                        // Nothing was read, Maybe driver doesn't support mass keys reading
                        foreignKeysCache.clearCache();
                    }
                } catch (Exception e) {
                    // Failed - seems to be unsupported feature
                    log.debug(e);
                }
            }
        }
    }

    @Override
    public Collection<GenericPackage> getPackages(DBRProgressMonitor monitor)
        throws DBException {
        if (procedures == null) {
            loadProcedures(monitor);
        }
        return packages;
    }

    public GenericPackage getPackage(DBRProgressMonitor monitor, String name)
        throws DBException {
        return DBUtils.findObject(getPackages(monitor), name);
    }

    public List<GenericProcedure> getProcedureCache() {
        return procedures;
    }

    @Override
    public List<GenericProcedure> getProcedures(DBRProgressMonitor monitor)
        throws DBException {
        if (procedures == null) {
            loadProcedures(monitor);
        }
        return procedures;
    }

    @Override
    public GenericProcedure getProcedure(DBRProgressMonitor monitor, String uniqueName) throws DBException {
        for (GenericProcedure procedure : CommonUtils.safeCollection(getProcedures(monitor))) {
            if (uniqueName.equals(procedure.getUniqueName())) {
                return procedure;
            }
        }
        return null;
    }

    @Override
    public List<GenericProcedure> getProcedures(DBRProgressMonitor monitor, String name)
        throws DBException {
        return DBUtils.findObjects(getProcedures(monitor), name);
    }

    @Override
    public List<? extends GenericProcedure> getProceduresOnly(DBRProgressMonitor monitor) throws DBException {
        if (!dataSource.splitProceduresAndFunctions()) {
            return getProcedures(monitor);
        }
        List<GenericProcedure> filteredProcedures = new ArrayList<>();
        for (GenericProcedure proc : CommonUtils.safeList(getProcedures(monitor))) {
            if (proc.getProcedureType() == DBSProcedureType.PROCEDURE) {
                filteredProcedures.add(proc);
            }
        }
        return filteredProcedures;
    }

    @Override
    public Collection<? extends GenericProcedure> getFunctionsOnly(DBRProgressMonitor monitor) throws DBException {
        List<GenericProcedure> filteredProcedures = new ArrayList<>();
        for (GenericProcedure proc : CommonUtils.safeList(getProcedures(monitor))) {
            if (proc.getProcedureType() == DBSProcedureType.FUNCTION) {
                filteredProcedures.add(proc);
            }
        }
        return filteredProcedures;
    }

    @Override
    public Collection<? extends GenericSequence> getSequences(DBRProgressMonitor monitor) throws DBException {
        return monitor == null ? sequenceCache.getCachedObjects() : sequenceCache.getAllObjects(monitor, this);
    }

    public GenericSequence getSequence(DBRProgressMonitor monitor, String name) throws DBException {
        return sequenceCache.getObject(monitor, this, name);
    }

    @Override
    public Collection<? extends GenericSynonym> getSynonyms(@NotNull DBRProgressMonitor monitor) throws DBException {
        return synonymCache.getAllObjects(monitor, this);
    }

    public GenericSynonym getSynonym(DBRProgressMonitor monitor, String name) throws DBException {
        return synonymCache.getObject(monitor, this, name);
    }

    @Override
    public Collection<? extends GenericTrigger> getTriggers(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getDataSource().getMetaModel().supportsDatabaseTriggers(getDataSource()) ? containerTriggerCache.getAllObjects(monitor, this) : Collections.emptyList();
    }

    @Override
    public Collection<? extends GenericTrigger> getTableTriggers(DBRProgressMonitor monitor) throws DBException {
        return getDataSource().getMetaModel().supportsTriggers(getDataSource()) ? tableTriggerCache.getAllObjects(monitor, this) : Collections.emptyList();
    }

    @Association
    @Nullable
    public GenericTrigger getTableTrigger(@NotNull DBRProgressMonitor monitor, String triggerName) throws DBException {
        return getDataSource().getMetaModel().supportsTriggers(getDataSource()) ?
            tableTriggerCache.getObject(monitor, this, triggerName) : null;
    }

    @Association
    public Collection<? extends DBSDataType> getDataTypes(DBRProgressMonitor monitor) throws DBException {
        return getDataSource().getDataTypes(monitor);
    }

    @Override
    public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        List<DBSObject> childrenList = new ArrayList<>(getTables(monitor));
        if (dataSource.getMetaModel().supportsSynonyms(dataSource)) {
            childrenList.addAll(getSynonyms(monitor));
        }
        return childrenList;
    }

    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName)
        throws DBException {
        return getTable(monitor, childName);
    }

    @Override
    public synchronized DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        final boolean needsStructureCaching = !getTableCache().isEmpty();

        this.tableCache.clearCache();
        this.indexCache.clearCache();
        this.constraintKeysCache.clearCache();
        this.foreignKeysCache.clearCache();
        this.containerTriggerCache.clearCache();
        this.tableTriggerCache.clearCache();
        this.sequenceCache.clearCache();
        this.synonymCache.clearCache();
        this.packages = null;
        this.procedures = null;
        this.sequences = null;
        this.synonyms = null;

        if (needsStructureCaching) {
            cacheStructure(monitor, STRUCT_ALL);
        }

        return this;
    }

    public String toString() {
        return getName() == null ? GenericMessages.generic_object_container_none : getName();
    }

    private synchronized void loadProcedures(DBRProgressMonitor monitor)
        throws DBException {
        dataSource.getMetaModel().loadProcedures(monitor, this);

        // Order procedures
        if (procedures != null) {
            DBUtils.orderObjects(procedures);
        }
        if (packages != null) {
            for (GenericPackage pack : packages) {
                pack.orderProcedures();
            }
        }
    }

    public void addProcedure(GenericProcedure procedure) {
        if (procedures == null) {
            procedures = new ArrayList<>();
        }
        procedures.add(procedure);
    }

    public boolean hasProcedure(String name) {
        if (procedures != null) {
            for (GenericProcedure proc : procedures) {
                if (proc.getName().equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void addPackage(GenericPackage procedurePackage) {
        if (packages == null) {
            packages = new ArrayList<>();
        }
        packages.add(procedurePackage);
    }

    public class ContainerTriggerCache extends JDBCObjectCache<GenericStructContainer, GenericTrigger> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer container) throws SQLException {
            return container.getDataSource().getMetaModel().prepareContainerTriggersLoadStatement(session, container);
        }

        @Nullable
        @Override
        protected GenericTrigger fetchObject(@NotNull JDBCSession session, @NotNull GenericStructContainer container, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return container.getDataSource().getMetaModel().createContainerTriggerImpl(container, resultSet);
        }
    }

    class GenericSequenceCache extends JDBCObjectCache<GenericObjectContainer, GenericSequence> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull GenericObjectContainer container) throws SQLException {
            return container.getDataSource().getMetaModel().prepareSequencesLoadStatement(session, container);
        }

        @Nullable
        @Override
        protected GenericSequence fetchObject(@NotNull JDBCSession session, @NotNull GenericObjectContainer container, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return container.getDataSource().getMetaModel().createSequenceImpl(session, container, resultSet);
        }

        @Override
        protected boolean handleCacheReadError(Exception error) {
            return getDataSource().getMetaModel().handleSequenceCacheReadingError(error);
        }
    }

    class GenericSynonymCache extends JDBCObjectCache<GenericObjectContainer, GenericSynonym> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull GenericObjectContainer container) throws SQLException {
            return container.getDataSource().getMetaModel().prepareSynonymsLoadStatement(session, container);
        }

        @Nullable
        @Override
        protected GenericSynonym fetchObject(@NotNull JDBCSession session, @NotNull GenericObjectContainer container, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return container.getDataSource().getMetaModel().createSynonymImpl(session, container, resultSet);
        }
    }

}
