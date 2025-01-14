/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.jface.preference.IPreferencePageContainer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWizard;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.preferences.PreferenceStoreDelegate;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * MultiPageWizardDialog
 */
public class MultiPageWizardDialog extends TitleAreaDialog implements IWizardContainer, IWizardContainer2, IPageChangeProvider, IPreferencePageContainer {

    protected enum PageCompletionMark {
        /** If a page is complete, a green check will be shown next to it */
        COMPLETE,
        /** If a page is incomplete, a red cross will be shown next to it */
        ERROR
    }

    private IWizard wizard;
    private Composite pageArea;
    private Tree pagesTree;
    private IDialogPage prevPage;

    private ProgressMonitorPart monitorPart;
    private SashForm wizardSash;
    private volatile int runningOperations = 0;

    private String finishButtonLabel = IDialogConstants.OK_LABEL;
    private String cancelButtonLabel = IDialogConstants.CANCEL_LABEL;

    private final ListenerList<IPageChangedListener> pageChangedListeners = new ListenerList<>();
    private Composite leftBottomPanel;
    private final Set<IWizardPage> resizedPages = new HashSet<>();

    public MultiPageWizardDialog(IWorkbenchWindow window, IWizard wizard) {
        this(window, wizard, null);
    }

    public MultiPageWizardDialog(IWorkbenchWindow window, IWizard wizard, IStructuredSelection selection) {
        super(window.getShell());

        this.wizard = wizard;
        this.wizard.setContainer(this);

        // Initialize wizard
        if (wizard instanceof IWorkbenchWizard) {
            if (selection == null) {
                if (window.getSelectionService().getSelection() instanceof IStructuredSelection) {
                    selection = (IStructuredSelection) window.getSelectionService().getSelection();
                }
            }
            ((IWorkbenchWizard) wizard).init(window.getWorkbench(), selection);
        }
    }

    protected Point getInitialSize() {
        Point minSize = new Point(700, 500);
        Point suggestedSize = super.getInitialSize();
        return new Point(Math.max(minSize.x, suggestedSize.x), Math.max(minSize.y, suggestedSize.y));
    }

    public IWizard getWizard() {
        return wizard;
    }

    public void setWizard(IWizard newWizard) {
        this.wizard = newWizard;
        this.wizard.setContainer(this);
        UIUtils.disposeChildControls(leftBottomPanel);
        createBottomLeftArea(leftBottomPanel);
        leftBottomPanel.getParent().layout(true, true);
        updateNavigationTree();
        updateButtons();
    }

    @NotNull
    protected EnumSet<PageCompletionMark> getShownCompletionMarks() {
        return EnumSet.of(PageCompletionMark.ERROR);
    }

