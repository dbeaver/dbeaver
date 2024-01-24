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

import org.jkiss.dbeaver.model.qm.QMEventFilter;

import java.util.Set;

public class QMAdminCursorFilter {
    private final QMAdminEventCriteria criteria;
    private final QMEventFilter filter;
    private final String sessionId;

    public QMAdminCursorFilter(String sessionId, QMAdminEventCriteria criteria, QMEventFilter filter) {
        this.sessionId = sessionId;
        this.criteria = criteria;
        this.filter = filter;
    }

    public QMAdminCursorFilter(QMCursorFilter cursorFilter, Set<String> userNames) {
        this.sessionId = cursorFilter.getSessionId();
        this.criteria = new QMAdminEventCriteria(cursorFilter.getCriteria(), userNames);
        this.filter = cursorFilter.getFilter();
    }

    public String getSessionId() {
        return sessionId;
    }

    public QMAdminEventCriteria getAdminCriteria() {
        return criteria;
    }

    public QMEventFilter getFilter() {
        return filter;
    }
}
