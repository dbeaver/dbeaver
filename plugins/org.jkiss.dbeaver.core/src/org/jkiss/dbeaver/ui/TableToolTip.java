/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
                            String toolTip = item.getText(selectedColumn);
                            if (toolTip != null) {
                                toolTip = toolTip.trim();
                            }
                            if (!CommonUtils.isEmpty(toolTip)) {
                                if (tip != null && !tip.isDisposed ()) tip.dispose ();
                                tip = new Shell (table.getShell(), SWT.ON_TOP | SWT.NO_FOCUS | SWT.TOOL);
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

}
