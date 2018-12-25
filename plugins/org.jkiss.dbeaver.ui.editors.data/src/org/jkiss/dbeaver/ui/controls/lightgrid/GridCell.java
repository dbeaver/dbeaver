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

package org.jkiss.dbeaver.ui.controls.lightgrid;

import org.jkiss.utils.CommonUtils;

/**
 * GridCell
 */
public class GridCell
{
    public Object col;
    public Object row;

    public GridCell(Object col, Object row)
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
