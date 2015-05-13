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
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableColumn;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueEditor;
import org.jkiss.dbeaver.model.impl.data.editors.BaseValueEditor;
import org.jkiss.dbeaver.ui.dialogs.data.DefaultValueViewDialog;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * MySQL SET value handler
 */
public class MySQLSetValueHandler extends MySQLEnumValueHandler {

    public static final MySQLSetValueHandler INSTANCE = new MySQLSetValueHandler();

    @Override
    public DBDValueEditor createEditor(@NotNull final DBDValueController controller)
        throws DBException
    {
        switch (controller.getEditType()) {
            case INLINE:
            case PANEL:
                final MySQLTableColumn column = ((MySQLTypeEnum) controller.getValue()).getColumn();

                return new BaseValueEditor<org.eclipse.swt.widgets.List>(controller) {
                    @Override
                    public void primeEditorValue(@Nullable Object value) throws DBException
                    {
                        MySQLTypeEnum enumValue = (MySQLTypeEnum) value;
                        fillSetList(control, enumValue);
                    }
                    @Override
                    public Object extractEditorValue()
                    {
                        String[] selection = control.getSelection();
                        StringBuilder resultString = new StringBuilder();
                        for (String selString : selection) {
                            if (CommonUtils.isEmpty(selString)) {
                                continue;
                            }
                            if (resultString.length() > 0) resultString.append(',');
                            resultString.append(selString);
                        }
                        return new MySQLTypeEnum(column, resultString.toString());
                    }

                    @Override
                    protected org.eclipse.swt.widgets.List createControl(Composite editPlaceholder)
                    {
                        return new org.eclipse.swt.widgets.List(controller.getEditPlaceholder(), SWT.BORDER | SWT.MULTI);
                    }
                };
            case EDITOR:
                return new DefaultValueViewDialog(controller);
            default:
                return null;
        }
    }

    static void fillSetList(org.eclipse.swt.widgets.List editor, MySQLTypeEnum value)
    {
        editor.removeAll();
        List<String> enumValues = value.getColumn().getEnumValues();
        String setString = value.getValue();
        List<String> setValues = new ArrayList<String>();
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