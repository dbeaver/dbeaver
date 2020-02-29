/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;

public class CustomizeColorsAction extends ColorAction {
    private ResultSetViewer resultSetViewer;
    private final DBDAttributeBinding curAttribute;
    private final ResultSetRow row;

    public CustomizeColorsAction(ResultSetViewer resultSetViewer, DBDAttributeBinding curAttribute, ResultSetRow row) {
        super(resultSetViewer, ResultSetMessages.actions_name_row_colors); //$NON-NLS-1$
        this.resultSetViewer = resultSetViewer;
        this.curAttribute = curAttribute;
        this.row = row;
    }

    @Override
    public void run() {
        final DBVEntity vEntity = getColorsVirtualEntity();
        ColorSettingsDialog dialog = new ColorSettingsDialog(resultSetViewer, vEntity, curAttribute, row);
        if (dialog.open() != IDialogConstants.OK_ID) {
            return;
        }
        updateColors(vEntity);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
