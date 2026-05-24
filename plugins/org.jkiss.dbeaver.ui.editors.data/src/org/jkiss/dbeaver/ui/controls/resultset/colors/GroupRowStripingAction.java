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
package org.jkiss.dbeaver.ui.controls.resultset.colors;

import org.eclipse.jface.action.Action;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;

/**
 * Opens group row striping settings.
 */
public class GroupRowStripingAction extends Action {

    @NotNull
    private final ResultSetViewer resultSetViewer;

    public GroupRowStripingAction(@NotNull ResultSetViewer resultSetViewer) {
        super(ResultSetMessages.actions_name_group_row_striping);
        this.resultSetViewer = resultSetViewer;
    }

    @Override
    public void run() {
        DBSDataContainer dataContainer = resultSetViewer.getDataContainer();
        if (dataContainer == null) {
            return;
        }
        DBVEntity vEntity = DBVUtils.getVirtualEntity(dataContainer, true);
        GroupRowStripingDialog dialog = new GroupRowStripingDialog(resultSetViewer, vEntity);
        dialog.open();
    }
}
