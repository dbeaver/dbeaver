/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ControlEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.controls.lightgrid.renderers.AbstractRenderer;

public class ResultSetTopLeftRenderer extends AbstractRenderer {
    private Button cfgButton;

    public ResultSetTopLeftRenderer(final ResultSetViewer resultSetViewer) {
        super(resultSetViewer.getSpreadsheet().getGrid());

        cfgButton = new Button(grid, SWT.FLAT | SWT.NO_FOCUS);
        cfgButton.setImage(DBIcon.FILTER.getImage());
        cfgButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                new ResultSetFilterDialog(resultSetViewer).open();
            }
        });
        ControlEditor controlEditor = new ControlEditor(grid);
        controlEditor.setEditor(cfgButton);
        //cfgButton.setText("...");
    }

    @Override
    public void setBounds(Rectangle bounds) {

        Rectangle cfgBounds = new Rectangle(bounds.x + 1, bounds.y + 1, bounds.width - 2, bounds.height - 2);
        cfgButton.setBounds(bounds);

        super.setBounds(bounds);
    }

    public void paint(GC gc) {
        //cfgButton.redraw();
        //gc.drawImage(DBIcon.FILTER.getImage(), 0, 0);
    }

}