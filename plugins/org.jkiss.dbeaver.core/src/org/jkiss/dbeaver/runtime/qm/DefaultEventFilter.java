/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.qm.QMConstants;
import org.jkiss.dbeaver.model.qm.QMEventFilter;
import org.jkiss.dbeaver.model.qm.QMMetaEvent;
import org.jkiss.dbeaver.model.qm.QMObjectType;
import org.jkiss.dbeaver.model.qm.meta.*;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Default event filter based on preference settings.
 */
public class DefaultEventFilter implements QMEventFilter {

    private boolean showSessions = false;
    private boolean showTransactions = false;
    private boolean showQueries = false;

    private java.util.List<DBCExecutionPurpose> showPurposes = new ArrayList<>();

    public DefaultEventFilter()
    {
        reloadPreferences();
    }

    public void reloadPreferences()
    {
        DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

        Collection<QMObjectType> objectTypes = QMObjectType.fromString(store.getString(QMConstants.PROP_OBJECT_TYPES));
        this.showSessions = objectTypes.contains(QMObjectType.session);
        this.showTransactions = objectTypes.contains(QMObjectType.txn);
        this.showQueries = objectTypes.contains(QMObjectType.query);

        this.showPurposes.clear();
        for (String queryType : CommonUtils.splitString(store.getString(QMConstants.PROP_QUERY_TYPES), ',')) {
            try {
                this.showPurposes.add(DBCExecutionPurpose.valueOf(queryType));
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }
    }

    @Override
    public boolean accept(QMMetaEvent event)
    {
        QMMObject object = event.getObject();
        if (object instanceof QMMStatementExecuteInfo) {
            return showQueries && showPurposes.contains(((QMMStatementExecuteInfo) object).getStatement().getPurpose());
        } else if (object instanceof QMMTransactionInfo || object instanceof QMMTransactionSavepointInfo) {
            return showTransactions;
        } else if (object instanceof QMMSessionInfo) {
            return showSessions;
        }
        return true;
    }

}
