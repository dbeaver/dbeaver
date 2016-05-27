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
package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

/**
 * Structured data record.
 * Consists of primitive values or other records
 */
public interface DBDComposite extends DBDComplexValue {

    DBSEntityAttribute[] EMPTY_ATTRIBUTE = new DBSEntityAttribute[0];
    Object[] EMPTY_VALUES = new Object[0];

    DBSDataType getDataType();

    @NotNull
    DBSAttributeBase[] getAttributes();

    @Nullable
    Object getAttributeValue(@NotNull DBSAttributeBase attribute)
        throws DBCException;

    void setAttributeValue(@NotNull DBSAttributeBase attribute, @Nullable Object value);

}
