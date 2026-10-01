/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
    int STATE_BOOLEAN = 1 << 7;

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

    @NotNull
    ElementState getDefaultState(@NotNull IGridColumn element);

    @NotNull
    IGridStatusColumn[] getStatusColumns();

    int getColumnPinIndex(@NotNull IGridColumn element);

    boolean isElementSupportsFilter(@Nullable IGridColumn element);

    boolean isElementSupportsSort(@Nullable IGridColumn element);

    boolean isElementReadOnly(@NotNull IGridColumn element);

    boolean isElementExpandable(@NotNull IGridItem item);

    boolean isGridReadOnly();

    /**
     * Checks for additional data read according to the specified cell/row
     */
    void validateDataPresence(@NotNull IGridColumn colElement, @NotNull IGridRow rowElement);

    /**
     * Returns cell information.
     * TODO: add returnColors parameter for optimization
     */
    @NotNull
    CellInformation getCellInfo(@NotNull IGridColumn colElement, @NotNull IGridRow rowElement, boolean selected);

    boolean isVoidCell(@NotNull IGridColumn gridColumn, @NotNull IGridRow gridRow);

    /**
     * @param formatString Format string values or return raw values
     *
     */
    @Nullable
    Object getCellValue(@NotNull IGridColumn colElement, @NotNull IGridRow rowElement, boolean formatString);

    @NotNull
    String getCellLinkText(@NotNull IGridColumn colElement, @NotNull IGridRow rowElement);

    @Nullable
    String getCellToolTip(@NotNull IGridColumn colElement, @NotNull IGridRow rowElement);

    @Nullable
    List<IGridHint> getCellHints(@NotNull IGridColumn colElement, @NotNull IGridRow rowElement, @Nullable Object cellValue, int options);

    @Nullable
    List<IGridHint> getColumnHints(@NotNull IGridItem element, int options);

    int getColumnHintsWidth(@NotNull IGridColumn colElement);

    // Resets all cached colors
    void resetColors();

}
