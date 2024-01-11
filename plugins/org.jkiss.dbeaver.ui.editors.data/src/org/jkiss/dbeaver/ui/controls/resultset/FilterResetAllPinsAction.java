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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.action.Action;
import org.jkiss.dbeaver.model.data.DBDAttributeConstraint;
import org.jkiss.dbeaver.model.data.DBDAttributeConstraintBase;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;

import java.util.List;

class FilterResetAllPinsAction extends Action {
    private final ResultSetViewer resultSetViewer;

    FilterResetAllPinsAction(ResultSetViewer resultSetViewer) {
        super(ResultSetMessages.controls_resultset_viewer_action_reset_all_pins);
        this.resultSetViewer = resultSetViewer;
    }

    @Override
    public void run() {
        execute(true);
    }

    void execute(boolean refresh) {
        List<DBDAttributeConstraint> constraints = resultSetViewer.getDataFilter().getConstraints();
        if (constraints == null) {
            return;
        }
        for (DBDAttributeConstraint c : constraints) {
            c.removeOption(DBDAttributeConstraintBase.ATTR_OPTION_PINNED);
        }
        if (refresh) {
            resultSetViewer.redrawData(true, true);
        }
    }
}
