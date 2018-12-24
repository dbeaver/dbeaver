/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package  org.jkiss.dbeaver.ui.controls.lightgrid;

import org.jkiss.dbeaver.model.data.DBDCellValue;

import java.util.Comparator;

/**
 * Grid position. Tuple of x/y coordinates.
 * Pos comparator orders positions in tree in natural order (first ordered by rows then by columns).
 */
public class GridPos implements DBDCellValue
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
