/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.editors.BitInlineEditor;
import org.jkiss.dbeaver.ui.data.editors.NumberInlineEditor;
import org.jkiss.dbeaver.ui.data.dialogs.DefaultValueViewDialog;

/**
 * JDBC number value handler
 */
public class NumericValueManager extends BaseValueManager {

    @NotNull
    @Override
    public IValueController.EditType[] getSupportedEditTypes() {
        return new IValueController.EditType[] {IValueController.EditType.INLINE, IValueController.EditType.PANEL, IValueController.EditType.EDITOR};
    }

    @Nullable
    @Override
    public IValueEditor createEditor(@NotNull IValueController controller)
        throws DBException
    {
        switch (controller.getEditType()) {
            case INLINE:
            case PANEL:
                if (controller.getValueType().getDataKind() == DBPDataKind.BOOLEAN) {
                    return new BitInlineEditor(controller);
                } else {
                    return new NumberInlineEditor(controller);
                }
            case EDITOR:
                return new DefaultValueViewDialog(controller);
            default:
                return null;
        }
    }

}