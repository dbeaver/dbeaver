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

import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;

/**
 * Result set row
 */
public class ResultSetRow {

    private ResultSetViewer viewer;
    private final Object[] values;

    ResultSetRow(ResultSetViewer viewer, Object[] values)
    {
        this.viewer = viewer;
        this.values = values;
    }

    public int getValueCount()
    {
        return values.length;
    }

    public Object[] getValues()
    {
        return values;
    }

    public Object getValue(DBSAttributeBase attribute)
    {
        DBDAttributeBinding binding = viewer.getModel().getAttributeBinding(attribute);
        return binding == null ? null : values[binding.getAttributeIndex()];
    }

}
