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

import org.jkiss.dbeaver.model.auth.DBASessionPersistent;
import org.jkiss.dbeaver.model.qm.meta.QMMObject;

/**
 * QM meta event
 */
public class QMMetaEvent implements QMEvent {
    protected final QMMObject object;
    protected final QMEventAction action;
    protected final DBASessionPersistent qmAppSessionPersistent;

    public QMMetaEvent(QMMObject object, QMEventAction action, DBASessionPersistent qmAppSessionPersistent) {
        this.object = object;
        this.action = action;
        this.qmAppSessionPersistent = qmAppSessionPersistent;
    }

    public QMMObject getObject() {
        return object;
    }

    public QMEventAction getAction() {
        return action;
    }

    public DBASessionPersistent getQmAppSessionPersistent() {
        return qmAppSessionPersistent;
    }

    @Override
    public String toString() {
        return action + " " + object;
    }
}
