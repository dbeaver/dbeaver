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
package org.jkiss.dbeaver.ext.sqlite.ui.data.manager;

import org.eclipse.jface.action.IContributionManager;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.managers.ContentValueManager;
import org.jkiss.dbeaver.ui.data.managers.StringValueManager;


/**
 * SQLiteValueHandler
 */
public class SQLiteValueManager extends StringValueManager {

    private static boolean isBinary(@NotNull IValueController controller) {
        DBPDataKind dataKind = controller.getValueType().getDataKind();
        return (dataKind == DBPDataKind.BINARY || dataKind == DBPDataKind.CONTENT);
    }

    @Override
    public IValueEditor createEditor(@NotNull IValueController controller) throws DBException {
        if (isBinary(controller)) {
            return new ContentValueManager().createEditor(controller);
        }
        return super.createEditor(controller);
    }

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull IValueController controller, @Nullable IValueEditor activeEditor) throws DBCException {
        if (isBinary(controller)) {
            new ContentValueManager().contributeActions(manager, controller, activeEditor);
        } else {
            super.contributeActions(manager, controller, activeEditor);
        }
    }
}
