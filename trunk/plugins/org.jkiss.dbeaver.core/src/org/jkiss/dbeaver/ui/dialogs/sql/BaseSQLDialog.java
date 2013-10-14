/*
 * Copyright (C) 2010-2013 Serge Rieder
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
package org.jkiss.dbeaver.ui.dialogs.sql;

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
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;

public abstract class BaseSQLDialog extends Dialog implements IDataSourceProvider {

    private IEditorSite subSite;
    private SQLEditorBase sqlViewer;
    private String title;
    private Image image;
    private StringEditorInput sqlInput;

    public BaseSQLDialog(final IWorkbenchPartSite parentSite, String title, Image image)
    {
        super(parentSite.getShell());
        this.title = title;
        this.image = image;
        this.subSite = new SubEditorSite(parentSite);
    }

    @Override
    protected boolean isResizable() {
    	return true;
    }

    @Override
    public void create()
    {
        super.create();
        getShell().setText(title);
        if (image != null) {
            getShell().setImage(image);
        }
    }

    protected Composite createSQLPanel(Composite parent)
    {
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
                return BaseSQLDialog.this.getDataSource();
            }
        };
        updateSQL();
        sqlViewer.createPartControl(editorPH);
        sqlViewer.reloadSyntaxRules();

        composite.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                sqlViewer.dispose();
            }
        });
        return parent;
    }

    protected abstract String getSQLText();

    protected void createCopyButton(Composite parent)
    {
        createButton(parent, IDialogConstants.DETAILS_ID, CoreMessages.dialog_view_sql_button_copy, false);
    }

    protected void saveToClipboard()
    {
        CharSequence text = getSQLText();
        UIUtils.setClipboardContents(getShell().getDisplay(), TextTransfer.getInstance(), text);
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        if (buttonId == IDialogConstants.DETAILS_ID) {
            saveToClipboard();
        } else {
            super.buttonPressed(buttonId);
        }
    }

    protected void updateSQL()
    {
        try {
            sqlInput = new StringEditorInput(getShell().getText(), getSQLText(), true);
            sqlViewer.init(subSite, sqlInput);
        } catch (PartInitException e) {
            UIUtils.showErrorDialog(getShell(), getShell().getText(), null, e);
        }
    }
}