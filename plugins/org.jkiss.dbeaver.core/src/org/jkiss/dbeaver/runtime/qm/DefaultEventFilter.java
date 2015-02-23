/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.runtime.qm;

import org.eclipse.jface.preference.IPreferenceStore;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.runtime.qm.meta.*;
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

    private java.util.List<DBCExecutionPurpose> showPurposes = new ArrayList<DBCExecutionPurpose>();

    public DefaultEventFilter()
    {
        reloadPreferences();
    }

    public void reloadPreferences()
    {
        IPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

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
