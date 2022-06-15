/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.qm.meta.*;
import org.jkiss.utils.CommonUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * QM meta event
 */
public class QMMetaEventEntity implements QMEvent {
    private final long id;
    private final QMSessionInfo sessionInfo;
    private final QMMObject object;
    private final QMEventAction action;

    public QMMetaEventEntity(QMMObject object, QMEventAction action, long id, String sessionId, @Nullable QMSessionInfo sessionInfo) {
        this.id = id;
        this.sessionInfo = sessionInfo;
        this.object = object;
        this.action = action;
    }

    public long getId() {
        return id;
    }

    @Nullable
    public QMSessionInfo getSessionInfo() {
        return sessionInfo;
    }

    public QMMObject getObject() {
        return object;
    }

    public QMEventAction getAction() {
        return action;
    }

    public static Map<String, Object> toMap(QMMetaEventEntity event) throws DBException {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("objectClassName", event.getObject().getClass().getName());
        result.put("object", event.getObject().toMap());
        result.put("action", event.getAction().getId());
        result.put("id", event.getId());
        if (event.getSessionInfo() != null) {
            result.put("sessionName", event.getSessionInfo().getUserName());
            result.put("sessionId", event.getSessionInfo().getUserDomain());
        }
        return result;
    }

    public static QMMetaEventEntity fromMap(Map<String, Object> map) {
        String className = CommonUtils.toString(map.get("objectClassName"));
        Map<String, Object> object = (Map<String, Object>) map.get("object");
        QMMObject eventObject;
        if (className.equals(QMMConnectionInfo.class.getName())) {
            eventObject = QMMConnectionInfo.fromMap(object);
        } else if (className.equals(QMMStatementExecuteInfo.class.getName())) {
            eventObject = QMMStatementExecuteInfo.fromMap(object);
        } else if (className.equals(QMMStatementInfo.class.getName())) {
            eventObject = QMMStatementInfo.fromMap(object);
        } else if (className.equals(QMMTransactionInfo.class.getName())) {
            eventObject = QMMTransactionInfo.fromMap(object);
        } else {
            eventObject = null;
        }
        QMEventAction action = QMEventAction.getById(CommonUtils.toInt(map.get("action")));
        long id = CommonUtils.toLong(map.get("id"));
        QMSessionInfo sessionInfo = null;
        String sessionUserName = CommonUtils.toString(map.get("sessionName"));
        String sessionUserDomain = CommonUtils.toString(map.get("sessionId"));
        if (!sessionUserName.isEmpty()) {
            sessionInfo = new QMSessionInfo(sessionUserName, sessionUserDomain);
        }
        return new QMMetaEventEntity(eventObject, action, id, "", sessionInfo);
    }
}
