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
package org.jkiss.dbeaver.ui.controls.resultset.spreadsheet;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.data.hints.DBDValueHint;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.controls.lightgrid.IGridController;
import org.jkiss.dbeaver.ui.controls.lightgrid.IGridHint;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.data.DBDValueHintActionHandler;

/**
 * Spreadsheet cell hint implementation
 */
public class SpreadsheetHint implements IGridHint {

    private final IResultSetController controller;
    private final DBDValueHint valueHint;

    public SpreadsheetHint(IResultSetController controller, DBDValueHint valueHint) {
        this.controller = controller;
        this.valueHint = valueHint;
    }

    @Nullable
    @Override
    public String getHintLabel() {
        return valueHint.getHintDescription();
    }

    @Nullable
    @Override
    public String getText() {
        return valueHint.getHintText();
    }

    @Nullable
    @Override
    public DBPImage getIcon() {
        return valueHint.getHintIcon();
    }

    @Override
    public boolean hasAction() {
        return valueHint instanceof DBDValueHintActionHandler;
    }

    @Override
    public void performAction(@NotNull IGridController grid, long state) {
        try {
            if (valueHint instanceof DBDValueHintActionHandler actionHandler) {
                actionHandler.performAction(controller, state);
            } else {
                throw new DBCException("Hint doesn't support action");
            }
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Hint error", "Error execution hint action", e);
        }
    }

    @Override
    public String getActionToolTip() {
        if (valueHint instanceof DBDValueHintActionHandler ah) {
            return ah.getActionText();
        }
        return null;
    }
}
