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
package org.jkiss.dbeaver.ui.controls.resultset.spreadsheet;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueEditor;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * RSV value view panel
 */
abstract class ViewValuePanel extends Composite {

    static final Log log = Log.getLog(ViewValuePanel.class);

    private final Label columnImageLabel;
    private final Text columnNameLabel;
    private final Composite viewPlaceholder;

    private DBDValueController previewController;
    private DBDValueEditor valueViewer;
    private ToolBarManager toolBarManager;

    ViewValuePanel(Composite parent)
    {
        super(parent, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        gl.verticalSpacing = 0;
        gl.horizontalSpacing = 0;
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        gl.marginBottom = 5;
        gl.marginRight = 5;
        setLayout(gl);
        //this.setBackground(this.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        this.setLayoutData(new GridData(GridData.FILL_BOTH));
        //Color infoBackground = getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);

        // Title
        Composite titleBar = UIUtils.createPlaceholder(this, 3);
        ((GridLayout)titleBar.getLayout()).marginWidth = 5;
        ((GridLayout)titleBar.getLayout()).marginHeight = 3;
        ((GridLayout)titleBar.getLayout()).horizontalSpacing = 5;
        titleBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        //titleBar.setBackground(infoBackground);

        columnImageLabel = new Label(titleBar, SWT.NONE);
        columnImageLabel.setImage(DBIcon.TYPE_OBJECT.getImage());

        columnNameLabel = new Text(titleBar, SWT.READ_ONLY);
        columnNameLabel.setText("");
        columnNameLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        ToolBar toolBar = new ToolBar(titleBar, SWT.FLAT | SWT.HORIZONTAL);
        toolBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END));
        toolBarManager = new ToolBarManager(toolBar);
        fillStandardToolBar();

        // Value editor
        viewPlaceholder = UIUtils.createPlaceholder(this, 1);
        viewPlaceholder.setLayoutData(new GridData(GridData.FILL_BOTH));
        viewPlaceholder.setLayout(new FillLayout());

        addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_ESCAPE) {
                    hidePanel();
                    e.doit = false;
                }
            }
        });
    }

    protected abstract void hidePanel();

    public Composite getViewPlaceholder()
    {
        return viewPlaceholder;
    }

    public void viewValue(final DBDValueController valueController)
    {
        if (previewController == null || valueController.getValueType() != previewController.getValueType()) {
            cleanupPanel();
            // Rest column info
            columnImageLabel.setImage(DBUtils.getTypeImage(valueController.getValueType()));
            columnNameLabel.setText(valueController.getValueName());
            // Create a new one
            try {
                valueViewer = valueController.getValueHandler().createEditor(valueController);
            } catch (DBException e) {
                UIUtils.showErrorDialog(getShell(), "Value preview", "Can't create value viewer", e);
                return;
            }
            toolBarManager.removeAll();
            try {
                valueController.getValueHandler().contributeActions(toolBarManager, valueController);
            } catch (DBCException e) {
                log.error("Error filling toolbar actions", e);
            }
            if (valueViewer != null) {
                valueViewer.createControl();
            } else {
                final Composite placeholder = UIUtils.createPlaceholder(viewPlaceholder, 1);
                placeholder.setBackground(placeholder.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
                placeholder.addPaintListener(new PaintListener() {
                    @Override
                    public void paintControl(PaintEvent e)
                    {
                        Rectangle bounds = placeholder.getBounds();
                        String message = "No editor for [" + valueController.getValueType().getTypeName() + "]";
                        Point ext = e.gc.textExtent(message);
                        e.gc.drawText(message, (bounds.width - ext.x) / 2, bounds.height / 3 + 20);
                    }
                });
            }
            previewController = valueController;

            fillStandardToolBar();
            toolBarManager.update(true);
            toolBarManager.getControl().getParent().layout();
            viewPlaceholder.layout();
        }
        if (valueViewer != null) {
            try {
                valueViewer.primeEditorValue(previewController.getValue());
            } catch (DBException e) {
                log.error(e);
            }
        }
    }

    public void clearValue()
    {
        cleanupPanel();
        toolBarManager.getControl().getParent().layout();
        viewPlaceholder.layout();
    }

    private void cleanupPanel()
    {
        // Cleanup previous viewer
        for (Control child : viewPlaceholder.getChildren()) {
            child.dispose();
        }
        previewController = null;

        // Cleanup toolbar
        toolBarManager.removeAll();
        toolBarManager.update(true);
    }

    private void fillStandardToolBar()
    {
        toolBarManager.add(new Action("Hide panel", DBIcon.CLOSE.getImageDescriptor()) {
            @Override
            public void run() {
                hidePanel();
            }
        });
    }

    public ToolBarManager getToolBar()
    {
        return toolBarManager;
    }
}
