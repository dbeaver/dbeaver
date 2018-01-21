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

    int ALIGN_LEFT          = 0;
    int ALIGN_CENTER        = 1;
    int ALIGN_RIGHT         = 2;

    @NotNull
    Object[] getElements(boolean horizontal);

    @Nullable
    Object[] getChildren(Object element);

    int getSortOrder(@Nullable Object element);

    ElementState getDefaultState(@NotNull Object element);

    int getColumnAlign(@Nullable Object element);

    boolean isElementSupportsFilter(@Nullable Object element);

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
    Color getCellForeground(Object colElement, Object rowElement, boolean selected);

    @Nullable
    Color getCellBackground(Object colElement, Object rowElement, boolean selected);

    @Nullable
    Color getCellHeaderForeground();

    @Nullable
    Color getCellHeaderBackground();

    @Nullable
    Color getCellHeaderSelectionBackground();

    // Resets all cached colors
    void resetColors();

}
