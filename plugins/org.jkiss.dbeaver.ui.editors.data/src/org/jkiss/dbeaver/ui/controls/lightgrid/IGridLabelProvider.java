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

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

public interface IGridLabelProvider {

    String OPTION_EXCLUDE_COLUMN_NAME_FOR_WIDTH_CALC = "OPTION_EXCLUDE_COLUMN_NAME_FOR_WIDTH_CALC";

    @NotNull
    String getText(@NotNull IGridItem element);

    @Nullable
    String getDescription(IGridItem element);

    @Nullable
    Image getImage(IGridItem element);

    /**
     * Provides a foreground color for the given element.
     *
     * @param element the element
     * @return	the foreground color for the element, or <code>null</code>
     *   to use the default foreground color
     */
    Color getForeground(IGridItem element);

    /**
     * Provides a background color for the given element.
     *
     * @param element the element
     * @return	the background color for the element, or <code>null</code>
     *   to use the default background color
     */
    Color getBackground(IGridItem element);

    /**
     * Provides a foreground color for the header of the given {@code item}.
     *
     * @param item     the item for which the color should be returned
     * @param selected whether the selection color should be returned instead or not
     * @return the foreground color for the {@code item}
     */
    @NotNull
    Color getHeaderForeground(@Nullable IGridItem item, boolean selected);

    /**
     * Provides a background color for the header of the given {@code item}.
     *
     * @param item     the item for which the color should be returned
     * @param selected whether the selection color should be returned instead or not
     * @return the background color for the {@code item}
     */
    @NotNull
    Color getHeaderBackground(@Nullable IGridItem item, boolean selected);

    /**
     * Provides a border color for the header of the given {@code item}.
     *
     * @param item the item for which the color should be returned
     * @return the border color for the {@code item}
     */
    @NotNull
    Color getHeaderBorder(@Nullable IGridItem item);

    /**
     * Disabled header color
     */
    Color getHeaderReadOnlyColor();

    /**
     * Provides a font for the given element.
     *
     * @param element the element
     * @return the font for the element, or <code>null</code>
     *   to use the default font
     */
    Font getFont(IGridItem element);

    /**
     * Get the text displayed in the tool tip for object.
     *
     * @param element
     *            the element for which the tool tip is shown
     * @return the {@link String} or <code>null</code> if there is not text to
     *         display
     */
    String getToolTipText(IGridItem element);

    Color getErrorForeground();

    Object getGridOption(String option);

}
