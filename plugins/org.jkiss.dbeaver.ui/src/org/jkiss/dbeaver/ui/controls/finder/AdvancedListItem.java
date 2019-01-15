/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.jkiss.dbeaver.Log;

/**
 * AdvancedListItem
 */
public class AdvancedListItem extends Canvas {

    private static final Log log = Log.getLog(AdvancedListItem.class);

    private final Image icon;

    public AdvancedListItem(AdvancedList list, String text, Image icon) {
        super(list, SWT.TRANSPARENT);

        this.icon = icon;

        GridLayout gl = new GridLayout(1, true);
        gl.marginHeight = 10;
        gl.marginWidth = 10;
        setLayout(gl);

        Point itemSize = list.getImageSize();
        Label iconLabel = new Label(this, SWT.NONE);
        iconLabel.setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        iconLabel.setSize(itemSize);
        GridData gd = new GridData(GridData.VERTICAL_ALIGN_CENTER | GridData.HORIZONTAL_ALIGN_CENTER);
        gd.widthHint = itemSize.x;
        gd.heightHint = itemSize.y;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        iconLabel.setLayoutData(gd);
        iconLabel.addPaintListener(e -> {
            Point size = iconLabel.getSize();
            list.paintIcon(e.gc, e.x, e.y, size.x, size.y, iconLabel, icon);
        });

        Label textLabel = new Label(this, SWT.CENTER);
        textLabel.setText(text);
        textLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
    }

    private AdvancedList getList() {
        return (AdvancedList) getParent();
    }

    @Override
    public Point computeSize(int wHint, int hHint, boolean changed) {
        Point imageSize = getList().getImageSize();
        return new Point(imageSize.x + 40, imageSize.y + 40);
        //return super.computeSize(wHint, hHint, changed);//getList().getImageSize();
    }

    private Image resize(Image image, int width, int height) {
        Image scaled = new Image(getDisplay().getDefault(), width, height);
        GC gc = new GC(scaled);
        gc.setAntialias(SWT.ON);
        gc.setInterpolation(SWT.HIGH);
        gc.drawImage(image, 0,  0,
            image.getBounds().width, image.getBounds().height,
            0, 0, width, height);
        gc.dispose();
        return scaled;
    }
}