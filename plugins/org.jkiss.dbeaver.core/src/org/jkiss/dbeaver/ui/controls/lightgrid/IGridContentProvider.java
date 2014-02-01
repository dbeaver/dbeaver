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

package  org.jkiss.dbeaver.ui.controls.lightgrid;

import org.eclipse.jface.viewers.IContentProvider;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

public interface IGridContentProvider extends IContentProvider {

    /**
     * Row count
     */
    public int getRowCount();

    /**
     * Column count
     */
    public int getColumnCount();

    /**
     * Get parent row index of specified row
     * @param row
     */
    //public void getRowInfo(int row, GridAxisInfo info);

    /**
     * Get parent column index of specified column
     * @param column column index
     * @return parent column index of -1
     */
    //public int getColumnParent(int column);

    /**
     * Gets raw element value by position
     * @param pos grid position
     * @return element (may be null)
     */
    @Nullable
    public Object getElement(@NotNull GridPos pos);

    /**
     * Gets element string representation. Returns string in native format
     * @param pos grid position
     * @return string representation (never null)
     */
    @NotNull
    public String getElementText(@NotNull GridPos pos);

    /**
     * Updates grid column properties.
     * Invoked once right after grid columns initialization.
     * @param column grid column
     */
    public void updateColumn(@NotNull GridColumn column);

}
