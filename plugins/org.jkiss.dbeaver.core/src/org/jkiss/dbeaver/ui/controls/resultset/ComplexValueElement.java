/*
 * Copyright (C) 2010-2014 Serge Rieder
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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;

/**
 * Complex value element.
 * Map pair or array item
 */
public abstract class ComplexValueElement {

    @NotNull
    private final DBDComplexValue ownerValue;
    @NotNull
    private final DBSAttributeBase attribute;
    @NotNull
    private final DBDValueHandler valueHandler;

    public ComplexValueElement(@NotNull DBDComplexValue ownerValue, @NotNull DBSAttributeBase attribute, @NotNull DBDValueHandler valueHandler) {
        this.ownerValue = ownerValue;
        this.attribute = attribute;
        this.valueHandler = valueHandler;
    }

    @NotNull
    public DBDComplexValue getOwnerValue() {
        return ownerValue;
    }

    @NotNull
    public DBDValueHandler getValueHandler() {
        return valueHandler;
    }

    @Nullable
    public Object getValue() throws DBCException {
        if (ownerValue instanceof DBDStructure) {
            return ((DBDStructure) ownerValue).getAttributeValue(attribute);
        } else if (ownerValue instanceof DBDCollection) {
            return ((DBDCollection) ownerValue).getItem(attribute.getOrdinalPosition());
        } else {
            throw new DBCException("Unsupported component type: " + ownerValue);
        }
    }

}
