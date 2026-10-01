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
package org.jkiss.dbeaver.ui.controls.decorations;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.internal.UIMessages;

/**
 * A link in the bottom right corner of a control that turns holiday decorations off.
 * <p>
 * It's painted by the decorations themselves rather than being a widget of its own,
 * so that snowflakes keep falling over it and the layout of the control is left intact.
 */
final class DecorationsToggle {
    private static final int MARGIN = 12;
    private static final int GAP = 6;
    private static final int PADDING = 4;
    private static final int ALPHA_IDLE = 200;
    private static final int ALPHA_HOVERED = 255;

    private final Control control;
    private final Image image;
    private Rectangle bounds = new Rectangle(0, 0, 0, 0);
    private boolean hovered;

    DecorationsToggle(@NotNull Control control, @NotNull Runnable action) {
        this.control = control;
        this.image = DBeaverIcons.getImage(SnowflakeIcons.SNOWFLAKE);

        control.addListener(SWT.MouseMove, event -> setHovered(contains(event.x, event.y)));
        control.addListener(SWT.MouseExit, event -> setHovered(false));
        control.addListener(SWT.MouseDown, event -> {
            if (event.button == 1 && contains(event.x, event.y)) {
                setHovered(false);
                action.run();
            }
        });
    }

    void paint(@NotNull GC gc) {
        final String label = UIMessages.holiday_decorations_disable_label;
        final Rectangle icon = image.getBounds();
        final Point text = gc.textExtent(label);
        final Point size = control.getSize();

        final int width = icon.width + GAP + text.x;
        final int height = Math.max(icon.height, text.y);
        // Text size depends on the font of the control, so the bounds are only known while painting
        bounds = new Rectangle(size.x - MARGIN - width, size.y - MARGIN - height, width, height);

        final int textX = bounds.x + icon.width + GAP;
        final int textY = bounds.y + (height - text.y) / 2;

        gc.setAlpha(hovered ? ALPHA_HOVERED : ALPHA_IDLE);
        gc.setForeground(control.getForeground());
        gc.drawImage(image, bounds.x, bounds.y + (height - icon.height) / 2);
        gc.drawText(label, textX, textY, true);

        if (hovered) {
            gc.drawLine(textX, textY + text.y - 1, textX + text.x, textY + text.y - 1);
        }

        gc.setAlpha(255);
    }

    private void setHovered(boolean hovered) {
        if (this.hovered == hovered) {
            return;
        }

        this.hovered = hovered;
        control.setCursor(hovered ? control.getDisplay().getSystemCursor(SWT.CURSOR_HAND) : null);
        control.redraw();
    }

    private boolean contains(int x, int y) {
        return x >= bounds.x - PADDING && x < bounds.x + bounds.width + PADDING
            && y >= bounds.y - PADDING && y < bounds.y + bounds.height + PADDING;
    }
}
