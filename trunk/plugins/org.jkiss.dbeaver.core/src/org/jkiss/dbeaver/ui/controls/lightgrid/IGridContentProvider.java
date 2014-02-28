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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

public interface IGridContentProvider extends IContentProvider {

    /**
     * Row count
     */
    int getRowCount();

    /**
     * Column count
     */
    int getColumnCount();

    int getColumnSortOrder(int column);

    @NotNull
    String getCellText(int col, int row);

    @Nullable
    Image getCellImage(int col, int row);

    @Nullable
    Color getCellForeground(int col, int row);

    @Nullable
    Color getCellBackground(int col, int row);

}
