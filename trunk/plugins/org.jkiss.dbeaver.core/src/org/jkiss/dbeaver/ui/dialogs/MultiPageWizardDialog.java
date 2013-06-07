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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWizard;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;

/**
 * MultiPageWizardDialog
 */
public class MultiPageWizardDialog extends TitleAreaDialog implements IWizardContainer {

    private IWizard wizard;
    private Composite pageArea;
    private Tree pagesTree;
    private IDialogPage prevPage;

    private ProgressMonitorPart monitorPart;

    public MultiPageWizardDialog(IWorkbenchWindow window, IWizard wizard)
    {
        this(window, wizard, null);
    }

    public MultiPageWizardDialog(IWorkbenchWindow window, IWizard wizard, IStructuredSelection selection)
    {
        super(window.getShell());

        this.wizard = wizard;
        this.wizard.setContainer(this);
        // Initialize wizard
        if (wizard instanceof IWorkbenchWizard) {
            if (selection == null) {
                if (window.getSelectionService().getSelection() instanceof IStructuredSelection) {
                    selection = (IStructuredSelection)window.getSelectionService().getSelection();
                }
            }
            ((IWorkbenchWizard)wizard).init(window.getWorkbench(), selection);
        }
    }

    public IWizard getWizard()
    {
        return wizard;
    }

    @Override
    protected boolean isResizable()
    {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite composite = (Composite)super.createDialogArea(parent);

        wizard.addPages();

        SashForm sash = new SashForm(composite, SWT.HORIZONTAL);
        sash.setLayoutData(new GridData(GridData.FILL_BOTH));

        pagesTree = new Tree(sash, SWT.SINGLE);
        pagesTree.setLayoutData(new GridData(GridData.FILL_BOTH));
        Composite pageContainer = UIUtils.createPlaceholder(sash, 2);

        // Vertical separator
        new Label(pageContainer, SWT.SEPARATOR | SWT.VERTICAL)
            .setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true));

        pageArea = UIUtils.createPlaceholder(pageContainer, 1);
        pageArea.setLayoutData(new GridData(GridData.FILL_BOTH));
        pageArea.setLayout(new GridLayout(1, true));

        sash.setWeights(new int[] {300 ,700});

        Point maxSize = new Point(0, 0);
        IWizardPage[] pages = wizard.getPages();
        for (int i = 0, pagesLength = pages.length; i < pagesLength; i++) {
            IDialogPage page = pages[i];
            page.createControl(pageArea);
            if (i == 0 && page instanceof ActiveWizardPage) {
                ((ActiveWizardPage) page).activatePage();
            }
            Control control = page.getControl();
            Point pageSize = control.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            if (pageSize.x > maxSize.x) maxSize.x = pageSize.x;
            if (pageSize.y > maxSize.y) maxSize.y = pageSize.y;
            GridData gd = (GridData) control.getLayoutData();
            if (gd == null) {
                gd = new GridData(GridData.FILL_BOTH);
                control.setLayoutData(gd);
            }
            gd.exclude = i > 0;
            control.setVisible(i == 0);

            TreeItem item = new TreeItem(pagesTree, SWT.NONE);
            item.setText(page.getTitle());
            item.setData(page);
        }
        prevPage = (IDialogPage) pagesTree.getItem(0).getData();
        GridData gd = (GridData) pageArea.getLayoutData();
        //gd.minimumWidth = 200;
        //gd.minimumHeight = 200;
        gd.minimumWidth = gd.widthHint = maxSize.x + 10;
        gd.minimumHeight = gd.heightHint = maxSize.y + 10;

        pagesTree.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                changePage();
            }
        });

        // Set title and image from first page
        setTitle(prevPage.getTitle());
        setTitleImage(prevPage.getImage());
        setMessage(prevPage.getMessage());

        // Horizontal separator
        new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR)
            .setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Progress monitor
        monitorPart = new ProgressMonitorPart(composite, null, true);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalIndent = 20;
        gd.verticalIndent = 0;
        monitorPart.setLayoutData(gd);
        monitorPart.setVisible(false);

        return composite;
    }

    private void changePage()
    {
        pageArea.setRedraw(false);
        try {
            TreeItem[] selection = pagesTree.getSelection();
            if (selection.length != 1) {
                return;
            }
            TreeItem newItem = selection[0];
            if (prevPage == newItem.getData()) {
                return;
            }

            GridData gd = (GridData) prevPage.getControl().getLayoutData();
            gd.exclude = true;
            prevPage.setVisible(false);
            if (prevPage instanceof ActiveWizardPage) {
                ((ActiveWizardPage) prevPage).deactivatePage();
            }

            IDialogPage page = (IDialogPage) newItem.getData();
            if (page instanceof ActiveWizardPage) {
                ((ActiveWizardPage) page).activatePage();
            }
            gd = (GridData) page.getControl().getLayoutData();
            gd.exclude = false;
            page.setVisible(true);

            prevPage = page;
            pageArea.layout();
        } finally {
            pageArea.setRedraw(true);
        }
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        if (buttonId == IDialogConstants.CANCEL_ID) {
            getWizard().performCancel();
        } else if (buttonId == IDialogConstants.OK_ID) {
            getWizard().performFinish();
        }
        super.buttonPressed(buttonId);
    }

    @Override
    public IWizardPage getCurrentPage()
    {
        return null;
    }

    @Override
    public void showPage(IWizardPage page)
    {
/*
        for (CTabItem item : pagesFolder.getItems()) {
            if (item.getData() == page) {
                pagesFolder.setSelection(item);
                break;
            }
        }
*/
    }

    @Override
    public void updateButtons()
    {

    }

    @Override
    public void updateMessage()
    {

    }

    @Override
    public void updateTitleBar()
    {

    }

    @Override
    public void updateWindowTitle()
    {

    }

    @Override
    public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException
    {
        // Code copied from WizardDialog
        if (monitorPart != null) {
            monitorPart.setVisible(true);
            monitorPart.layout();
            monitorPart.attachToCancelComponent(null);
        }
        try {
            ModalContext.run(runnable, true, monitorPart, getShell().getDisplay());
        } finally {
            if (monitorPart != null) {
                monitorPart.done();
                monitorPart.setVisible(false);
            }
        }
    }
}
