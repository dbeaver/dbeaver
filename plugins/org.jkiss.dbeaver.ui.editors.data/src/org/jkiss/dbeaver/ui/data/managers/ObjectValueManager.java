/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDCursor;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.dialogs.CursorViewDialog;

/**
 * Object value manager.
 */
public class ObjectValueManager extends StringValueManager {

    @Override
    public IValueEditor createEditor(@NotNull final IValueController controller)
        throws DBException
    {
        final Object value = controller.getValue();
        if (controller.getEditType() == IValueController.EditType.EDITOR) {
            if (value instanceof DBDCursor) {
                return new CursorViewDialog(controller);
            }
        }
        return super.createEditor(controller);
    }

}