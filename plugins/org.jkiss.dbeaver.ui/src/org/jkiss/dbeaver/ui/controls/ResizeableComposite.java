/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tracker;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;

/**
 * A composite that can be resized by dragging its borders.
 *
 * <dl>
 * <dt><b>Styles:</b><dd>HORIZONTAL, VERTICAL
 * </dl>
 */
public final class ResizeableComposite extends Composite {
    private final int style;
    private Control content;
    private Point minSize = new Point(0, 0);
    private Point prefSize = new Point(SWT.DEFAULT, SWT.DEFAULT);

    public ResizeableComposite(@NotNull Composite parent, int style) {
        super(parent, SWT.NONE);
        this.style = style;

        boolean horizontal = (style & SWT.HORIZONTAL) != 0;
        boolean vertical = (style & SWT.VERTICAL) != 0;

        if (!horizontal && !vertical) {
            throw new IllegalArgumentException("Must specify at least one of HORIZONTAL or VERTICAL styles");
        }

        GridLayoutFactory.fillDefaults()
            .numColumns(horizontal ? 2 : 1)
            .spacing(0, 0)
            .applyTo(this);

        if (horizontal) {
            Label dragger = new Label(this, SWT.NONE);
            dragger.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
            dragger.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_SIZEWE));
            dragger.setImage(DBeaverIcons.getImage(UIIcon.SEPARATOR_V));
            dragger.addMouseListener(MouseListener.mouseDownAdapter(e -> update(e, true)));
        }

        if (vertical) {
            Label dragger = new Label(this, SWT.NONE);
            dragger.setLayoutData(new GridData(SWT.CENTER, SWT.BEGINNING, false, false));
            dragger.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_SIZENS));
            dragger.setImage(DBeaverIcons.getImage(UIIcon.SEPARATOR_H));
            dragger.addMouseListener(MouseListener.mouseDownAdapter(e -> update(e, false)));

            if (horizontal) {
                ((GridData) dragger.getLayoutData()).horizontalSpan = 2;
            }
        }
    }

    /**
     * Sets the content of this composite.
     *
     * @param content the control to be added as content
     */
    public void setContent(@NotNull Control content) {
        checkWidget();
        this.content = content;
        this.content.moveAbove(null);
        updateLayoutData();
    }

    /**
     * Specifies the minimum size of the content.
     *
     * @param minSize the minimum size of the content
     */
    public void setMinSize(@NotNull Point minSize) {
        checkWidget();
        this.minSize = new Point(minSize.x, minSize.y);
        updateLayoutData();
    }

    /**
     * Specifies the preferred size of the content.
     *
     * @param prefSize the preferred size of the content
     */
    public void setPrefSize(@NotNull Point prefSize) {
        checkWidget();
        this.prefSize = new Point(prefSize.x, prefSize.y);
        updateLayoutData();
    }

    private void updateLayoutData() {
        if (content != null) {
            content.setLayoutData(createLayoutData());
            layout(true, true);
        }
    }

    @NotNull
    private GridData createLayoutData() {
        GridData data = new GridData(
            SWT.FILL,
            SWT.FILL,
            (style & SWT.HORIZONTAL) == 0,
            (style & SWT.VERTICAL) == 0
        );
        if (minSize != null) {
            data.minimumWidth = minSize.x;
            data.minimumHeight = minSize.y;
        }
        if (prefSize != null) {
            data.widthHint = prefSize.x;
            data.heightHint = prefSize.y;
        }
        return data;
    }

    private void update(@NotNull MouseEvent e, boolean horizontal) {
        if (content == null) {
            return;
        }

        Point size = content.getSize();
        Control control = (Control) e.widget;

        Rectangle rectangle = new Rectangle(0, 0, 0, 0);
        if (horizontal) {
            rectangle.width = control.getLocation().x + e.x;
            rectangle.height = size.y;
        } else {
            rectangle.width = size.x;
            rectangle.height = control.getLocation().y + e.y;
        }

        Tracker tracker = new Tracker(this, SWT.RESIZE | (horizontal ? SWT.RIGHT : SWT.DOWN));
        tracker.setStippled(true);
        tracker.setRectangles(new Rectangle[]{rectangle});

        if (tracker.open()) {
            GridData data = (GridData) content.getLayoutData();

            // Adjustment for the difference between the actual size and the hint
            int adjustment;
            if (horizontal) {
                adjustment = data.widthHint != SWT.DEFAULT ? size.x - data.widthHint : 0;
            } else {
                adjustment = data.heightHint != SWT.DEFAULT ? size.y - data.heightHint : 0;
            }

            // Extra width/height between the content and the expander
            int extra;
            if (horizontal) {
                extra = rectangle.width - size.x;
            } else {
                extra = rectangle.height - size.y;
            }

            Rectangle result = tracker.getRectangles()[0];
            tracker.dispose();

            if (horizontal) {
                int width = result.width - adjustment - extra;
                if (width == size.x) {
                    return;
                }
                data.widthHint = Math.max(width, data.minimumWidth);
            } else {
                int height = result.height - adjustment - extra;
                if (height == size.y) {
                    return;
                }
                data.heightHint = Math.max(height, data.minimumHeight);
            }

            layout(true, true);
        }
    }
}
