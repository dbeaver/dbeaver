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
package org.jkiss.dbeaver.ui.data.managers;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetPreferences;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.dialogs.TextViewDialog;
import org.jkiss.dbeaver.ui.data.editors.ContentPanelEditor;
import org.jkiss.dbeaver.ui.data.editors.StringInlineEditor;
import org.jkiss.dbeaver.ui.editors.content.ContentEditor;

import java.util.UUID;

/**
 * UUID value manager
 */
public class UUIDValueManager extends ContentValueManager {

    @NotNull
    @Override
    public IValueController.EditType[] getSupportedEditTypes() {
        return new IValueController.EditType[] {IValueController.EditType.INLINE, IValueController.EditType.PANEL, IValueController.EditType.EDITOR};
    }

    @Override
    public IValueEditor createEditor(@NotNull IValueController controller) throws DBException {
        switch (controller.getEditType()) {
            case INLINE:
                return new StringInlineEditor(controller) {
                    @Override
                    public Object extractEditorValue() throws DBCException {
                        Object strValue = super.extractEditorValue();
                        if (strValue instanceof String) {
                            if (((String) strValue).isEmpty()) {
                                return null;
                            }
                            try {
                                return UUID.fromString((String) strValue);
                            } catch (Exception e) {
                                throw new DBCException("Bad UUID value [" + strValue + "]");
                            }
                        }
                        return strValue;
                    }
                };
            case PANEL:
                return new ContentPanelEditor(controller);
            case EDITOR:
                if (controller.getExecutionContext().getDataSource().getContainer().getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_STRING_USE_CONTENT_EDITOR)) {
                    return ContentEditor.openEditor(controller);
                } else {
                    return new TextViewDialog(controller);
                }
            default:
                return null;
        }
    }

}
