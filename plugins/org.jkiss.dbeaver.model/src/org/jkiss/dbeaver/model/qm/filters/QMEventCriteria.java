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

package org.jkiss.dbeaver.model.qm.filters;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.qm.QMObjectType;
import org.jkiss.utils.ArrayUtils;

import java.util.Collections;
import java.util.Set;

/**
 * QM event criteria
 */
public class QMEventCriteria {
    @Nullable
    private String containerId;
    @Nullable
    private Long sessionId;
    @NotNull
    private QMObjectType[] objectTypes = new QMObjectType[0];
    @NotNull
    private DBCExecutionPurpose[] queryTypes = new DBCExecutionPurpose[0];
    private boolean skipEmptyQueries = false;
    @Nullable
    private String searchString;
    @Nullable
    private Long lastEventId;
    @NotNull
    private Set<String> driverIds = Collections.emptySet();
    @NotNull
    private Set<QMEventStatus> eventStatuses = Collections.emptySet();
    @NotNull
    private Set<String> projectIds = Collections.emptySet();
    @NotNull
    private Set<String> dataSourceIds = Collections.emptySet();
    @NotNull
    private Set<String> schemas = Collections.emptySet();
    @NotNull
    private Set<String> catalogs = Collections.emptySet();
    @NotNull
    private QMSortField sortField = QMSortField.DATE;
    @Nullable
    private QMDateRange startDateRange;
    private boolean desc = true;
    private int fetchingSize = 200;

    @Nullable
    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public boolean hasSessionId() {
        return !(sessionId == null || sessionId == 0);
    }

    @Nullable
    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public QMObjectType[] getObjectTypes() {
        return objectTypes;
    }

    public void setObjectTypes(QMObjectType[] objectTypes) {
        this.objectTypes = objectTypes;
    }

    public boolean hasObjectTypes() {
        return objectTypes.length > 0 && objectTypes.length != QMObjectType.values().length;
    }

    public boolean hasObjectType(QMObjectType type) {
        // If all object types are here it is the same as no object type
        return objectTypes.length > 0 && ArrayUtils.contains(objectTypes, type);
    }

    public DBCExecutionPurpose[] getQueryTypes() {
        return queryTypes;
    }

    public void setQueryTypes(DBCExecutionPurpose[] queryTypes) {
        this.queryTypes = queryTypes;
    }

    public boolean hasQueryTypes() {
        // If all query types are here it is the same as no query type
        return queryTypes.length > 0 && queryTypes.length != DBCExecutionPurpose.values().length;
    }

    public boolean hasQueryType(DBCExecutionPurpose type) {
        return queryTypes.length > 0 && ArrayUtils.contains(queryTypes, type);
    }

    @Nullable
    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public int getFetchingSize() {
        return fetchingSize;
    }

    public void setFetchingSize(int fetchingSize) {
        this.fetchingSize = fetchingSize;
    }

    public boolean isSkipEmptyQueries() {
        return skipEmptyQueries;
    }

    public void setSkipEmptyQueries(boolean skipEmptyQueries) {
        this.skipEmptyQueries = skipEmptyQueries;
    }


    @NotNull
    public Set<String> getDriverIds() {
        return driverIds;
    }

    public void setDriverIds(@NotNull Set<String> driverIds) {
        this.driverIds = driverIds;
    }

    public boolean hasDriverIds() {
        return !driverIds.isEmpty();
    }

    @NotNull
    public Set<QMEventStatus> getEventStatuses() {
        return eventStatuses;
    }

    public void setEventStatuses(@NotNull Set<QMEventStatus> eventStatuses) {
        this.eventStatuses = eventStatuses;
    }

    public boolean hasEventStatuses() {
        return !eventStatuses.isEmpty() && eventStatuses.size() < QMEventStatus.values().length;
    }

    @NotNull
    public QMSortField getSortField() {
        return sortField;
    }

    public void setSortField(@NotNull QMSortField sortField) {
        this.sortField = sortField;
    }

    public boolean isDesc() {
        return desc;
    }

    public void setDesc(boolean desc) {
        this.desc = desc;
    }

    @Nullable
    public QMDateRange getDateRange() {
        return startDateRange;
    }

    public void setDateRange(@Nullable QMDateRange startDateRange) {
        this.startDateRange = startDateRange;
    }

    @Nullable
    public Long getLastEventId() {
        return lastEventId;
    }

    public void setLastEventId(@Nullable Long lastEventId) {
        this.lastEventId = lastEventId;
    }

    public boolean hasLastEventId() {
        return lastEventId != null;
    }

    @NotNull
    public Set<String> getProjectIds() {
        return projectIds;
    }

    public void setProjectIds(@NotNull Set<String> projectIds) {
        this.projectIds = projectIds;
    }

    public boolean hasProjectIds() {
        return !projectIds.isEmpty();
    }

    @NotNull
    public Set<String> getDataSourceIds() {
        return dataSourceIds;
    }

    public void setDataSourceIds(@NotNull Set<String> dataSourceIds) {
        this.dataSourceIds = dataSourceIds;
    }

    @NotNull
    public Set<String> getCatalogs() {
        return catalogs;
    }

    public void setCatalogs(@NotNull Set<String> catalogs) {
        this.catalogs = catalogs;
    }

    @NotNull
    public Set<String> getSchemas() {
        return schemas;
    }

    public void setSchemas(@NotNull Set<String> schemas) {
        this.schemas = schemas;
    }
}
