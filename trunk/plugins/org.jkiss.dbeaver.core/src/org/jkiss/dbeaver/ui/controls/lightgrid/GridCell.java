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

package org.jkiss.dbeaver.ui.controls.lightgrid;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.utils.CommonUtils;

/**
 * GridCell
 */
public class GridCell
{
    @NotNull
    public Object col;
    @NotNull
    public Object row;

    public GridCell(@NotNull Object col, @NotNull Object row)
    {
        this.col = col;
        this.row = row;
    }

    public GridCell(GridCell copy)
    {
        this.col = copy.col;
        this.row = copy.row;
    }

    public boolean equals(Object object)
    {
        return object instanceof GridCell && equalsTo((GridCell) object);
    }

    public boolean equalsTo(GridCell pos)
    {
        return CommonUtils.equalObjects(this.col, pos.col) &&
            CommonUtils.equalObjects(this.row, pos.row);
    }

    public String toString()
    {
        return col + ":" + row;
    }

    public int hashCode()
    {
        return col.hashCode() ^ row.hashCode();
    }
}
