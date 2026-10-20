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

package org.jkiss.dbeaver.runtime.qm;

import org.jkiss.dbeaver.model.qm.QMEvent;
import org.jkiss.dbeaver.model.qm.QMEventFilter;
import org.jkiss.dbeaver.model.qm.QMObjectType;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.qm.filters.QMEventCriteria;
import org.jkiss.dbeaver.model.qm.meta.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;

/**
 * Default event filter based on preference settings.
 */
public class DefaultEventFilter implements QMEventFilter {

    private QMEventCriteria eventCriteria = new QMEventCriteria();

    public DefaultEventFilter()
    {
        reloadPreferences();
    }

    public void reloadPreferences()
    {
        eventCriteria = QMUtils.createDefaultCriteria(DBWorkbench.getPlatform().getPreferenceStore());
    }

    @Override
    public boolean accept(QMEvent event) {
        QMMObject object = event.getObject();
        if (object instanceof QMMStatementExecuteInfo) {
            return eventCriteria.hasObjectType(QMObjectType.query) &&
                eventCriteria.hasQueryType(((QMMStatementExecuteInfo) object).getStatement().getPurpose());
        } else if (object instanceof QMMTransactionInfo || object instanceof QMMTransactionSavepointInfo) {
            return eventCriteria.hasObjectType(QMObjectType.txn);
        } else if (object instanceof QMMConnectionInfo) {
            return eventCriteria.hasObjectType(QMObjectType.session);
        }
        return true;
    }

}
