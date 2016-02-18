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
package org.jkiss.dbeaver.ui.editors.content;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

import java.util.List;

/**
 * SelectContentPartDialog
 *
 * @author Serge Rieder
 */
class SelectContentPartDialog extends Dialog {

    private List<ContentEditorPart> dirtyParts;
    private ContentEditorPart selectedPart;

    private SelectContentPartDialog(Shell parentShell, List<ContentEditorPart> dirtyParts)
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
        for (ContentEditorPart part : dirtyParts) {
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
        for (ContentEditorPart part : dirtyParts) {
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
                selectedPart = (ContentEditorPart) item.getData();
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

    public ContentEditorPart getSelectedPart()
    {
        return selectedPart;
    }

    public static ContentEditorPart selectContentPart(Shell parentShell, List<ContentEditorPart> dirtyParts)
    {
        SelectContentPartDialog scDialog = new SelectContentPartDialog(parentShell, dirtyParts);
        if (scDialog.open() == IDialogConstants.OK_ID) {
            return scDialog.getSelectedPart();
        } else {
            return null;
        }
    }
}