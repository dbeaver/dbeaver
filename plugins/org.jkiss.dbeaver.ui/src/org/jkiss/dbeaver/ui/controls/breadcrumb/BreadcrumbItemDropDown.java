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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.forms.FormColors;
import org.jkiss.code.NotNull;

final class BreadcrumbItemDropDown {
    private final BreadcrumbItem item;
    private final Composite composite;
    private final ToolBar toolBar;

    BreadcrumbItemDropDown(@NotNull BreadcrumbItem item, @NotNull Composite composite) {
        this.item = item;
        this.composite = composite;

        toolBar = new ToolBar(composite, SWT.FLAT);
        toolBar.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));

        var action = new Action(null, new AccessibleArrowImage(isLTR())) {
            @Override
            public void run() {
                super.run();
            }
        };

        var manager = new ToolBarManager(toolBar);
        manager.add(action);
        manager.update(true);
    }

    public void setEnabled(boolean enabled) {
        toolBar.setVisible(enabled);
    }

    private boolean isLTR() {
        return (item.getStyle() & SWT.RIGHT_TO_LEFT) == 0;
    }

    private final class AccessibleArrowImage extends CompositeImageDescriptor {
        private final static int ARROW_SIZE = 5;
        private final boolean ltr;

        public AccessibleArrowImage(boolean ltr) {
            this.ltr = ltr;
        }

        @Override
        protected void drawCompositeImage(int width, int height) {
            Display display = composite.getDisplay();
            ImageDataProvider imageProvider = zoom -> {
                Image image = new Image(display, ARROW_SIZE, ARROW_SIZE * 2);

                GC gc = new GC(image, ltr ? SWT.LEFT_TO_RIGHT : SWT.RIGHT_TO_LEFT);
                gc.setAntialias(SWT.ON);

                Color triangleColor = createColor(SWT.COLOR_LIST_FOREGROUND, SWT.COLOR_LIST_BACKGROUND, 20, display);
                gc.setBackground(triangleColor);
                gc.fillPolygon(new int[]{0, 0, ARROW_SIZE, ARROW_SIZE, 0, ARROW_SIZE * 2});
                gc.dispose();
                triangleColor.dispose();

                ImageData imageData = image.getImageData(zoom);
                image.dispose();
                int zoomedArrowSize = ARROW_SIZE * zoom / 100;
                for (int y1 = 0; y1 < zoomedArrowSize; y1++) {
                    // set opaque pixels for top half of the breadcrumb arrow
                    for (int x1 = 0; x1 <= y1; x1++) {
                        imageData.setAlpha(ltr ? x1 : zoomedArrowSize - x1 - 1, y1, 255);
                    }
                    // set transparent pixels for top half of the breadcrumbe arrow
                    for (int x1 = y1 + 1; x1 < zoomedArrowSize; x1++) {
                        imageData.setAlpha(ltr ? x1 : zoomedArrowSize - x1 - 1, y1, 0);
                    }
                }
                for (int y2 = 0; y2 < zoomedArrowSize; y2++) {
                    // set opaque pixels for bottom half of the breadcrumb arrow
                    for (int x2 = 0; x2 <= y2; x2++) {
                        imageData.setAlpha(ltr ? x2 : zoomedArrowSize - x2 - 1, zoomedArrowSize * 2 - y2 - 1, 255);
                    }
                    // set transparent pixels for bottom half of the breadcrumbe arrow
                    for (int x2 = y2 + 1; x2 < zoomedArrowSize; x2++) {
                        imageData.setAlpha(ltr ? x2 : zoomedArrowSize - x2 - 1, zoomedArrowSize * 2 - y2 - 1, 0);
                    }
                }
                return imageData;
            };

            drawImage(imageProvider, (width / 2) - (ARROW_SIZE / 2), (height / 2) - ARROW_SIZE);
        }

        @Override
        protected Point getSize() {
            return new Point(10, 14);
        }

        @NotNull
        private Color createColor(int color1, int color2, int ratio, @NotNull Display display) {
            RGB rgb1 = display.getSystemColor(color1).getRGB();
            RGB rgb2 = display.getSystemColor(color2).getRGB();
            RGB blend = FormColors.blend(rgb2, rgb1, ratio);

            return new Color(display, blend);
        }
    }
}
