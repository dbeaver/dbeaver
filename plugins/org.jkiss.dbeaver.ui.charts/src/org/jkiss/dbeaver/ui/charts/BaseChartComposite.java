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

package org.jkiss.dbeaver.ui.charts;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.*;
import org.jfree.chart.swt.ChartComposite;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;

import java.io.IOException;

/**
 * Base chart composite
 */
public class BaseChartComposite extends ChartComposite {

    public BaseChartComposite(Composite parent, int style, Point preferredSize) {
        super(parent, style, null,
            preferredSize.x, preferredSize.y,
            30, 20,
            10000, 10000,
            true, false, true, true, true, true);
    }

    public Canvas getChartCanvas() {
        Control[] children = getChildren();
        return children.length == 0 ? null : (Canvas) children[0];
    }

    @Override
    public void mouseDoubleClick(MouseEvent event) {
        if (showChartConfigDialog()) {
            forceRedraw();
        }
    }

    @Override
    protected Menu createPopupMenu(boolean properties, boolean save, boolean print, boolean zoom) {
        MenuManager manager = new MenuManager();

        fillContextMenu(manager);

        return manager.createContextMenu(this);
    }

    protected void fillContextMenu(MenuManager manager) {
        manager.add(new Action("Settings ...", DBeaverIcons.getImageDescriptor(UIIcon.CONFIGURATION)) {
            @Override
            public void run() {
                showChartConfigDialog();
            }
        });
        manager.add(new Separator());
        manager.add(new Action("Save as ...") {
            @Override
            public void run() {
                try {
                    doSaveAs();
                } catch (IOException e) {
                    DBWorkbench.getPlatformUI().showError("Save image", "Error saving chart image", e);
                }
            }
        });
        manager.add(new Action("Print ...") {
            @Override
            public void run() {
                createChartPrintJob();
            }
        });
        manager.add(new Separator());
        manager.add(new Action("Zoom In", DBeaverIcons.getImageDescriptor(UIIcon.ZOOM_IN)) {
            @Override
            public void runWithEvent(Event e) {
                zoomInBoth(e.x, e.y);
            }
        });
        manager.add(new Action("Zoom Out", DBeaverIcons.getImageDescriptor(UIIcon.ZOOM_OUT)) {
            @Override
            public void runWithEvent(Event e) {
                zoomOutBoth(e.x, e.y);
            }
        });
        manager.add(new Action("Zoom Reset", DBeaverIcons.getImageDescriptor(UIIcon.ZOOM)) {
            @Override
            public void runWithEvent(Event e) {
                restoreAutoBounds();
            }
        });

    }

    protected boolean showChartConfigDialog() {
        return false;
    }

}
