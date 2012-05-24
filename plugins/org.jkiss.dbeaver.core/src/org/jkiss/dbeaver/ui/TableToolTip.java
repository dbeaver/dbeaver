/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui;

import org.jkiss.utils.CommonUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;

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
