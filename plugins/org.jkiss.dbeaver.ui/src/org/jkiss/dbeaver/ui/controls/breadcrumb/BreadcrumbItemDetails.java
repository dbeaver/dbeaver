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
package org.jkiss.dbeaver.ui.controls.breadcrumb;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

final class BreadcrumbItemDetails {
    private final BreadcrumbItem item;
    private final Composite composite;

    private final Composite detailComposite;
    private final Label elementText;
    private final Label elementImage;

    private boolean hovered;
    private boolean focused;
    private boolean selected;

    BreadcrumbItemDetails(@NotNull BreadcrumbItem item, @NotNull Composite composite) {
        this.item = item;
        this.composite = composite;

        detailComposite = new Composite(composite, SWT.NONE);
        detailComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        detailComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).spacing(0, 0).create());

        var imageComposite = new Composite(detailComposite, SWT.NONE);
        imageComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        imageComposite.setLayout(GridLayoutFactory.fillDefaults().margins(2, 1).create());

        elementImage = new Label(imageComposite, SWT.NONE);
        elementImage.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

        var textComposite = new Composite(detailComposite, SWT.NONE);
        textComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        textComposite.setLayout(GridLayoutFactory.fillDefaults().margins(2, 2).create());
        textComposite.addPaintListener(e -> {
            if (isHovered()) {
                e.gc.drawFocus(e.x, e.y, e.width, e.height);
            }
        });

        elementText = new Label(textComposite, SWT.NONE);
        elementText.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

        MouseTrackAdapter listener = new MouseTrackAdapter() {
            @Override
            public void mouseEnter(MouseEvent e) {
                if (isOver(e)) {
                    setHovered(true);
                }
            }

            @Override
            public void mouseExit(MouseEvent e) {
                if (!isOver(e)) {
                    setHovered(false);
                }
            }

            private boolean isOver(MouseEvent e) {
                Point point = e.display.map((Control) e.widget, detailComposite, e.x, e.y);
                Point size = detailComposite.getSize();
                return point.x >= 0 && point.y >= 0 && point.x < size.x && point.y < size.y;
            }
        };

        addElementListener(detailComposite);
        addElementListener(imageComposite);
        addElementListener(elementImage);
        addElementListener(textComposite);
        addElementListener(elementText);
    }

    public boolean isHovered() {
        return hovered;
    }

    public void setHovered(boolean hovered) {
        if (this.hovered != hovered) {
            this.hovered = hovered;
            this.elementText.getParent().redraw();
        }
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        this.focused &= selected;
    }

    public void setImage(@Nullable Image image) {
        if (image != elementImage.getImage()) {
            elementImage.setImage(image);
        }
    }

    public void setText(@Nullable String text) {
        if (text == null) {
            text = "";
        }
        if (!text.equals(elementText.getText())) {
            elementText.setText(text);
        }
    }

    public void setToolTipText(@Nullable String toolTipText) {
        elementText.getParent().setToolTipText(toolTipText);
        elementText.setToolTipText(toolTipText);
        elementImage.setToolTipText(toolTipText);
    }

    private void addElementListener(@NotNull Control control) {
        control.addMouseTrackListener(new MouseTrackAdapter() {
            @Override
            public void mouseEnter(MouseEvent e) {
                if (isOver(e)) {
                    setHovered(true);
                }
            }

            @Override
            public void mouseExit(MouseEvent e) {
                if (!isOver(e)) {
                    setHovered(false);
                }
            }

            private boolean isOver(MouseEvent e) {
                Point point = e.display.map((Control) e.widget, detailComposite, e.x, e.y);
                Point size = detailComposite.getSize();
                return point.x >= 0 && point.y >= 0 && point.x < size.x && point.y < size.y;
            }
        });
        control.addMenuDetectListener(e -> {
            BreadcrumbViewer viewer = item.getViewer();
            viewer.selectItem(item);
            viewer.fireMenuDetect(e);
        });
    }
}
