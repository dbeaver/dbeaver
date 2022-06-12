/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
import org.jkiss.dbeaver.ui.UIElementAlignment;

public interface IGridContentProvider extends IContentProvider {

    // @formatter:off
    int STYLE_NONE          = 0;
    int STYLE_LINK          = 1;
    int STYLE_HYPERLINK     = 1 << 1;
    int STYLE_TRANSFORMED   = 1 << 2;
    int STYLE_TOGGLEABLE    = 1 << 3;
    int STYLE_DECORATED     = 1 << 4;
    // @formatter:on

    /* Data */

    @NotNull
    Object[] getColumnElements();

    @NotNull
    Object[] getRowElements();

    @Nullable
    Object[] getChildren(Object element);

    @Nullable
    Object[] getChildren(@NotNull IGridCell cell);

    boolean isGridReadOnly();

    /* Columns content */

    int getColumnSortOrder(@Nullable Object colElement);

    int getColumnPinIndex(@NotNull Object colElement);

    boolean isColumnSupportsFilter(@NotNull Object colElement);

    boolean isColumnReadOnly(@NotNull Object colElement);

    /* Cells content */

    /**
     * @param cellText Pre-rendered cell text. Used for cache purposes.
     */
    int getCellStyle(@NotNull IGridCell cell, @Nullable String cellText);

    @NotNull
    UIElementAlignment getCellAlign(@NotNull IGridCell cell);

    @NotNull
    GridExpandState getCellInitialExpandState(@NotNull IGridCell cell);

    @Nullable
    Font getCellFont(@NotNull IGridCell cell);

    /**
     * @param formatString Format string values or return raw values
     * @param lockData     Block any automatic data fetch/refresh (without side-effects)
     */
    @Nullable
    Object getCellValue(@NotNull IGridCell cell, boolean formatString, boolean lockData);

    @NotNull
    String getCellText(@NotNull IGridCell cell);

    @Nullable
    String getCellLinkText(@NotNull IGridCell cell);

    @Nullable
    DBPImage getCellImage(@NotNull IGridCell cell);

    @Nullable
    Color getCellForegroundColor(@NotNull IGridCell cell, boolean selected);

    @Nullable
    Color getCellBackgroundColor(@NotNull IGridCell cell, boolean selected);

    @Nullable
    Color getHeaderForegroundColor();

    @Nullable
    Color getHeaderBackgroundColor(boolean selected);

    @NotNull
    Color getHeaderBorderColor();

    // Resets all cached colors
    void resetColors();

}
