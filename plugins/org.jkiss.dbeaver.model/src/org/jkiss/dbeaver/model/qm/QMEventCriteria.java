/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.model.qm;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.utils.ArrayUtils;

/**
 * QM event criteria
 */
public class QMEventCriteria {

    @Nullable
    String containerId;
    @Nullable
    String sessionId;
    @NotNull
    QMObjectType[] objectTypes = new QMObjectType[0];
    @NotNull
    DBCExecutionPurpose[] queryTypes = new DBCExecutionPurpose[0];
    @Nullable
    String searchString;

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
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

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }
}
