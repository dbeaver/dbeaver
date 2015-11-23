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

/**
 * MySQL SET value handler
 */
public class MySQLSetValueHandler extends MySQLEnumValueHandler {

    public static final MySQLSetValueHandler INSTANCE = new MySQLSetValueHandler();

/*
    @Override
    public IValueEditor createEditor(@NotNull final IValueController controller)
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

*/

}