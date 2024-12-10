/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPImage;

import java.util.List;

public interface IGridContentProvider extends IContentProvider {

    enum ElementState {
        NONE,
        EXPANDED,
        COLLAPSED
    }

    int STATE_NONE = 0;
    int STATE_LINK = 1;
    int STATE_HYPER_LINK = 1 << 1;
    int STATE_TRANSFORMED = 1 << 2;
    int STATE_TOGGLE = 1 << 3;
    int STATE_DECORATED = 1 << 4;
    int STATE_EXPANDED = 1 << 5;
    int STATE_COLLAPSED = 1 << 6;
    int STATE_HINT = 1 << 8;

    int ALIGN_LEFT = 0;
    int ALIGN_CENTER = 1;
    int ALIGN_RIGHT = 2;

    class CellInformation {
        public int state;
        public int align;
        public Font font;
        public DBPImage image;
        public Color foreground;
        public Color background;
        public Object value;
        public Object text;
    }

    @NotNull
    Object[] getElements(boolean horizontal);

    boolean hasChildren(@NotNull IGridItem item);

    @Nullable
    Object[] getChildren(@NotNull IGridItem item);

    /**
     * Return for collection cell values returns size of collection.
     * Called for all cells of columns for which isCollectionElement() returns true.
     */
    int getCollectionSize(@NotNull IGridColumn colElement, @NotNull IGridRow rowElement);

    int getSortOrder(@Nullable IGridColumn element);

    ElementState getDefaultState(@NotNull IGridColumn element);

    IGridStatusColumn[] getStatusColumns();

    int getColumnPinIndex(@NotNull IGridColumn element);

    boolean isElementSupportsFilter(@Nullable IGridColumn element);

    boolean isElementSupportsSort(@Nullable IGridColumn element);

    boolean isElementReadOnly(IGridColumn element);

    boolean isElementExpandable(@NotNull IGridItem item);

    boolean isGridReadOnly();

    /**
     * Checks for additional data read according to the specified cell/row
     */
    void validateDataPresence(IGridColumn colElement, IGridRow rowElement);

    /**
     * Returns cell information.
     * TODO: add returnColors parameter for optimization
     */
    @NotNull
    CellInformation getCellInfo(IGridColumn colElement, IGridRow rowElement, boolean selected);

    boolean isVoidCell(IGridColumn gridColumn, IGridRow gridRow);

    /**
     * @param formatString Format string values or return raw values
     *
     */
    Object getCellValue(IGridColumn colElement, IGridRow rowElement, boolean formatString);

    @NotNull
    String getCellLinkText(IGridColumn colElement, IGridRow rowElement);

    String getCellToolTip(IGridColumn colElement, IGridRow rowElement);

    List<IGridHint> getCellHints(IGridColumn colElement, IGridRow rowElement, Object cellValue, int options);

    int getColumnHintsWidth(IGridColumn colElement);

    // Resets all cached colors
    void resetColors();

}
