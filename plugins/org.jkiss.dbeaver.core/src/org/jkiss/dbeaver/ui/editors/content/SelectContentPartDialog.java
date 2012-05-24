/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.content;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.IContentEditorPart;

import java.util.List;

/**
 * SelectContentPartDialog
 *
 * @author Serge Rieder
 */
class SelectContentPartDialog extends Dialog {

    private List<IContentEditorPart> dirtyParts;
    private IContentEditorPart selectedPart;

    private SelectContentPartDialog(Shell parentShell, List<IContentEditorPart> dirtyParts)
    {
        super(parentShell);
        this.dirtyParts = dirtyParts;
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText("Choose content editor");

        Composite group = (Composite) super.createDialogArea(parent);
        GridData gd = new GridData(GridData.FILL_BOTH);
        group.setLayoutData(gd);

        Label infoLabel = new Label(group, SWT.NONE);
        infoLabel.setText("Content was modified in mutliple editors. Choose correct one:");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        infoLabel.setLayoutData(gd);

        final Combo combo = new Combo(group, SWT.READ_ONLY | SWT.DROP_DOWN);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        combo.setLayoutData(gd);
        combo.add("");
        for (IContentEditorPart part : dirtyParts) {
            combo.add(part.getContentTypeTitle());
        }
        combo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (combo.getSelectionIndex() >= 1) {
                    selectedPart = dirtyParts.get(combo.getSelectionIndex() - 1);
                } else {
                    selectedPart = null;
                }
                getButton(IDialogConstants.OK_ID).setEnabled(selectedPart != null);
            }
        });
/*
        final Table table = new Table(group, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION);
        table.setLinesVisible (true);
        table.setHeaderVisible(true);
        gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 150;
        gd.widthHint = 200;
        table.setLayoutData(gd);

        TableColumn tableColumn = new TableColumn(table, SWT.NONE);
        tableColumn.setText("Editor");
        for (IContentEditorPart part : dirtyParts) {
            TableItem item = new TableItem(table, SWT.NONE);
            item.setText(part.getContentTypeTitle());
            Image image = part.getContentTypeImage();
            if (image != null) {
                item.setImage(image);
            }
            item.setData(part);
        }
        tableColumn.pack();
        table.pack();

        table.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                TableItem item = (TableItem) e.item;
                selectedPart = (IContentEditorPart) item.getData();
                getButton(IDialogConstants.OK_ID).setEnabled(true);
            }
        });
*/

        return group;
    }

    @Override
    protected Control createContents(Composite parent)
    {
        Control ctl = super.createContents(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
        return ctl;
    }

    public IContentEditorPart getSelectedPart()
    {
        return selectedPart;
    }

    public static IContentEditorPart selectContentPart(Shell parentShell, List<IContentEditorPart> dirtyParts)
    {
        SelectContentPartDialog scDialog = new SelectContentPartDialog(parentShell, dirtyParts);
        if (scDialog.open() == IDialogConstants.OK_ID) {
            return scDialog.getSelectedPart();
        } else {
            return null;
        }
    }
}