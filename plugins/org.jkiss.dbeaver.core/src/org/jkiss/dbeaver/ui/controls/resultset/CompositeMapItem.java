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

import org.eclipse.swt.graphics.Image;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDStructure;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;

/**
 * Map item
 */
public class CompositeMapItem implements CompositeObjectElement {

    @NotNull
    private final DBDStructure ownerValue;
    private final DBSAttributeBase attribute;
    private final DBDValueHandler valueHandler;

    public CompositeMapItem(@NotNull DBDStructure ownerValue, @NotNull DBSAttributeBase attribute) {
        this.ownerValue = ownerValue;
        this.attribute = attribute;
        this.valueHandler = DBUtils.findValueHandler(ownerValue.getDataType().getDataSource(), attribute);
    }

    @NotNull
    public String getLabel() {
        return attribute.getName();
    }

    @NotNull
    public Image getImage() {
        return DBUtils.getDataIcon(attribute).getImage();
    }

    @NotNull
    public DBDValueHandler getValueHandler() {
        return valueHandler;
    }

    @Nullable
    public Object getValue() throws DBCException {
        return ownerValue.getAttributeValue(attribute);
    }

    public void setValue(Object value) throws DBCException {
        ownerValue.setAttributeValue(attribute, value);
    }

}
