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

public class QMCursorFilter {
    private final QMEventCriteria criteria;
    private final QMEventFilter filter;
    private final String sessionId;

    public QMCursorFilter(String sessionId, QMEventCriteria criteria, QMEventFilter filter) {
        this.sessionId = sessionId;
        this.criteria = criteria;
        this.filter = filter;
    }

    public String getSessionId() {
        return sessionId;
    }

    public QMEventCriteria getCriteria() {
        return criteria;
    }

    public QMEventFilter getFilter() {
        return filter;
    }
}