    protected Tree getPagesTree() {
        return pagesTree;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    public int getShellStyle() {
        if (isModalWizard() || UIUtils.isInDialog()) {
            return SWT.TITLE | SWT.MAX | SWT.RESIZE | SWT.APPLICATION_MODAL;
        }
        return SWT.CLOSE | SWT.MAX | SWT.TITLE | SWT.BORDER | SWT.RESIZE | getDefaultOrientation();
    }

    protected boolean isModalWizard() {
        return true;
    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);
        applyDialogFont(contents);

        // Show the first page first - it may initialize state required later
        showPage((IWizardPage) pagesTree.getItem(0).getData());

        // Set title and image from first page
        IWizardPage firstPage = getStartingPage();
        setTitle(firstPage.getTitle());
        setTitleImage(firstPage.getImage());
        setMessage(firstPage.getDescription());

        // Afterwards show the starting page
        showPage(firstPage);

        updateButtons();

        updateWindowTitle();

        return contents;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        wizard.addPages();

        wizardSash = new SashForm(composite, SWT.HORIZONTAL);
        wizardSash.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite leftPane = UIUtils.createComposite(wizardSash, 1);
        pagesTree = new Tree(leftPane, SWT.SINGLE);
        pagesTree.setLayoutData(new GridData(GridData.FILL_BOTH));

        leftPane.setBackground(pagesTree.getBackground());
        leftBottomPanel = UIUtils.createComposite(leftPane, 1);
        leftBottomPanel.setBackground(pagesTree.getBackground());
        createBottomLeftArea(leftBottomPanel);

        Composite pageContainer = UIUtils.createPlaceholder(wizardSash, 2);

        // Vertical separator
        new Label(pageContainer, SWT.SEPARATOR | SWT.VERTICAL)
            .setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true));
        pageArea = UIUtils.createPlaceholder(pageContainer, 1);
        GridData gd = new GridData(GridData.FILL_BOTH);
        pageArea.setLayoutData(gd);
        pageArea.setLayout(new GridLayout(1, true));

        wizardSash.setWeights(220, 780);

        Point size = leftPane.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        if (size.x > 0) {
            ((GridData) wizardSash.getLayoutData()).widthHint = size.x * 6;
        }

        updateNavigationTree();

        pagesTree.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                TreeItem[] selection = pagesTree.getSelection();
                if (selection.length > 0) {
                    Object newPage = selection[0].getData();
                    // If we are in long operation or target page is not navigable - flip back
                    if (runningOperations > 0 ||
                        (newPage instanceof IWizardPageNavigable && !((IWizardPageNavigable) newPage).isPageNavigable()))
                    {
                        if (prevPage != null) {
                            TreeItem prevItem = UIUtils.getTreeItem(pagesTree, prevPage);
                            if (prevItem != null) {
                                pagesTree.select(prevItem);
                            }
                        }
                        return;
                    }
                }
                changePage();
            }
        });

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

    protected void createBottomLeftArea(Composite pane) {

    }

    protected void cancelCurrentOperation() {

    }

    private void changePage() {
        TreeItem[] selection = pagesTree.getSelection();
        if (selection.length != 1) {
            return;
        }
        TreeItem newItem = selection[0];
        if (prevPage == newItem.getData()) {
            return;
        }

        pageArea.setRedraw(false);
        IWizard wizard = getWizard();
        try {
            GridData gd;
            if (prevPage != null && prevPage.getControl() != null) {
                gd = (GridData) prevPage.getControl().getLayoutData();
                gd.exclude = true;
                prevPage.setVisible(false);
                if (prevPage instanceof ActiveWizardPage<?> awp) {
                    awp.deactivatePage();
                }
            }

            boolean pageCreated = false;
            IDialogPage page = (IDialogPage) newItem.getData();
            Control pageControl = page.getControl();
            if (pageControl == null) {
                // Create page contents
                page.createControl(pageArea);
                pageControl = page.getControl();
                applyDialogFont(pageControl);

                if (pageControl != null) {
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
            }
            if (pageControl != null) {
                gd = (GridData) pageControl.getLayoutData();
                gd.exclude = false;
                page.setVisible(true);
            }

            setTitle(page.getTitle());
            setMessage(page.getDescription());

            prevPage = page;
            pageArea.layout();
            if (prevPage.getControl() != null) {
                prevPage.getControl().setFocus();
            }

            if (page instanceof ActiveWizardPage<?> awp) {
                awp.updatePageCompletion();
            }
        } catch (Throwable e) {
            DBWorkbench.getPlatformUI().showError("Page switch", "Error switching active page", e);
        } finally {
            pageArea.setRedraw(true);
        }

        firePageChanged(new PageChangedEvent(this, getCurrentPage()));
        updatePageCompleteMark(null);
    }

    protected boolean isAutoLayoutAvailable() {
        return true;
    }

    public void disableButtonsOnProgress() {
        getButton(IDialogConstants.OK_ID).setEnabled(false);
        getButton(IDialogConstants.CANCEL_ID).setEnabled(false);
    }

    public void enableButtonsAfterProgress() {
        getButton(IDialogConstants.OK_ID).setEnabled(true);
        getButton(IDialogConstants.CANCEL_ID).setEnabled(true);
    }

    public void setCompleteMarkAfterProgress() {
        TreeItem[] selection = pagesTree.getSelection();
        if (selection.length != 1) {
            return;
        }
        TreeItem selectedItem = selection[0];
        selectedItem.setImage(DBeaverIcons.getImage(UIIcon.OK_MARK));
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.CANCEL_ID) {
            getWizard().performCancel();
        } else if (buttonId == IDialogConstants.OK_ID) {
            if (!getWizard().performFinish()) {
                return;
            }
        } else if (buttonId == IDialogConstants.FINISH_ID) {
            finishPressed();
        } else if (buttonId == IDialogConstants.NEXT_ID) {
            nextPressed();
        } else if (buttonId == IDialogConstants.BACK_ID) {
            backPressed();
        }
        super.buttonPressed(buttonId);
    }

    @Override
    public IWizardPage getCurrentPage() {
        TreeItem[] selection = pagesTree.getSelection();
        if (ArrayUtils.isEmpty(selection)) {
            return null;
        }
        IDialogPage page = (IDialogPage) selection[0].getData();
        return page instanceof IWizardPage ? (IWizardPage) page : null;
    }

    @Override
    public void showPage(IWizardPage page) {
        for (TreeItem item : pagesTree.getItems()) {
            if (item.getData() == page) {
                pagesTree.setSelection(item);
                changePage();
                return;
            }
            for (TreeItem child : item.getItems()) {
                if (child.getData() == page) {
                    pagesTree.setSelection(child);
                    changePage();
                    return;
                }
            }
        }
        for (TreeItem item : pagesTree.getItems()) {
            if (item.getData() instanceof ICompositeDialogPageContainer) {
                IDialogPage[] subPages = ((ICompositeDialogPageContainer) item.getData()).getDialogPages(false, false);
                if (!ArrayUtils.isEmpty(subPages)) {
                    for (IDialogPage subPage : subPages) {
                        if (subPage == page) {
                            pagesTree.setSelection(item);
                            changePage();
                            ((ICompositeDialogPageContainer) item.getData()).showSubPage(page);
                            return;
                        }
                    }
                }
            }

        }
    }

    @Override
    public IPreferenceStore getPreferenceStore() {
        return new PreferenceStoreDelegate(DBWorkbench.getPlatform().getPreferenceStore());
    }

    public void updateNavigationTree() {
        pagesTree.setRedraw(false);
        try {
            Object selectedPage = getSelectedPage();

            pagesTree.removeAll();

            IWizardPage[] pages = wizard.getPages();
            for (IWizardPage page : pages) {
                addNavigationItem(null, page);
            }

            for (TreeItem item : pagesTree.getItems()) {
                if (item.getData() == selectedPage) {
                    pagesTree.select(item);
                    break;
                }
            }

            updatePageCompleteMark(null);

        } finally {
            pagesTree.setRedraw(true);
        }
    }

    private void updatePageCompleteMark(TreeItem parent) {
        final EnumSet<PageCompletionMark> shownCompletionMarks = getShownCompletionMarks();
        final IWizardPage currentPage = getCurrentPage();
        for (TreeItem item : parent == null ? pagesTree.getItems() : parent.getItems()) {
            Object page = item.getData();
            if (page instanceof IWizardPageNavigable pageNavigable && !pageNavigable.isPageNavigable()) {
                continue;
            }
            if (page == currentPage) {
                // Don't show any completion marks for current page
                item.setImage((Image) null);
            } else if (page instanceof IWizardPage wizardPage && !wizardPage.isPageComplete()) {
                item.setImage(shownCompletionMarks.contains(PageCompletionMark.ERROR) ? DBeaverIcons.getImage(DBIcon.SMALL_ERROR) : null);
            } else {
                item.setImage(shownCompletionMarks.contains(PageCompletionMark.COMPLETE) ? DBeaverIcons.getImage(UIIcon.OK_MARK) : null);
            }
            updatePageCompleteMark(item);
        }
    }

    private TreeItem addNavigationItem(TreeItem parentItem, IDialogPage page) {
        if (page instanceof IWizardPageNavigable && !((IWizardPageNavigable) page).isPageApplicable()) {
            return null;
        }
        TreeItem item = parentItem == null ?
            new TreeItem(pagesTree, SWT.NONE) :
            new TreeItem(parentItem, SWT.NONE);
        item.setText(CommonUtils.toString(page.getTitle(), page.getClass().getSimpleName()));
        if (page instanceof IWizardPageNavigable && !((IWizardPageNavigable) page).isPageNavigable()) {
            int nnColor = UIStyles.isDarkTheme() ?
                SWT.COLOR_WIDGET_NORMAL_SHADOW : SWT.COLOR_WIDGET_DARK_SHADOW;
            item.setForeground(getShell().getDisplay().getSystemColor(nnColor));
        }

        item.setData(page);

        // Ad sub pages
        if (page instanceof IDialogPageProvider) {
            IDialogPage[] subPages = ((IDialogPageProvider) page).getDialogPages(true, resizeHasOccurred);
            if (!ArrayUtils.isEmpty(subPages)) {
                for (IDialogPage subPage : subPages) {
                    addNavigationItem(item, subPage);
                }
                //item.setExpanded(true);
            }
        }

        return item;
    }

    protected void updatePageCompletion() {
        IWizardPage page = getCurrentPage();
        if (page instanceof ActiveWizardPage<?> awp) {
            awp.updatePageCompletion();
        }
    }

    @Override
    public void updateButtons() {
        Button finishButton = getButton(IDialogConstants.OK_ID);
        if (finishButton != null && !finishButton.isDisposed()) {
            finishButton.setEnabled(wizard.canFinish());
        }

        IWizardPage currentPage = getCurrentPage();
        if (currentPage != null) {
            Button nextButton = getButton(IDialogConstants.NEXT_ID);
            if (nextButton != null) {
                nextButton.setEnabled(getCurrentPage().isPageComplete() && wizard.getNextPage(currentPage) != null);
            }
            Button prevButton = getButton(IDialogConstants.BACK_ID);
            if (prevButton != null) {
                prevButton.setEnabled(wizard.getPreviousPage(currentPage) != null);
            }
        }
        updatePageCompleteMark(null);
    }

    @Override
    public void updateMessage() {
        IWizardPage currentPage = getCurrentPage();
        if (currentPage == null) {
            return;
        }
        final String message = currentPage.getMessage();
        if (message == null) {
            setMessage(currentPage.getDescription());
        } else if (currentPage instanceof IMessageProvider provider) {
            setMessage(message, provider.getMessageType());
        } else {
            setMessage(message);
        }
        setErrorMessage(currentPage.getErrorMessage());
    }

    @Override
    public void updateTitle() {
        updateWindowTitle();
    }

    @Override
    public void updateTitleBar() {
        //setTitleImage(getCurrentPage().getImage());
    }

    @Override
    public void updateWindowTitle() {
        Shell shell = getShell();
        if (shell != null) {
            shell.setText(getWizard().getWindowTitle());
            // Do not update dialog icon. It can be disposed in the page and this will break connection dialog
            //getShell().setImage(getWizard().getDefaultPageImage());//DBeaverIcons.getImage(activeDataSource.getObjectImage()));

            updateMessage();
        }
    }

    public boolean close() {
        if (runningOperations > 0) {
            return false;
        }
        if (wizard != null) {
            wizard.dispose();
        }
        return super.close();
    }

    @Override
    public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException {
        // Code copied from WizardDialog
        if (monitorPart != null) {
            monitorPart.setVisible(true);
            //monitorPart.layout();
            monitorPart.attachToCancelComponent(null);
        }
        boolean isDisableControlsOnRun = isDisableControlsOnRun();
        ControlEnableState pageEnableState = isDisableControlsOnRun ? ControlEnableState.disable(wizardSash) : null;
        ControlEnableState buttonsEnableState = isDisableControlsOnRun ? ControlEnableState.disable(getButtonBar()) : null;
        try {
            runningOperations++;
            ModalContext.run(runnable, true, monitorPart, getShell().getDisplay());
        } finally {
            runningOperations--;
            if (buttonsEnableState != null) {
                buttonsEnableState.restore();
            }
            if (pageEnableState != null) {
                pageEnableState.restore();
            }
            if (monitorPart != null && !monitorPart.isDisposed()) {
                monitorPart.done();
                monitorPart.setVisible(false);
            }
        }
    }

    protected boolean isDisableControlsOnRun() {
        return false;
    }

    protected void showPage(String pageName) {
        for (IWizardPage page : getWizard().getPages()) {
            if (pageName.equals(page.getName())) {
                showPage(page);
                break;
            }
        }
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, finishButtonLabel,
            getShell().getDefaultButton() == null);
        createButton(parent, IDialogConstants.CANCEL_ID, cancelButtonLabel, false);
    }

    protected void setFinishButtonLabel(String finishButtonLabel) {
        this.finishButtonLabel = finishButtonLabel;
    }

    protected void setCancelButtonLabel(String cancelButtonLabel) {
        this.cancelButtonLabel = cancelButtonLabel;
    }

    @Override
    public Object getSelectedPage() {
        return getCurrentPage();
    }

    @NotNull
    protected IWizardPage getStartingPage() {
        return (IWizardPage) pagesTree.getItem(0).getData();
    }

    protected void finishPressed() {
 
    }

    public void nextPressed() {
        IWizard wizard = getWizard();
        IWizardPage currentPage = getCurrentPage();
        if (!currentPage.isPageComplete()) {
            return;
        }
        IWizardPage nextPage = wizard.getNextPage(currentPage);
        if (nextPage != null) {
            showPage(nextPage);
        }
    }

    public void backPressed() {
        IWizard wizard = getWizard();
        IWizardPage currentPage = getCurrentPage();
        IWizardPage prevPage = wizard.getPreviousPage(currentPage);
        if (prevPage != null) {
            showPage(prevPage);
        }
    }

    public void addPageChangedListener(IPageChangedListener listener) {
        pageChangedListeners.add(listener);
    }

    public void removePageChangedListener(IPageChangedListener listener) {
        pageChangedListeners.remove(listener);
    }

    private void firePageChanged(final PageChangedEvent event) {
        for (IPageChangedListener l : pageChangedListeners) {
            SafeRunnable.run(new SafeRunnable() {
                @Override
                public void run() {
                    l.pageChanged(event);
                }
            });
        }
        updateSize();
    }

    @Override
    public void updateSize() {
        updateSize(getCurrentPage());
    }

    private void updateSize(IWizardPage page) {
        if (page == null || page.getControl() == null || resizedPages.contains(page)) {
            return;
        }
        updateSizeForPage(page);
        pageArea.layout();
        resizedPages.add(page);
    }

    /**
     * Computes the correct dialog size for the given page and resizes its shell if necessary.
     *
     * @param page the wizard page
     */
    private void updateSizeForPage(IWizardPage page) {
        if (isAutoLayoutAvailable() &&
            (!(page instanceof  ActiveWizardPage<?> awp) || awp.isAutoResizeEnabled())) {
            UIUtils.asyncExec(() -> {
                Point pageCompSize = page.getControl().computeSize(SWT.DEFAULT, SWT.DEFAULT);
                for (Control parent = page.getControl().getParent(); parent != null; parent = parent.getParent()) {
                    if (parent instanceof SashForm) {
                        pageCompSize = parent.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                        break;
                    }
                }
                Point shellCompSize = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);
                if (shellCompSize.y > pageCompSize.y) {
                    pageCompSize.y = shellCompSize.y;
                }
                UIUtils.resizeShell(
                    getShell(),
                    pageCompSize);
            });
        }
    }

    /**
     * Computes the correct dialog size for the given wizard and resizes its shell if necessary.
     *
     * @param sizingWizard the wizard
     */
    private void updateSizeForWizard(IWizard sizingWizard) {
        Point delta = new Point(0, 0);
        IWizardPage[] pages = sizingWizard.getPages();
        for (IWizardPage page : pages) {
            // ensure the page container is large enough
            Point pageDelta = calculatePageSizeDelta(page);
            delta.x = Math.max(delta.x, pageDelta.x);
            delta.y = Math.max(delta.y, pageDelta.y);
        }
        if (delta.x > 0 || delta.y > 0) {
            // increase the size of the shell
            Shell shell = getShell();
            Point shellSize = shell.getSize();
            setShellSize(shellSize.x + delta.x, shellSize.y + delta.y);
        }
    }

    private Point calculatePageSizeDelta(IWizardPage page) {
        Control pageControl = page.getControl();
        if (pageControl == null) {
            // control not created yet
            return new Point(0, 0);
        }
        Point contentSize = pageControl.computeSize(SWT.DEFAULT, SWT.DEFAULT,
            true);
        Rectangle rect = pageArea.getClientArea();
        Point containerSize = new Point(rect.width, rect.height);
        return new Point(Math.max(0, contentSize.x - containerSize.x), Math
            .max(0, contentSize.y - containerSize.y));
    }

    private void setShellSize(int width, int height) {
        Rectangle size = getShell().getBounds();
        size.height = height;
        size.width = width;
        getShell().setBounds(getConstrainedShellBounds(size));
    }

}
