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

import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableColumn;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.managers.EnumValueManager;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * MySQL ENUM value manager
 */
public class MySQLEnumValueManager extends EnumValueManager {

    @Override
    protected boolean isMultiValue(IValueController valueController) {
        return valueController.getValueType().getTypeName().equalsIgnoreCase(MySQLConstants.TYPE_NAME_SET);
    }

    @Override
    protected List<String> getEnumValues(IValueController valueController) {
        return ((MySQLTableColumn) valueController.getValueType()).getEnumValues();
    }

    @Override
    protected List<String> getSetValues(IValueController valueController, Object value) {
        String setString = DBValueFormatting.getDefaultValueDisplayString(value, DBDDisplayFormat.UI);
        List<String> setValues = new ArrayList<String>();
        if (!CommonUtils.isEmpty(setString)) {
            StringTokenizer st = new StringTokenizer(setString, ",");
            while (st.hasMoreTokens()) {
                setValues.add(st.nextToken());
            }
        }
        return setValues;
    }

}