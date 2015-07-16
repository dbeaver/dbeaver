/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.data.managers;

import org.eclipse.jface.action.IContributionManager;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPPropertyManager;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueManager;

/**
 * Base value manager
 */
public abstract class BaseValueManager implements IValueManager {

    static final Log log = Log.getLog(BaseValueManager.class);

    @Nullable
    public static Object makeNullValue(@NotNull final IValueController valueController)
    {
        try {
            DBCExecutionContext executionContext = valueController.getExecutionContext();
            if (executionContext == null) {
                throw new DBCException(CoreMessages.editors_sql_status_not_connected_to_database);
            }
            // We are going to create NULL value - it shouldn't result in any DB roundtrips so let's use dummy monitor
            DBCSession session = executionContext.openSession(VoidProgressMonitor.INSTANCE, DBCExecutionPurpose.UTIL, "Set NULL value");
            try {
                return DBUtils.makeNullValue(
                    session,
                    valueController.getValueHandler(),
                    valueController.getValueType());
            } finally {
                session.close();
            }
        } catch (DBCException e) {
            log.error("Can't make NULL value", e);
            return null;
        }
    }

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull IValueController controller) throws DBCException {
        // nothing
    }

    @Override
    public void contributeProperties(@NotNull DBPPropertyManager propertySource, @NotNull IValueController controller) {
        // nothing
    }

/*
    @Override
    public IValueController.EditType[] getSupportedEditTypes() {
        // Base value manager do not support any edit type.
        return new IValueController.EditType[] {};
    }
*/


}