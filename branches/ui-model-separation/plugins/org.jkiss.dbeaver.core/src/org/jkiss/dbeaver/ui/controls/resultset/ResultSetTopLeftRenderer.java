/*
 * Copyright (C) 2010-2012 Serge Rieder
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

    @Override
    public void paint(GC gc) {
        //cfgButton.redraw();
        //gc.drawImage(DBIcon.FILTER.getImage(), 0, 0);
    }

}