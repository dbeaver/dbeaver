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

package org.jkiss.dbeaver.runtime.qm;

import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.qm.*;
import org.jkiss.dbeaver.model.qm.meta.*;

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
        eventCriteria = QMUtils.createDefaultCriteria(ModelPreferences.getPreferences());
    }

    @Override
    public boolean accept(QMMetaEvent event)
    {
        QMMObject object = event.getObject();
        if (object instanceof QMMStatementExecuteInfo) {
            return eventCriteria.hasObjectType(QMObjectType.query) &&
                eventCriteria.hasQueryType(((QMMStatementExecuteInfo) object).getStatement().getPurpose());
        } else if (object instanceof QMMTransactionInfo || object instanceof QMMTransactionSavepointInfo) {
            return eventCriteria.hasObjectType(QMObjectType.txn);
        } else if (object instanceof QMMSessionInfo) {
            return eventCriteria.hasObjectType(QMObjectType.session);
        }
        return true;
    }

}
