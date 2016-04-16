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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreAttribute;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.editors.BaseValueEditor;
import org.jkiss.dbeaver.ui.data.managers.BaseValueManager;
import org.jkiss.dbeaver.ui.dialogs.data.DefaultValueViewDialog;

/**
 * PostgreSQL ENUM value manager
 */
public class PostgreEnumValueManager extends BaseValueManager {

    @NotNull
    @Override
    public IValueController.EditType[] getSupportedEditTypes() {
        return new IValueController.EditType[] {IValueController.EditType.INLINE, IValueController.EditType.PANEL, IValueController.EditType.EDITOR};
    }

    @Override
    public IValueEditor createEditor(@NotNull final IValueController controller)
        throws DBException
    {
        switch (controller.getEditType()) {
            case INLINE:
            {
                return new EnumInlineEditor(controller);
            }
            case PANEL:
            {
                return new EnumPanelEditor(controller);
            }
            case EDITOR:
                return new DefaultValueViewDialog(controller);
            default:
                return null;
        }
    }

    private static class EnumInlineEditor extends BaseValueEditor<Combo> {
        private final IValueController controller;

        public EnumInlineEditor(IValueController controller) {
            super(controller);
            this.controller = controller;
        }

        @Override
        public void primeEditorValue(@Nullable Object value) throws DBException
        {
            control.setText(DBUtils.isNullValue(value) ? "" : DBUtils.getDefaultValueDisplayString(value, DBDDisplayFormat.UI));
        }

        @Override
        public Object extractEditorValue()
        {
            int selIndex = control.getSelectionIndex();
            if (selIndex < 0) {
                return null;
            } else {
                return control.getItem(selIndex);
            }
        }

        @Override
        protected Combo createControl(Composite editPlaceholder)
        {
            final Combo editor = new Combo(controller.getEditPlaceholder(), SWT.READ_ONLY);
            final PostgreAttribute attribute = getAttribute();
            Object[] enumValues = attribute.getDataType().getEnumValues();
            if (enumValues != null) {
                for (Object enumValue : enumValues) {
                    editor.add(DBUtils.getDefaultValueDisplayString(enumValue, DBDDisplayFormat.UI));
                }
            }
            if (editor.getSelectionIndex() < 0) {
                editor.select(0);
            }
            return editor;
        }

        private PostgreAttribute getAttribute()
        {
            return (PostgreAttribute) controller.getValueType();
        }
    }

    private static class EnumPanelEditor extends BaseValueEditor<List> {
        private final IValueController controller;

        public EnumPanelEditor(IValueController controller) {
            super(controller);
            this.controller = controller;
        }

        @Override
        public void primeEditorValue(@Nullable Object value) throws DBException
        {
            if (value == null) {
                control.setSelection(-1);
            } else {
                int itemCount = control.getItemCount();
                for (int i = 0; i < itemCount; i++) {
                    if (control.getItem(i).equals(DBUtils.getDefaultValueDisplayString(value, DBDDisplayFormat.UI))) {
                        control.setSelection(i);
                        break;
                    }
                }
            }
        }

        @Override
        public Object extractEditorValue()
        {
            int selIndex = control.getSelectionIndex();
            if (selIndex < 0) {
                return null;
            } else {
                return control.getItem(selIndex);
            }
        }

        @Override
        protected List createControl(Composite editPlaceholder)
        {
            final PostgreAttribute column = getAttribute();
            int style = SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL | SWT.SINGLE;
            final List editor = new List(editPlaceholder, style);
            Object[] enumValues = column.getDataType().getEnumValues();
            if (enumValues != null) {
                for (Object ev : enumValues) {
                    editor.add(DBUtils.getDefaultValueDisplayString(ev, DBDDisplayFormat.UI));
                }
            }
            if (editor.getSelectionIndex() < 0) {
                editor.select(0);
            }
            if (controller.getEditType() == IValueController.EditType.INLINE) {
                editor.setFocus();
            }
            return editor;
        }

        private PostgreAttribute getAttribute()
        {
            return (PostgreAttribute) controller.getValueType();
        }
    }

}