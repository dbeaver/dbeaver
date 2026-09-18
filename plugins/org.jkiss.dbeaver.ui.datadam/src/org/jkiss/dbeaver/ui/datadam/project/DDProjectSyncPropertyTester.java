/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.datadam.project;

import org.eclipse.core.expressions.PropertyTester;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;

public class DDProjectSyncPropertyTester extends PropertyTester {
    public static final String NAMESPACE = "org.jkiss.dbeaver.ui.datadam.projectSync";
    public static final String PROP_ENABLED = "enabled";
    public static final String PROP_CAN_SHARE = "canShare";
    public static final String PROP_CAN_SYNC = "canSync";

    private static final Log log = Log.getLog(DDProjectSyncPropertyTester.class);

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof DBNProject projectNode)) {
            return false;
        }
        DDProjectSyncUIManager manager = DDProjectSyncUIManager.getInstance();
        if (!manager.isDDEnabled()) {
            return false;
        }
        if (PROP_ENABLED.equals(property)) {
            return true;
        }
        try {
            boolean shared = manager.getService().isShared(projectNode.getProject());
            return switch (property) {
                case PROP_CAN_SHARE -> !shared;
                case PROP_CAN_SYNC -> shared;
                default -> false;
            };
        } catch (DBException e) {
            log.debug("Error checking DataDam project state", e);
            return false;
        }
    }

    public static void firePropertyChange() {
        UIUtils.asyncExec(() -> {
            ActionUtils.evaluatePropertyState(NAMESPACE + "." + PROP_ENABLED);
            ActionUtils.evaluatePropertyState(NAMESPACE + "." + PROP_CAN_SHARE);
            ActionUtils.evaluatePropertyState(NAMESPACE + "." + PROP_CAN_SYNC);
        });
    }
}
