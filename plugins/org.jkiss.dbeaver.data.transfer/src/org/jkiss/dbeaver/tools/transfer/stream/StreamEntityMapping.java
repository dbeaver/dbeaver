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
package org.jkiss.dbeaver.tools.transfer.stream;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionSource;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.tools.transfer.stream.model.StreamDataSource;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.util.*;

public class StreamEntityMapping implements DBSEntity, DBSDataContainer, DBPQualifiedObject {
    private final File inputFile;
    private final DBPDataSource dataSource;
    private final String entityName;
    private final List<StreamDataImporterColumnInfo> streamColumns = new ArrayList<>();

    public StreamEntityMapping(File inputFile) {
        this.inputFile = inputFile;
        this.entityName = inputFile.getName();
        this.dataSource = new StreamDataSource(entityName);
    }

    StreamEntityMapping(Map<String, Object> config) throws DBCException {
        this.entityName = CommonUtils.toString(config.get("entityId"));

        String inputFileName = CommonUtils.toString(config.get("inputFile"));
        if (CommonUtils.isEmpty(inputFileName)) {
            inputFileName = this.entityName;
        }
        this.inputFile = new File(inputFileName);

        this.dataSource = new StreamDataSource(entityName);
    }

    public File getInputFile() {
        return inputFile;
    }

    public String getEntityName() {
        return entityName;
    }

    @NotNull
    @Override
    public DBSEntityType getEntityType() {
        return DBSEntityType.TABLE;
    }

    @Override
    public List<StreamDataImporterColumnInfo> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return streamColumns;
    }

    @Override
    public DBSEntityAttribute getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) throws DBException {
        return DBUtils.findObject(streamColumns, attributeName);
    }

    @Override
    public Collection<? extends DBSEntityConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Override
    public Collection<? extends DBSEntityAssociation> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Override
    public Collection<? extends DBSEntityAssociation> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public DBSObject getParentObject() {
        return dataSource;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public int getSupportedFeatures() {
        return DATA_SELECT;
    }

    @NotNull
    @Override
    public DBCStatistics readData(@NotNull DBCExecutionSource source, @NotNull DBCSession session, @NotNull DBDDataReceiver dataReceiver, DBDDataFilter dataFilter, long firstRow, long maxRows, long flags, int fetchSize) throws DBCException {
        throw new DBCException("Not implemented");
    }

    @Override
    public long countData(@NotNull DBCExecutionSource source, @NotNull DBCSession session, DBDDataFilter dataFilter, long flags) throws DBCException {
        return -1;
    }

    @NotNull
    @Override
    public String getName() {
        return entityName;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return getName();
    }

    public List<StreamDataImporterColumnInfo> getStreamColumns() {
        return streamColumns;
    }

    void setStreamColumns(List<StreamDataImporterColumnInfo> streamColumns) {
        this.streamColumns.clear();
        this.streamColumns.addAll(streamColumns);
    }

    public StreamDataImporterColumnInfo getStreamColumn(String name) {
        for (StreamDataImporterColumnInfo col : streamColumns) {
            if (name.equals(col.getName())) {
                return col;
            }
        }
        return null;
    }

    Map<String, Object>saveSettings() {
        Map<String, Object> mappings = new LinkedHashMap<>();
        mappings.put("entityId", entityName);
        return mappings;
    }

    public boolean isSameColumns(@NotNull StreamEntityMapping mapping) {
        if (streamColumns.size() != mapping.streamColumns.size()) {
            return false;
        }
        for (int index = 0; index < streamColumns.size(); index++) {
            StreamDataImporterColumnInfo oldColumn = streamColumns.get(index);
            StreamDataImporterColumnInfo newColumn = mapping.streamColumns.get(index);
            if (!oldColumn.getName().equals(newColumn.getName())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return inputFile.getAbsolutePath();
    }

    @Override
    public int hashCode() {
        return inputFile.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof StreamEntityMapping &&
            CommonUtils.equalObjects(inputFile, ((StreamEntityMapping) obj).inputFile);
    }
}
