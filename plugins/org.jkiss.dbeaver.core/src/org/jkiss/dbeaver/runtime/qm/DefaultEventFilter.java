/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.runtime.qm;

import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
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
