/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.data.managers;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.editors.StringInlineEditor;
import org.jkiss.dbeaver.ui.dialogs.data.TextViewDialog;
import org.jkiss.utils.CommonUtils;

import java.util.UUID;

/**
 * UUID value manager
 */
public class UUIDValueManager extends BaseValueManager {
    private static final Log log = Log.getLog(UUIDValueManager.class);

    @NotNull
    @Override
    public IValueController.EditType[] getSupportedEditTypes() {
        return new IValueController.EditType[] {IValueController.EditType.INLINE, IValueController.EditType.PANEL, IValueController.EditType.EDITOR};
    }

    @Override
    public IValueEditor createEditor(@NotNull IValueController controller) throws DBException {
        switch (controller.getEditType()) {
            case INLINE:
            case PANEL:
                return new StringInlineEditor(controller) {
                    @Override
                    public Object extractEditorValue() throws DBCException {
                        String strValue = (String) super.extractEditorValue();
                        if (strValue == null || strValue.isEmpty()) {
                            return null;
                        }
                        try {
                            return UUID.fromString(CommonUtils.toString(strValue));
                        } catch (Exception e) {
                            log.warn(e);
                            return null;
                        }
                    }
                };
            case EDITOR:
                return new TextViewDialog(controller) {
                    @Override
                    public Object extractEditorValue() {
                        String strValue = (String) super.extractEditorValue();
                        if (strValue == null || strValue.isEmpty()) {
                            return null;
                        }
                        try {
                            return UUID.fromString(CommonUtils.toString(super.extractEditorValue()));
                        } catch (Exception e) {
                            log.warn(e);
                            return null;
                        }
                    }
                };
            default:
                return null;
        }
    }

}
