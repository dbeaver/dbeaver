/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.data.managers;

import org.eclipse.jface.action.IContributionManager;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.preferences.DBPPropertyManager;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.IValueManager;

/**
 * Base value manager
 */
public abstract class BaseValueManager implements IValueManager {

    private static final Log log = Log.getLog(BaseValueManager.class);

    @Nullable
    public static Object makeNullValue(@NotNull final IValueController valueController)
    {
        try {
            DBCExecutionContext executionContext = valueController.getExecutionContext();
            if (executionContext == null) {
                throw new DBCException(ModelMessages.error_not_connected_to_database);
            }
            // We are going to create NULL value - it shouldn't result in any DB roundtrips so let's use dummy monitor
            try (DBCSession session = executionContext.openSession(new VoidProgressMonitor(), DBCExecutionPurpose.UTIL, "Set NULL value")) {
                return DBUtils.makeNullValue(
                    session,
                    valueController.getValueHandler(),
                    valueController.getValueType());
            }
        } catch (DBCException e) {
            log.error("Can't make NULL value", e);
            return null;
        }
    }

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull IValueController controller, @Nullable IValueEditor activeEditor) throws DBCException {
        if (activeEditor != null) {
            activeEditor.contributeActions(manager, controller);
        }
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