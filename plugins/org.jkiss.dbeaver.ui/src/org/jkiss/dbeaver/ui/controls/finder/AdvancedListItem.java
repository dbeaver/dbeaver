/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.finder;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.*;
import org.jkiss.dbeaver.Log;

/**
 * AdvancedListItem
 */
public class AdvancedListItem {

    private static final Log log = Log.getLog(AdvancedListItem.class);
    public static final int BORDER_MARGIN = 5;

    private final AdvancedList list;
    private final Object data;
    private final ILabelProvider labelProvider;
    private boolean isHover;
    final TextLayout textLayout;

    public AdvancedListItem(AdvancedList list, Object item, ILabelProvider labelProvider) {
        this.labelProvider = labelProvider;
        this.list = list;
        this.list.addItem(this);
        this.data = item;
        this.textLayout = new TextLayout(list.getDisplay());
        this.textLayout.setText(labelProvider.getText(item));
    }

    public ILabelProvider getLabelProvider() {
        return labelProvider;
    }

    public Object getData() {
        return data;
    }

    void painItem(PaintEvent e, int x, int y) {
        Point itemSize = list.getItemSize();
        if (itemSize.x <= 0 || itemSize.y <= 0) {
            return;
        }
        boolean isSelected = list.getSelectedItem() == this;

        GC gc = e.gc;
        if (isSelected) {
            gc.setBackground(list.getSelectionBackgroundColor());
            gc.setForeground(list.getSelectionForegroundColor());
        } else if (isHover) {
            gc.setBackground(list.getHoverBackgroundColor());
            gc.setForeground(list.getForegroundColor());
        } else {
            gc.setBackground(list.getBackground());
            gc.setForeground(list.getForegroundColor());
        }

        if (isSelected || isHover) {
            gc.fillRoundRectangle(x + 2, y + 2, itemSize.x - 4, itemSize.y - 4, 5, 5);
        }

        Image icon = labelProvider.getImage(data);
        Rectangle iconBounds = icon.getBounds();
        Point imageSize = list.getImageSize();

        int imgPosX = (itemSize.x - imageSize.x) / 2;
        int imgPosY = BORDER_MARGIN;//(itemBounds.height - iconBounds.height) / 2 ;

        if (iconBounds.width == imageSize.x && iconBounds.height == imageSize.y) {
            gc.drawImage(icon, x + imgPosX - e.x, y + imgPosY);
        } else {
            gc.drawImage(icon, 0, 0, iconBounds.width, iconBounds.height,
                x + imgPosX - e.x, y + imgPosY, imageSize.x, imageSize.y);
        }

        gc.setAntialias(SWT.ON);
        gc.setInterpolation(SWT.HIGH);
        this.textLayout.setWidth(itemSize.x - BORDER_MARGIN * 2);
        this.textLayout.setAlignment(SWT.CENTER);
        this.textLayout.draw(gc, x + BORDER_MARGIN, y + imageSize.y + BORDER_MARGIN);
    }

    public boolean isHover() {
        return isHover;
    }

}