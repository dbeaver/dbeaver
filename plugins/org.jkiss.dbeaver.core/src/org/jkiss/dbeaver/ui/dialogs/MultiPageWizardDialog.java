/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.ControlEnableState;
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
import org.jkiss.dbeaver.ui.ICompositeDialogPage;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.ArrayUtils;

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
    private SashForm wizardSash;
    private volatile int runningOperations = 0;

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
    protected int getShellStyle()
    {
        return SWT.TITLE | SWT.MAX | SWT.RESIZE | SWT.APPLICATION_MODAL;
    }

    @Override
    protected Control createContents(Composite parent)
    {
        Control contents = super.createContents(parent);
        updateButtons();
        return contents;
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite composite = (Composite)super.createDialogArea(parent);

        wizard.addPages();

        wizardSash = new SashForm(composite, SWT.HORIZONTAL);
        wizardSash.setLayoutData(new GridData(GridData.FILL_BOTH));

        pagesTree = new Tree(wizardSash, SWT.SINGLE);
        pagesTree.setLayoutData(new GridData(GridData.FILL_BOTH));
        Composite pageContainer = UIUtils.createPlaceholder(wizardSash, 2);

        // Vertical separator
        new Label(pageContainer, SWT.SEPARATOR | SWT.VERTICAL)
            .setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true));

        pageArea = UIUtils.createPlaceholder(pageContainer, 1);
        GridData gd = new GridData(GridData.FILL_BOTH);
        pageArea.setLayoutData(gd);
        pageArea.setLayout(new GridLayout(1, true));

        wizardSash.setWeights(new int[]{300, 700});

        Point maxSize = new Point(0, 0);
        IWizardPage[] pages = wizard.getPages();
        for (IWizardPage page : pages) {
            addPage(null, page, maxSize);
        }
        gd = (GridData) pageArea.getLayoutData();
        gd.widthHint = 500;
        gd.heightHint = 400;

        pagesTree.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                changePage();
            }
        });
        // Select first page
        pagesTree.select(pagesTree.getItem(0));
        changePage();

        // Set title and image from first page
        IDialogPage firstPage = (IDialogPage) pagesTree.getItem(0).getData();
        setTitle(firstPage.getTitle());
        setTitleImage(firstPage.getImage());
        setMessage(firstPage.getMessage());

        // Horizontal separator
        new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR)
            .setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Progress monitor
        monitorPart = new ProgressMonitorPart(composite, null, true) {
            @Override
            public void setCanceled(boolean b) {
                super.setCanceled(b);
                if (b) {
                    cancelCurrentOperation();
                }
            }
        };
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalIndent = 20;
        gd.verticalIndent = 0;
        monitorPart.setLayoutData(gd);
        monitorPart.setVisible(false);

        return composite;
    }

    protected void cancelCurrentOperation() {

    }

    private TreeItem addPage(TreeItem parentItem, IDialogPage page, Point maxSize)
    {
        TreeItem item = parentItem == null ?
            new TreeItem(pagesTree, SWT.NONE) :
            new TreeItem(parentItem, SWT.NONE);
        item.setText(page.getTitle());
        item.setData(page);

        // Ad sub pages
        if (page instanceof ICompositeDialogPage) {
            IDialogPage[] subPages = ((ICompositeDialogPage) page).getSubPages();
            if (!ArrayUtils.isEmpty(subPages)) {
                for (IDialogPage subPage : subPages) {
                    addPage(item, subPage, maxSize);
                }
                item.setExpanded(true);
            }
        }

        return item;
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

            GridData gd;
            if (prevPage != null) {
                gd = (GridData) prevPage.getControl().getLayoutData();
                gd.exclude = true;
                prevPage.setVisible(false);
            }

            boolean pageCreated = false;
            IDialogPage page = (IDialogPage) newItem.getData();
            Control pageControl = page.getControl();
            if (pageControl == null) {
                // Create page contents
                page.createControl(pageArea);
                pageControl = page.getControl();
                //Point pageSize = pageControl.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                //if (pageSize.x > maxSize.x) maxSize.x = pageSize.x;
                //if (pageSize.y > maxSize.y) maxSize.y = pageSize.y;
                gd = (GridData) pageControl.getLayoutData();
                if (gd == null) {
                    gd = new GridData(GridData.FILL_BOTH);
                    pageControl.setLayoutData(gd);
                }
                gd.exclude = false;
                pageCreated = true;
            }
            gd = (GridData) pageControl.getLayoutData();
            gd.exclude = false;
            page.setVisible(true);
            setTitle(page.getTitle());
            setMessage(page.getDescription());

            prevPage = page;
            pageArea.layout();
            if (pageCreated) {
                UIUtils.resizeShell(getWizard().getContainer().getShell());
            }
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
            if (!getWizard().performFinish()) {
                return;
            }
        }
        super.buttonPressed(buttonId);
    }

    @Override
    public IWizardPage getCurrentPage()
    {
        TreeItem[] selection = pagesTree.getSelection();
        if (ArrayUtils.isEmpty(selection)) {
            return null;
        }
        IDialogPage page = (IDialogPage)selection[0].getData();
        return page instanceof IWizardPage ? (IWizardPage) page : null;
    }

    @Override
    public void showPage(IWizardPage page)
    {
        for (TreeItem item : pagesTree.getItems()) {
            if (item.getData() == page) {
                pagesTree.setSelection(item);
                changePage();
                break;
            }
            for (TreeItem child : item.getItems()) {
                if (child.getData() == page) {
                    pagesTree.setSelection(child);
                    changePage();
                    return;
                }
            }
        }
    }

    @Override
    public void updateButtons()
    {
        boolean complete = true;
        for (TreeItem item : pagesTree.getItems()) {
            if (item.getData() instanceof IWizardPage) {
                IWizardPage page = (IWizardPage) item.getData();
                if (page.getControl() != null && !page.isPageComplete()) {
                    complete = false;
                    break;
                }
            }
        }
        Button button = getButton(IDialogConstants.OK_ID);
        if (button != null && !button.isDisposed()) {
            button.setEnabled(complete);
        }
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

    public boolean close() {
        if (runningOperations > 0) {
            return false;
        }
        return super.close();
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
        ControlEnableState pageEnableState = ControlEnableState.disable(wizardSash);
        ControlEnableState buttonsEnableState = ControlEnableState.disable(getButtonBar());
        try {
            runningOperations++;
            ModalContext.run(runnable, true, monitorPart, getShell().getDisplay());
        } finally {
            runningOperations--;
            buttonsEnableState.restore();
            pageEnableState.restore();
            if (monitorPart != null) {
                monitorPart.done();
                monitorPart.setVisible(false);
            }
        }
    }

}
