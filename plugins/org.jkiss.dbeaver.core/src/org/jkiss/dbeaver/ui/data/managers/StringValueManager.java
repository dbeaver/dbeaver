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
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.data.DBDContentCached;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.editors.ContentInlineEditor;
import org.jkiss.dbeaver.ui.data.editors.ContentPanelEditor;
import org.jkiss.dbeaver.ui.data.editors.StringInlineEditor;
import org.jkiss.dbeaver.ui.dialogs.data.TextViewDialog;
import org.jkiss.dbeaver.ui.editors.content.ContentEditor;
import org.jkiss.dbeaver.utils.ContentUtils;

/**
 * String value manager
 */
public class StringValueManager extends ContentValueManager {

    private static final long PLAIN_STRING_MAX_LENGTH = 100;

    @NotNull
    @Override
    public IValueController.EditType[] getSupportedEditTypes() {
        return new IValueController.EditType[] {IValueController.EditType.INLINE, IValueController.EditType.PANEL, IValueController.EditType.EDITOR};
    }

    @Override
    public IValueEditor createEditor(@NotNull IValueController controller)
        throws DBException
    {
        DBPDataKind dataKind = controller.getValueType().getDataKind();
        switch (controller.getEditType()) {
            case INLINE:
                // Open inline/panel editor
                Object value = controller.getValue();
                if (dataKind == DBPDataKind.STRING || dataKind == DBPDataKind.NUMERIC || dataKind == DBPDataKind.DATETIME || dataKind == DBPDataKind.BOOLEAN) {
                    return new StringInlineEditor(controller);
                } else if (value instanceof DBDContentCached &&
                    ContentUtils.isTextValue(((DBDContentCached) value).getCachedValue()))
                {
                    return new ContentInlineEditor(controller);
                } else {
                    return null;
                }
            case PANEL:
                long maxLength = controller.getValueType().getMaxLength();
                if (dataKind == DBPDataKind.NUMERIC || dataKind == DBPDataKind.DATETIME || dataKind == DBPDataKind.BOOLEAN || (maxLength > 0 && maxLength < PLAIN_STRING_MAX_LENGTH)) {
                    return new StringInlineEditor(controller);
                } else {
                    return new ContentPanelEditor(controller);
                }
            case EDITOR:
                if (controller.getExecutionContext().getDataSource().getContainer().getPreferenceStore().getBoolean(DBeaverPreferences.RESULT_SET_STRING_USE_CONTENT_EDITOR)) {
                    return ContentEditor.openEditor(controller);
                } else {
                    return new TextViewDialog(controller);
                }
            default:
                return null;
        }
    }

}