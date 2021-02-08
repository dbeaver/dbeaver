/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.utils.CommonUtils;

/**
 * Fake table tooltip provider
 */
public class TableToolTip {

    private final Table table;

    public TableToolTip(Table table) {
        this.table = table;
        applyCustomTolTips();
    }

    public void applyCustomTolTips()
    {
        // Disable native tooltip
        table.setToolTipText (""); //$NON-NLS-1$

        // Implement a "fake" tooltip
        final Listener labelListener = new Listener () {
            @Override
            public void handleEvent (Event event) {
                Label label = (Label)event.widget;
                Shell shell = label.getShell ();
                switch (event.type) {
                    case SWT.MouseExit:
                        shell.dispose ();
                        break;
                }
            }
        };

        Listener tableListener = new Listener () {
            Shell tip = null;
            Label label = null;
            @Override
            public void handleEvent (Event event) {
                switch (event.type) {
                    case SWT.Dispose:
                    case SWT.KeyDown:
                    case SWT.MouseMove: {
                        if (tip == null) break;
                        tip.dispose ();
                        tip = null;
                        label = null;
                        break;
                    }
                    case SWT.MouseHover: {
                        Point eventPt = new Point(event.x, event.y);
                        TableItem item = table.getItem (eventPt);
                        int selectedColumn = -1;
                        if (item != null) {
                            int columnCount = table.getColumnCount();
                            for (int i = 0; i < columnCount; i++) {
                                if (item.getBounds(i).contains(eventPt)) {
                                    selectedColumn = i;
                                    break;
                                }
                            }
                        }

                        if (item != null && selectedColumn >= 0) {
                            String toolTip = getItemToolTip(item, selectedColumn);
                            if (toolTip != null) {
                                toolTip = toolTip.trim();
                            }
                            if (!CommonUtils.isEmpty(toolTip)) {
                                if (tip != null && !tip.isDisposed ()) tip.dispose ();
                                tip = new Shell (table.getShell(), SWT.ON_TOP | SWT.NO_FOCUS | SWT.TOOL);
                                tip.setForeground (table.getDisplay().getSystemColor (SWT.COLOR_INFO_FOREGROUND));
                                tip.setBackground (table.getDisplay().getSystemColor (SWT.COLOR_INFO_BACKGROUND));
                                FillLayout layout = new FillLayout ();
                                layout.marginWidth = 2;
                                tip.setLayout (layout);
                                label = new Label (tip, SWT.NONE);
                                label.setForeground (table.getDisplay().getSystemColor (SWT.COLOR_INFO_FOREGROUND));
                                label.setBackground (table.getDisplay().getSystemColor (SWT.COLOR_INFO_BACKGROUND));
                                label.setText (toolTip);
                                label.addListener (SWT.MouseExit, labelListener);
                                Point size = tip.computeSize (SWT.DEFAULT, SWT.DEFAULT);
                                Point pt = table.toDisplay (event.x, event.y);
                                tip.setBounds (pt.x, pt.y + item.getBounds().height, size.x, size.y);
                                tip.setVisible (true);
                            }
                        }
                    }
                }
            }
        };
        table.addListener (SWT.Dispose, tableListener);
        table.addListener (SWT.KeyDown, tableListener);
        table.addListener (SWT.MouseMove, tableListener);
        table.addListener (SWT.MouseHover, tableListener);
    }

    public String getItemToolTip(TableItem item, int selectedColumn) {
        return item.getText(selectedColumn);
    }

}
