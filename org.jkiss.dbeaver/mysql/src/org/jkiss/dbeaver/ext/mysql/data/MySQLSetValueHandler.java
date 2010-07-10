/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.data;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.swt.SWT;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDValueController;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * MySQL SET value handler
 */
public class MySQLSetValueHandler extends MySQLEnumValueHandler {

    public static final MySQLSetValueHandler INSTANCE = new MySQLSetValueHandler();

    public boolean editValue(final DBDValueController controller)
        throws DBException
    {
        if (controller.isInlineEdit()) {
            final MySQLTypeEnum value = (MySQLTypeEnum)controller.getValue();

            org.eclipse.swt.widgets.List editor = new org.eclipse.swt.widgets.List(controller.getInlinePlaceholder(), SWT.BORDER | SWT.MULTI);
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

            } else {
                // Can't edit empty set
                return false;
            }

            editor.setFocus();
            initInlineControl(controller, editor, new ValueExtractor<org.eclipse.swt.widgets.List>() {
                public Object getValueFromControl(org.eclipse.swt.widgets.List control)
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
                    return new MySQLTypeEnum(value.getColumn(), resultString.toString());
                }
            });
            return true;
        } else {
            EnumViewDialog dialog = new EnumViewDialog(controller);
            dialog.open();
            return true;
        }
    }

}