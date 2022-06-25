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

import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.qm.meta.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * QM meta event
 */
public class QMMetaEvent implements QMEvent {
    protected final QMMObject object;
    protected final QMEventAction action;
    protected final String sessionId;

    public QMMetaEvent(QMMObject object, QMEventAction action, String sessionId) {
        this.object = object;
        this.action = action;
        this.sessionId = sessionId;
    }

    public QMMObject getObject() {
        return object;
    }

    public QMEventAction getAction() {
        return action;
    }

    public String getSessionId() {
        return sessionId;
    }

    @Override
    public String toString() {
        return action + " " + object;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", object.getObjectType().getId());
        result.put("object", object.toMap());
        result.put("action", action.getId());
        result.put("sessionId", sessionId);
        return result;
    }

    public static QMMetaEvent fromMap(Map<String, Object> map) {
        QMMObject.ObjectType objectType = QMMObject.ObjectType.getById(JSONUtils.getString(map, "type"));
        if (objectType == null) {
            return null;
        }
        Map<String, Object> object = JSONUtils.getObject(map, "object");
        QMMObject eventObject;
        switch (objectType) {
            case ConnectionInfo:
                eventObject = QMMConnectionInfo.fromMap(object);
                break;
            case StatementExecuteInfo:
                eventObject = QMMStatementExecuteInfo.fromMap(object);
                break;
            case StatementInfo:
                eventObject = QMMStatementInfo.fromMap(object);
                break;
            case TransactionInfo:
                eventObject = QMMTransactionInfo.fromMap(object);
                break;
            default:
                return null;
        }
        QMEventAction action = QMEventAction.getById(JSONUtils.getInteger(map, "action"));
        String sessionId = JSONUtils.getString(map, "sessionId");
        return new QMMetaEvent(eventObject, action, sessionId);
    }

}
