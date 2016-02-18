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
package org.jkiss.dbeaver.ext.mysql.data;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableColumn;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.editors.BaseValueEditor;
import org.jkiss.dbeaver.ui.data.managers.BaseValueManager;
import org.jkiss.dbeaver.ui.dialogs.data.DefaultValueViewDialog;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;

/**
 * MySQL ENUM value manager
 */
public class MySQLEnumValueManager extends BaseValueManager {

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
                if (controller.getValueType().getTypeName().equalsIgnoreCase(MySQLConstants.TYPE_NAME_SET)) {
                    return null;
                }
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
            Collection<String> enumValues = getColumn().getEnumValues();
            if (enumValues != null) {
                for (String enumValue : enumValues) {
                    editor.add(enumValue);
                }
            }
            if (editor.getSelectionIndex() < 0) {
                editor.select(0);
            }
            return editor;
        }

        private MySQLTableColumn getColumn()
        {
            return ((MySQLTableColumn) controller.getValueType());
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
            final MySQLTableColumn column = getColumn();
            if (column.isTypeSet()) {
                fillSetList(control, column, value);
            } else {
                if (value == null) {
                    control.setSelection(-1);
                }
                int itemCount = control.getItemCount();
                for (int i = 0; i < itemCount; i++) {
                    if (control.getItem(i).equals(value)) {
                        control.setSelection(i);
                        break;
                    }
                }
            }
        }

        @Override
        public Object extractEditorValue()
        {
            if (getColumn().isTypeSet()) {
                StringBuilder setString = new StringBuilder();
                for (String sel : control.getSelection()) {
                    if (setString.length() > 0) setString.append(',');
                    setString.append(sel);
                }
                return setString.toString();
            } else {
                int selIndex = control.getSelectionIndex();
                if (selIndex < 0) {
                    return null;
                } else {
                    return control.getItem(selIndex);
                }
            }
        }

        @Override
        protected List createControl(Composite editPlaceholder)
        {
            final MySQLTableColumn column = getColumn();
            int style = SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL;
            if (column.isTypeSet()) {
                style |= SWT.MULTI;
            } else {
                style |= SWT.SINGLE;
            }
            final List editor = new List(editPlaceholder, style);
            Collection<String> enumValues = column.getEnumValues();
            if (enumValues != null) {
                for (String ev : enumValues) {
                    editor.add(ev);
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

        private MySQLTableColumn getColumn()
        {
            return (MySQLTableColumn) controller.getValueType();
        }
    }

    static void fillSetList(org.eclipse.swt.widgets.List editor, MySQLTableColumn column, Object value)
    {
        editor.removeAll();
        java.util.List<String> enumValues = column.getEnumValues();
        String setString = DBUtils.getDefaultValueDisplayString(value, DBDDisplayFormat.UI);
        java.util.List<String> setValues = new ArrayList<String>();
        if (!CommonUtils.isEmpty(setString)) {
            StringTokenizer st = new StringTokenizer(setString, ",");
            while (st.hasMoreTokens()) {
                setValues.add(st.nextToken());
            }
        }
        if (enumValues != null) {
            int[] selIndices = new int[setValues.size()];
            int selIndex = 0;
            for (int i = 0; i < enumValues.size(); i++) {
                String enumValue = enumValues.get(i);
                editor.add(enumValue);
                if (setValues.contains(enumValue)) {
                    selIndices[selIndex++] = i;
                }
            }
            editor.select(selIndices);
        }
    }

}