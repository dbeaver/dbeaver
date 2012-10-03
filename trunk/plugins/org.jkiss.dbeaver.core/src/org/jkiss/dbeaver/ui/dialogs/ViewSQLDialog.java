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
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;

public class ViewSQLDialog extends Dialog {

    private IEditorSite subSite;
    private DBSDataSourceContainer dataSource;
    private String title;
    private String text;
    private SQLEditorBase sqlViewer;
    private Image image;
    private boolean showSaveButton = false;

    public ViewSQLDialog(final IWorkbenchPartSite parentSite, DBSDataSourceContainer dataSource, String title, String text)
    {
        super(parentSite.getShell());
        this.dataSource = dataSource;
        this.title = title;
        this.text = text;

        this.subSite = new SubEditorSite(parentSite);
    }

    public void setImage(Image image)
    {
        this.image = image;
    }

    public void setShowSaveButton(boolean showSaveButton)
    {
        this.showSaveButton = showSaveButton;
    }

    @Override
    protected boolean isResizable() {
    	return true;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        getShell().setText(title);
        if (image != null) {
            getShell().setImage(image);
        }

        Composite composite = (Composite) super.createDialogArea(parent);
        Composite editorPH = new Composite(composite, SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 500;
        gd.heightHint = 400;
        editorPH.setLayoutData(gd);
        editorPH.setLayout(new FillLayout());

        sqlViewer = new SQLEditorBase() {
            @Override
            public DBPDataSource getDataSource()
            {
                return dataSource.getDataSource();
            }
        };
        try {
            sqlViewer.init(subSite, new StringEditorInput(title, text, true));
        } catch (PartInitException e) {
            UIUtils.showErrorDialog(getShell(), title, null, e);
        }
        sqlViewer.createPartControl(editorPH);
        sqlViewer.reloadSyntaxRules();

        composite.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                if (sqlViewer != null) {
                    sqlViewer.dispose();
                    sqlViewer = null;
                }
            }
        });
        return parent;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        if (showSaveButton) {
            createButton(parent, IDialogConstants.PROCEED_ID, CoreMessages.dialog_view_sql_button_persist, true);
            createButton(parent, IDialogConstants.DETAILS_ID, CoreMessages.dialog_view_sql_button_copy, false);
            createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        } else {
            createButton(parent, IDialogConstants.DETAILS_ID, CoreMessages.dialog_view_sql_button_copy, false);
            createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);
        }
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        if (buttonId == IDialogConstants.DETAILS_ID) {
            UIUtils.setClipboardContents(getShell().getDisplay(), TextTransfer.getInstance(), text);
        } else if (buttonId == IDialogConstants.PROCEED_ID) {
            setReturnCode(IDialogConstants.PROCEED_ID);
            close();
        } else {
            super.buttonPressed(buttonId);
        }
    }
}