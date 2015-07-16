/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableColumn;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.editors.BaseValueEditor;
import org.jkiss.dbeaver.ui.data.managers.BaseValueManager;
import org.jkiss.dbeaver.ui.dialogs.data.DefaultValueViewDialog;

import java.util.Collection;

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
                return new BaseValueEditor<Combo>(controller) {
                    @Override
                    public void primeEditorValue(@Nullable Object value) throws DBException
                    {
                        MySQLTypeEnum enumValue = (MySQLTypeEnum) value;
                        control.setText(DBUtils.isNullValue(enumValue) ? "" : enumValue.getValue());
                    }
                    @Override
                    public Object extractEditorValue()
                    {
                        int selIndex = control.getSelectionIndex();
                        if (selIndex < 0) {
                            return new MySQLTypeEnum(getColumn(), null);
                        } else {
                            return new MySQLTypeEnum(getColumn(), control.getItem(selIndex));
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
                        return ((MySQLTypeEnum) controller.getValue()).getColumn();
                    }
                };
            }
            case PANEL:
            {
                return new BaseValueEditor<List>(controller) {
                    @Override
                    public void primeEditorValue(@Nullable Object value) throws DBException
                    {
                        MySQLTypeEnum enumValue = (MySQLTypeEnum) value;
                        if (enumValue.isNull()) {
                            control.setSelection(-1);
                        }
                        int itemCount = control.getItemCount();
                        for (int i = 0 ; i < itemCount; i++) {
                            if (control.getItem(i).equals(enumValue.getValue())) {
                                control.setSelection(i);
                                break;
                            }
                        }
                    }

                    @Override
                    public Object extractEditorValue()
                    {
                        int selIndex = control.getSelectionIndex();
                        if (selIndex < 0) {
                            return new MySQLTypeEnum(getColumn(), null);
                        } else {
                            return new MySQLTypeEnum(getColumn(), control.getItem(selIndex));
                        }
                    }

                    @Override
                    protected List createControl(Composite editPlaceholder)
                    {
                        final MySQLTableColumn column = ((MySQLTypeEnum) controller.getValue()).getColumn();
                        final List editor = new List(controller.getEditPlaceholder(), SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL);
                        Collection<String> enumValues = column.getEnumValues();
                        if (enumValues != null) {
                            for (String enumValue : enumValues) {
                                editor.add(enumValue);
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
                        return ((MySQLTypeEnum) controller.getValue()).getColumn();
                    }
                };
            }
            case EDITOR:
                return new DefaultValueViewDialog(controller);
            default:
                return null;
        }
    }

}