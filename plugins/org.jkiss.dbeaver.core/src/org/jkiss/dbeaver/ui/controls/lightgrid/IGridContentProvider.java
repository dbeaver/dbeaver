/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package  org.jkiss.dbeaver.ui.controls.lightgrid;

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.swt.graphics.Color;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPImage;

public interface IGridContentProvider extends IContentProvider {

    enum ElementState {
        NONE,
        EXPANDED,
        COLLAPSED
    }

    int STATE_NONE          = 0;
    int STATE_LINK          = 1;
    int STATE_HYPER_LINK    = 2;
    int STATE_TRANSFORMED   = 4;

    @NotNull
    Object[] getElements(boolean horizontal);

    @Nullable
    Object[] getChildren(Object element);

    int getSortOrder(@Nullable Object element);

    ElementState getDefaultState(@NotNull Object element);

    /**
     *
     * @param cellText    pre-rendered cell text. Used for cache purposes.
     */
    int getCellState(Object colElement, Object rowElement, @Nullable String cellText);

    Object getCellValue(Object colElement, Object rowElement, boolean formatString);

    @NotNull
    String getCellText(Object colElement, Object rowElement);

    @Nullable
    DBPImage getCellImage(Object colElement, Object rowElement);

    @Nullable
    Color getCellForeground(Object colElement, Object rowElement);

    @Nullable
    Color getCellBackground(Object colElement, Object rowElement);

    // Resets all cached colors
    void resetColors();

}
