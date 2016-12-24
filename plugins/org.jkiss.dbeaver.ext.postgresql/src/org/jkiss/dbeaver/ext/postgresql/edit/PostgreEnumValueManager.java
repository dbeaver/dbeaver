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

import org.jkiss.dbeaver.ext.postgresql.model.PostgreAttribute;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.managers.EnumValueManager;

import java.util.ArrayList;
import java.util.List;

/**
 * PostgreSQL ENUM value manager
 */
public class PostgreEnumValueManager extends EnumValueManager {
    @Override
    protected boolean isMultiValue(IValueController valueController) {
        return false;
    }

    @Override
    protected List<String> getEnumValues(IValueController valueController) {
        final PostgreAttribute attribute = (PostgreAttribute) valueController.getValueType();
        if (attribute.getDataType() == null) {
            return null;
        }
        final Object[] values = attribute.getDataType().getEnumValues();
        if (values == null) {
            return null;
        }
        List<String> strValues = new ArrayList<>(values.length);
        for (Object value : values) {
            strValues.add(DBValueFormatting.getDefaultValueDisplayString(value, DBDDisplayFormat.UI));
        }
        return strValues;
    }

    @Override
    protected List<String> getSetValues(IValueController valueController, Object value) {
        return null;
    }

}