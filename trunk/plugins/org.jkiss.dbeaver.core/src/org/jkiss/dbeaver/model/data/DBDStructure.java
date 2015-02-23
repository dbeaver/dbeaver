/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

import java.util.List;

/**
 * Structured data record.
 * Consists of primitive values or other records
 */
public interface DBDStructure extends DBDComplexValue {

    public static final DBSEntityAttribute[] EMPTY_ATTRIBUTE = new DBSEntityAttribute[0];
    public static final Object[] EMPTY_VALUES = new Object[0];

    DBSDataType getDataType();

    @NotNull
    DBSAttributeBase[] getAttributes();

    @Nullable
    Object getAttributeValue(@NotNull DBSAttributeBase attribute)
        throws DBCException;

    void setAttributeValue(@NotNull DBSAttributeBase attribute, @Nullable Object value)
        throws DBCException;

}
