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

import org.eclipse.swt.graphics.*;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UITextUtils;

/**
 * Grid row header renderer.
 */
class GridRowRenderer extends AbstractRenderer {
    static final Image IMG_EXPAND = DBeaverIcons.getImage(UIIcon.TREE_EXPAND);
    static final Image IMG_COLLAPSE = DBeaverIcons.getImage(UIIcon.TREE_COLLAPSE);
    static final Rectangle EXPANDED_BOUNDS = IMG_EXPAND.getBounds();

    public static final int LEFT_MARGIN = 4;
    public static final int RIGHT_MARGIN = 4;
    public static final int IMAGE_SPACING = 5;
    public static final int EXPANDER_SPACING = 2;
    public static final int LEVEL_SPACING = EXPANDED_BOUNDS.width;

    public GridRowRenderer(LightGrid grid) {
        super(grid);
    }

    public void paint(
        GC gc,
        Rectangle bounds,
        boolean selected,
        int level,
        IGridContentProvider.ElementState state,
        IGridRow element)
    {
        String text = grid.getLabelProvider().getText(element);

        gc.setFont(grid.normalFont);
        gc.setBackground(grid.getLabelProvider().getHeaderBackground(element, selected));
        gc.fillRectangle(bounds.x, bounds.y, bounds.width, bounds.height + 1);

        {
            gc.setForeground(grid.getLabelProvider().getHeaderBorder(element));

            gc.drawLine(
                bounds.x + bounds.width - 1,
                bounds.y,
                bounds.x + bounds.width - 1,
                bounds.y + bounds.height - 1);
            gc.drawLine(
                bounds.x,
                bounds.y + bounds.height - 1,
                bounds.x + bounds.width - 1,
                bounds.y + bounds.height - 1);
        }

        int x = LEFT_MARGIN;
        if (level > 0) {
            x += level * LEVEL_SPACING;
        }
        if (state != IGridContentProvider.ElementState.NONE) {
            Image expandImage = state == IGridContentProvider.ElementState.EXPANDED ? IMG_COLLAPSE : IMG_EXPAND;
            gc.drawImage(expandImage, x, bounds.y + (bounds.height - EXPANDED_BOUNDS.height) / 2);
            x += EXPANDED_BOUNDS.width + EXPANDER_SPACING;
        } else if (grid.getContentProvider().isElementExpandable(element) && level == 0) {
            x += EXPANDED_BOUNDS.width + EXPANDER_SPACING;
        }

        Image image = grid.getLabelProvider().getImage(element);

        if (image != null) {
            gc.drawImage(image, x, bounds.y + (bounds.height - image.getBounds().height) / 2);
            x += image.getBounds().width + IMAGE_SPACING;
        }

        gc.setForeground(grid.getLabelProvider().getHeaderForeground(element, false));

        Font font = grid.getLabelProvider().getFont(element);
        if (font == null) {
            font = (element == grid.getFocusRowElement() ? grid.boldFont : grid.normalFont);
        }
        gc.setFont(font);
        gc.drawString(
            UITextUtils.getShortString(grid.fontMetrics, text, bounds.width - x - RIGHT_MARGIN),
            bounds.x + x,
            bounds.y + (bounds.height - gc.stringExtent(text).y) / 2,
            isTransparent
        );
    }

    public int computeHeaderWidth(IGridRow element, int level) {
        int width = GridRowRenderer.LEFT_MARGIN + GridRowRenderer.RIGHT_MARGIN;
        if (grid.getContentProvider().isElementExpandable(element)) {
            width += GridRowRenderer.EXPANDED_BOUNDS.width + EXPANDER_SPACING;
        }
        Image rowImage = grid.getLabelProvider().getImage(element);
        if (rowImage != null) {
            width += rowImage.getBounds().width;
            width += GridRowRenderer.IMAGE_SPACING;
        }
        String rowText = grid.getLabelProvider().getText(element);
        Point ext = grid.sizingGC.stringExtent(rowText);
        width += ext.x;
        width += level * GridRowRenderer.LEVEL_SPACING;
        return width;
    }

    public static boolean isOverExpander(int x, int level)
    {
        int expandBegin = LEFT_MARGIN + level * LEVEL_SPACING;
        int expandEnd = expandBegin + EXPANDED_BOUNDS.width;
        return x >= expandBegin && x <= expandEnd;
    }
}
