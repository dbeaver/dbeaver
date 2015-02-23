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

package  org.jkiss.dbeaver.ui.controls.lightgrid;

import java.util.Comparator;

/**
 * Grid position. Tuple of x/y coordinates.
 * Pos comparator orders positions in tree in natural order (first ordered by rows then by columns).
 */
public class GridPos
{
    public int col;
    public int row;

    public GridPos(int col, int row)
    {
        this.col = col;
        this.row = row;
    }

    public GridPos(GridPos copy)
    {
        this.col = copy.col;
        this.row = copy.row;
    }

   public boolean isValid()
    {
        return col >= 0 && row >= 0;
    }

    public boolean equals(Object object)
    {
        return object instanceof GridPos && equalsTo((GridPos) object);
    }

    public boolean equals(int col, int row)
    {
        return this.col == col && this.row == row;
    }

    public boolean equalsTo(GridPos pos)
    {
        return this.col == pos.col && this.row == pos.row;
    }

    public String toString()
    {
        return col + ":" + row;
    }

    public int hashCode()
    {
        return col ^ row;
    }

    public static class PosComparator implements Comparator<GridPos> {
        @Override
        public int compare(GridPos pos1, GridPos pos2)
        {
            int res = pos1.row - pos2.row;
            return res != 0 ? res : pos1.col - pos2.col;
        }
    }
}
