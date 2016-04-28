/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.controls.resultset.spreadsheet;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.menus.CommandContributionItem;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.IValueManager;
import org.jkiss.dbeaver.ui.data.editors.ReferenceValueEditor;
import org.jkiss.utils.CommonUtils;

/**
 * RSV value view panel
 */
abstract class ViewValuePanel extends Composite {

    private static final Log log = Log.getLog(ViewValuePanel.class);
    public static final String CMD_SAVE_VALUE = "org.jkiss.dbeaver.core.resultset.cell.save";

    private final IResultSetController resultSet;
    private final Label columnImageLabel;
    private final Text columnNameLabel;
    private final Composite viewPlaceholder;

    private IValueController previewController;
    private IValueEditor valueViewer;
    private ToolBarManager toolBarManager;
    private ReferenceValueEditor referenceValueEditor;

    ViewValuePanel(IResultSetController resultSet, Composite parent)
    {
        super(parent, SWT.NONE);
        this.resultSet = resultSet;
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

        Color infoBackground = getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);

        // Title
        Composite titleBar = UIUtils.createPlaceholder(this, 3);
        ((GridLayout)titleBar.getLayout()).marginWidth = 5;
        ((GridLayout)titleBar.getLayout()).marginHeight = 3;
        ((GridLayout)titleBar.getLayout()).horizontalSpacing = 5;
        titleBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        //titleBar.setBackground(infoBackground);

        columnImageLabel = new Label(titleBar, SWT.NONE);
        columnImageLabel.setImage(DBeaverIcons.getImage(DBIcon.TYPE_OBJECT));

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
        //viewPlaceholder.setBackground(infoBackground);
        viewPlaceholder.addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent e) {
                if (viewPlaceholder.getChildren().length == 0) {
                    String hidePanelCmd = ActionUtils.findCommandDescription(SpreadsheetCommandHandler.CMD_TOGGLE_PREVIEW, ViewValuePanel.this.resultSet.getSite(), true);

                    UIUtils.drawMessageOverControl(viewPlaceholder, e, "Select a cell to view/edit value", 0);
                    UIUtils.drawMessageOverControl(viewPlaceholder, e, "Press " + hidePanelCmd + " to hide this panel", 20);
                }
            }
        });

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

    public void viewValue(final IValueController valueController)
    {
        if (previewController == null || valueController.getValueType() != previewController.getValueType()) {
            cleanupPanel();
            // Rest column info
            columnImageLabel.setImage(DBeaverIcons.getImage(DBUtils.getTypeImage(valueController.getValueType())));
            columnNameLabel.setText(valueController.getValueName() + ": " + valueController.getValueType().getTypeName());
            // Create a new one
            IValueManager valueManager = valueController.getValueManager();
            try {
                valueViewer = valueManager.createEditor(valueController);
            } catch (DBException e) {
                UIUtils.showErrorDialog(getShell(), "Value preview", "Can't create value viewer", e);
                return;
            }
            toolBarManager.removeAll();
            try {
                valueManager.contributeActions(toolBarManager, valueController);
            } catch (DBCException e) {
                log.error("Error filling toolbar actions", e);
            }
            if (valueViewer != null) {
                try {
                    valueViewer.createControl();
                } catch (Exception e) {
                    log.error(e);
                }
                Control control = valueViewer.getControl();
                if (control != null) {
                    resultSet.lockActionsByFocus(control);
                    control.addKeyListener(new KeyAdapter() {
                        @Override
                        public void keyPressed(KeyEvent e) {
                            if (e.keyCode == SWT.CR && e.stateMask == SWT.CTRL) {
                                saveValue();
                                e.doit = false;
                            }
                        }
                    });
                }

                referenceValueEditor = new ReferenceValueEditor(valueController, valueViewer);
                if (referenceValueEditor.isReferenceValue()) {
                    GridLayout gl = new GridLayout(1, false);
                    viewPlaceholder.setLayout(gl);
                    valueViewer.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                    referenceValueEditor.createEditorSelector(viewPlaceholder);
                } else {
                    viewPlaceholder.setLayout(new FillLayout());
                }

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
                referenceValueEditor = null;
            }
            previewController = valueController;

            fillStandardToolBar();
            toolBarManager.update(true);
            toolBarManager.getControl().getParent().layout();
            viewPlaceholder.layout();
        }
        if (valueViewer != null) {
            try {
                if (referenceValueEditor != null) {
                    referenceValueEditor.setHandleEditorChange(false);
                }
                Object newValue = previewController.getValue();
                if (newValue instanceof DBDValue) {
                    // Do not check for difference
                    valueViewer.primeEditorValue(newValue);
                } else {
                    Object oldValue = valueViewer.extractEditorValue();
                    if (!CommonUtils.equalObjects(oldValue, newValue)) {
                        valueViewer.primeEditorValue(newValue);
                    }
                }
            } catch (DBException e) {
                log.error(e);
            } finally {
                if (referenceValueEditor != null) {
                    referenceValueEditor.setHandleEditorChange(true);
                }
            }
        }
    }

    private void saveValue()
    {
        try {
            Object newValue = valueViewer.extractEditorValue();
            previewController.updateValue(newValue);
        } catch (DBException e) {
            UIUtils.showErrorDialog(null, "Value save", "Can't save edited value", e);
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
        columnImageLabel.setImage(null);
        columnNameLabel.setText("");
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
        toolBarManager.add(new Separator());
        if (previewController != null && !previewController.isReadOnly()) {
//                ActionUtils.makeCommandContribution(
//                    resultSet.getSite(),
//                    CMD_SAVE_VALUE,
//                    CommandContributionItem.STYLE_PUSH));

                Action applyAction = new Action("Save cell value", DBeaverIcons.getImageDescriptor(UIIcon.CONFIRM)) {
                    @Override
                    public void run() {
                        saveValue();
                    }
                };
                applyAction.setActionDefinitionId(CMD_SAVE_VALUE);
                applyAction.setId(CMD_SAVE_VALUE);
                applyAction.setAccelerator(SWT.CTRL | SWT.CR);
            toolBarManager.add(applyAction);
        }
        toolBarManager.add(
            ActionUtils.makeCommandContribution(
                resultSet.getSite(),
                SpreadsheetCommandHandler.CMD_TOGGLE_PREVIEW,
                CommandContributionItem.STYLE_PUSH,
                UIIcon.CLOSE));
    }

    public ToolBarManager getToolBar()
    {
        return toolBarManager;
    }
}
