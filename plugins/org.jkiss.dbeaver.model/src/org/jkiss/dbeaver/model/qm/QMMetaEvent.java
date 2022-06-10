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

import org.jkiss.dbeaver.model.auth.SMSessionPersistent;
import org.jkiss.dbeaver.model.qm.meta.*;
import org.jkiss.utils.CommonUtils;

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

    public static Map<String, Object> toMap(QMMetaEvent event) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("objectClassName", event.getObject().getClass().getName());
        result.put("object", event.getObject().toMap());
        result.put("action", event.getAction().getId());
        result.put("sessionId", event.getSessionId());
        return result;
    }

    public static QMMetaEvent fromMap(Map<String, Object> map) {
        String className = (String) map.get("objectClassName");
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
        String sessionId = CommonUtils.toString(map.get("sessionId"));
        return new QMMetaEvent(eventObject, action, sessionId);
    }

}
