/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;

/**
 * QM event criteria
 */
public class QMEventCriteria {

    @Nullable
    String containerId;
    @Nullable
    String sessionId;
    @Nullable
    QMObjectType[] objectTypes;
    @Nullable
    DBCExecutionPurpose[] queryTypes;
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

    public DBCExecutionPurpose[] getQueryTypes() {
        return queryTypes;
    }

    public void setQueryTypes(DBCExecutionPurpose[] queryTypes) {
        this.queryTypes = queryTypes;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }
}
