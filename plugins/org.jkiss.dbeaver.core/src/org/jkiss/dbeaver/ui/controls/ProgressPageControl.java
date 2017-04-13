/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.ProxyProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;

/**
 * ItemListControl
 */
public class ProgressPageControl extends Composite implements ISearchContextProvider
{
    static final Log log = Log.getLog(ProgressPageControl.class);

    private final static int PROGRESS_MIN = 0;
    private final static int PROGRESS_MAX = 20;

    private boolean showDivider;

    private Text listInfoLabel;

    private ProgressBar progressBar;
    private Text searchText;

    private int loadCount = 0;
    private ProgressPageControl ownerPageControl = null;
    private ProgressPageControl childPageControl = null;
    private Composite searchControlsComposite;

    private String curInfo;
    private String curSearchText;
    private volatile Job curSearchJob;

    private Color searchNotFoundColor;
    private ToolBarManager defaultToolbarManager;
    private ToolBarManager searchToolbarManager;
    private ToolBarManager customToolbarManager;
    private Composite customControlsComposite;

    public ProgressPageControl(
        Composite parent,
        int style)
    {
        super(parent, style);
        GridLayout layout = new GridLayout(1, true);
        if ((style & SWT.SHEET) != 0) {
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            layout.verticalSpacing = 0;
            layout.horizontalSpacing = 0;
        }
        //layout.horizontalSpacing = 0;
        //layout.verticalSpacing = 0;
        this.setLayout(layout);
        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                disposeControl();
            }
        });
        searchNotFoundColor = new Color(getDisplay(), 255, 128, 128);
    }

    @Override
    public GridLayout getLayout()
    {
        return (GridLayout)super.getLayout();
    }

    public void setShowDivider(boolean showDivider)
    {
        this.showDivider = showDivider;
    }

    public void setInfo(String info)
    {
        if (!CommonUtils.isEmpty(info)) {
            this.curInfo = info;
        }
        if (ownerPageControl != null) {
            ownerPageControl.setInfo(info);
        } else if (listInfoLabel != null && !listInfoLabel.isDisposed()) {
            listInfoLabel.setText(info);
        }
    }

    public final Composite createProgressPanel()
    {
        return createProgressPanel(this);
    }

    public final void substituteProgressPanel(ProgressPageControl externalPageControl)
    {
        this.ownerPageControl = externalPageControl;
    }

    private void setChildControl(ProgressPageControl progressPageControl)
    {
        if (progressPageControl == this.childPageControl) {
            return;
        }
//        if (this.childPageControl != null && progressPageControl != null) {
//            log.warn("Overwrite of child page control '" + this.childPageControl); //$NON-NLS-1$
//        }
        this.childPageControl = progressPageControl;
        if (getProgressControl().progressBar == null) {
            hideControls(true);
        }
    }

    ProgressPageControl getProgressControl()
    {
        return ownerPageControl != null ? ownerPageControl : this;
    }

    public Composite createContentContainer()
    {
        Composite container = new Composite(this, (getStyle() & SWT.SHEET) == SWT.SHEET ? SWT.NONE :  SWT.BORDER);
        container.setLayout(new FillLayout());
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalIndent = 0;
        gd.verticalIndent = 0;
        container.setLayoutData(gd);

        return container;
    }

    public Composite createProgressPanel(Composite container)
    {
        if (this.ownerPageControl != null) {
            throw new IllegalStateException("Can't create page control while substitution control already set"); //$NON-NLS-1$
        }
        if (showDivider) {
            Label separator = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
            separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        }
        Composite infoGroup = new Composite(container, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        infoGroup.setLayoutData(gd);
        GridLayout gl = new GridLayout(2, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        infoGroup.setLayout(gl);

        listInfoLabel = new Text(infoGroup, SWT.READ_ONLY);
        //listInfoLabel.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.minimumWidth = 100;
        listInfoLabel.setLayoutData(gd);

        Composite controlsComposite = UIUtils.createPlaceholder(infoGroup, 2, 5);
        controlsComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        searchControlsComposite = UIUtils.createPlaceholder(controlsComposite, 1);
        //gd.heightHint = listInfoLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT, false).y + gl.verticalSpacing;
        searchControlsComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        // Placeholder toolbar (need to set initial height of search composite)
        new ToolBar(searchControlsComposite, SWT.NONE);

        customControlsComposite = new Composite(controlsComposite, SWT.NONE);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
        //gd.verticalIndent = 3;
        customControlsComposite.setLayoutData(gd);
        gl = new GridLayout(2, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        customControlsComposite.setLayout(gl);

        defaultToolbarManager = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL);
        customToolbarManager = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL);

        return customControlsComposite;
    }

    protected void fillCustomToolbar(ToolBarManager toolbarManager)
    {
        if (childPageControl != null) {
            childPageControl.fillCustomToolbar(toolbarManager);
        }
    }

    private void hideControls(boolean showDefaultControls)
    {
        if (searchControlsComposite == null || searchControlsComposite.isDisposed()) {
            return;
        }
        searchControlsComposite.getParent().setRedraw(false);
        try {
            // Delete all controls created in searchControlsComposite
            for (Control child : searchControlsComposite.getChildren()) {
                child.dispose();
            }

            // Nullify all controls
            progressBar = null;
            searchText = null;

            // Create default controls toolbar
            if (showDefaultControls) {
                ((GridLayout)searchControlsComposite.getLayout()).numColumns = 2;
                defaultToolbarManager.removeAll();
                if (isSearchPossible() && isSearchEnabled()) {
                    defaultToolbarManager.add(ActionUtils.makeCommandContribution(
                        PlatformUI.getWorkbench(),
                            IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE,
                            CoreMessages.controls_progress_page_toolbar_title,
                            UIIcon.SEARCH));
                }
                Label phLabel = new Label(searchControlsComposite, SWT.NONE);
                phLabel.setText(""); //$NON-NLS-1$
                phLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                ToolBar defaultToolbar = defaultToolbarManager.createControl(searchControlsComposite);
                defaultToolbar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END));

                // Recreate custom controls
                for (Control child : customControlsComposite.getChildren()) {
                    child.dispose();
                }

                customToolbarManager.removeAll();
                fillCustomToolbar(customToolbarManager);
                if (!customToolbarManager.isEmpty()) {
                    ToolBar toolbar = customToolbarManager.createControl(customControlsComposite);
                    toolbar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END));
                }
            }

            searchControlsComposite.getParent().layout();
            //customControlsComposite.layout();
        } finally {
            searchControlsComposite.getParent().setRedraw(true);
        }
    }

    private void createProgressControls()
    {
        if (progressBar != null) {
            return;
        }
        hideControls(false);
        ((GridLayout)searchControlsComposite.getLayout()).numColumns = 2;
        progressBar = new ProgressBar(searchControlsComposite, SWT.SMOOTH | SWT.HORIZONTAL);
        progressBar.setSize(300, 16);
        progressBar.setState(SWT.NORMAL);
        progressBar.setMinimum(PROGRESS_MIN);
        progressBar.setMaximum(PROGRESS_MAX);
        progressBar.setToolTipText(CoreMessages.controls_progress_page_progress_bar_loading_tooltip);
        progressBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        ToolBar progressTools = new ToolBar(searchControlsComposite, SWT.HORIZONTAL);
        final ToolItem stopButton = new ToolItem(progressTools, SWT.PUSH);
        stopButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ELCL_STOP));
        stopButton.setToolTipText(CoreMessages.controls_progress_page_progress_bar_cancel_tooltip);
        stopButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                // Cancel current job
                if (cancelProgress()) {
                    if (!stopButton.isDisposed()) {
                        stopButton.setEnabled(false);
                        stopButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ELCL_STOP_DISABLED));
                    }
                }
            }
        });
        searchControlsComposite.getParent().layout();
    }

    private void createSearchControls()
    {
        if (searchText != null) {
            return;
        }
        hideControls(false);
        ((GridLayout)searchControlsComposite.getLayout()).numColumns = 2;

        searchText = new Text(searchControlsComposite, SWT.BORDER);
        if (curSearchText != null) {
            searchText.setText(curSearchText);
            searchText.setSelection(curSearchText.length());
        }
        //searchText.setBackground(searchNotFoundColor);
        searchText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        searchText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e)
            {
                if (e.character == SWT.ESC) {
                    cancelSearch(true);
                } else if (e.character == SWT.CR) {
                    performSearch(SearchType.NEXT);
                }
            }
        });
        searchText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e)
            {
                curSearchText = searchText.getText();
                if (curSearchJob == null) {
                    curSearchJob = new UIJob(CoreMessages.controls_progress_page_job_search) {
                        @Override
                        public IStatus runInUIThread(IProgressMonitor monitor)
                        {
                            if (monitor.isCanceled()) {
                                return Status.CANCEL_STATUS;
                            }
                            performSearch(SearchType.NEXT);
                            curSearchJob = null;
                            return Status.OK_STATUS;
                        }
                    };
                    curSearchJob.schedule(200);
                }
            }
        });

        //ToolBar searchTools = new ToolBar(searchControlsComposite, SWT.HORIZONTAL);
        if (searchToolbarManager == null) {
            searchToolbarManager = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL);
            searchToolbarManager.add(ActionUtils.makeCommandContribution(
                    PlatformUI.getWorkbench(),
                    IWorkbenchActionDefinitionIds.FIND_NEXT,
                    null,
                    UIIcon.ARROW_DOWN));
            searchToolbarManager.add(ActionUtils.makeCommandContribution(
                    PlatformUI.getWorkbench(),
                    IWorkbenchActionDefinitionIds.FIND_PREVIOUS,
                    null,
                    UIIcon.ARROW_UP));
            //ToolItem closeButton = new ToolItem(searchTools, SWT.PUSH);
            searchToolbarManager.add(new Action(CoreMessages.controls_progress_page_action_close, PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE)) {
                @Override
                public void run()
                {
                    cancelSearch(true);
                }
            });
        }
        searchToolbarManager.createControl(searchControlsComposite);

        searchControlsComposite.getParent().layout();
    }

    public void disposeControl()
    {
        if (searchToolbarManager != null) {
            searchToolbarManager.dispose();
            searchToolbarManager = null;
        }
        if (defaultToolbarManager != null) {
            defaultToolbarManager.dispose();
            defaultToolbarManager = null;
        }
        if (customToolbarManager != null) {
            customToolbarManager.dispose();
            customToolbarManager = null;
        }
        UIUtils.dispose(searchNotFoundColor);
    }

    protected boolean cancelProgress()
    {
        return false;
    }

    protected ISearchExecutor getSearchRunner()
    {
        if (childPageControl != null) {
            return childPageControl.getSearchRunner();
        }
        return null;
    }

    @Override
    public boolean isSearchPossible()
    {
        return getSearchRunner() != null;
    }

    @Override
    public boolean isSearchEnabled()
    {
        return getProgressControl().progressBar == null;
    }

    @Override
    public boolean performSearch(SearchType searchType)
    {
        getProgressControl().createSearchControls();
        if (searchType == SearchType.NONE) {
            getProgressControl().searchText.setFocus();
        }
        if (!CommonUtils.isEmpty(getProgressControl().curSearchText)) {
            int options = 0;
            if (searchType == SearchType.PREVIOUS) {
                options |= ISearchExecutor.SEARCH_PREVIOUS;
            } else {
                options |= ISearchExecutor.SEARCH_NEXT;
            }
            boolean success = getSearchRunner().performSearch(getProgressControl().curSearchText, options);
            getProgressControl().searchText.setBackground(success ? null : searchNotFoundColor);
            return success;
        } else {
            cancelSearch(false);
            return true;
        }
    }

    private void cancelSearch(boolean hide)
    {
        if (curSearchJob != null) {
            curSearchJob.cancel();
            curSearchJob = null;
        }
        getSearchRunner().cancelSearch();

        if (hide) {
            hideControls(true);
        } else {
            getProgressControl().searchText.setBackground(null);
        }
    }

    public void activate(boolean active)
    {
        if (active && curInfo != null) {
            setInfo(curInfo);
        }

        if (this.ownerPageControl != null) {
            if (active) {
                this.ownerPageControl.setChildControl(this);
            } else {
                // Do NOT set child to NULL because deactivation usually means just focus lost
                // and we don't want to deactivate page control on focus loss.
                //this.ownerPageControl.setChildControl(null);
            }
        }
    }

/*
    public void run(boolean fork, boolean cancelable, final IRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException
    {
        Job job = new Job("Progress") {
            @Override
            protected IStatus run(IProgressMonitor monitor)
            {
                try {
                    runnable.run(monitor);
                } catch (InvocationTargetException e) {
                    return RuntimeUtils.makeExceptionStatus(e.getTargetException());
                } catch (InterruptedException e) {
                    // do nothing
                }
                return Status.OK_STATUS;
            }
        };
        job.schedule();
        job.join();
    }
*/

    private static class TaskInfo {
        final String name;
        final int totalWork;
        int progress;

        private TaskInfo(String name, int totalWork)
        {
            this.name = name;
            this.totalWork = totalWork;
        }
    }

    public class ProgressVisualizer<RESULT> implements ILoadVisualizer<RESULT> {

        private boolean completed = false;
        private String curStatus;
        private final java.util.List<TaskInfo> tasksRunning = new ArrayList<>();

        @Override
        public DBRProgressMonitor overwriteMonitor(final DBRProgressMonitor monitor)
        {
            return new ProxyProgressMonitor(monitor) {
                @Override
                public void beginTask(final String name, int totalWork)
                {
                    super.beginTask(name, totalWork);
                    curStatus = name;
                    synchronized (tasksRunning) {
                        tasksRunning.add(new TaskInfo(name, totalWork));
                    }
                }

                @Override
                public void done()
                {
                    super.done();
                    curStatus = ""; //$NON-NLS-1$
                    synchronized (tasksRunning) {
                        if (tasksRunning.isEmpty()) {
                            log.warn("Task end when no tasks are running"); //$NON-NLS-1$
                        } else {
                            tasksRunning.remove(tasksRunning.size() - 1);
                        }
                    }
                }

                @Override
                public void subTask(String name)
                {
                    super.subTask(name);
                    curStatus = name;
                }

                @Override
                public void worked(int work)
                {
                    super.worked(work);
                    synchronized (tasksRunning) {
                        if (!tasksRunning.isEmpty()) {
                            tasksRunning.get(tasksRunning.size() - 1).progress += work;
                        }
                    }
                }
            };
        }

        private TaskInfo getCurTaskInfo()
        {
            for (int i = tasksRunning.size() - 1; i >= 0; i--) {
                if (tasksRunning.get(i).totalWork > 1) {
                    return tasksRunning.get(i);
                }
            }
            return null;
        }

        @Override
        public boolean isCompleted()
        {
            return completed;
        }

        @Override
        public void visualizeLoading()
        {
            if (!getProgressControl().isDisposed()) {
                getProgressControl().createProgressControls();
                synchronized (tasksRunning) {
                    TaskInfo taskInfo = getCurTaskInfo();
                    if (taskInfo != null) {
                        getProgressControl().progressBar.setMaximum(taskInfo.totalWork);
                        getProgressControl().progressBar.setSelection(taskInfo.progress);
                    } else {
                        getProgressControl().progressBar.setMaximum(PROGRESS_MAX);
                        getProgressControl().progressBar.setSelection(loadCount);
                    }
                }
                if (curStatus != null) {
                    setInfo(curStatus);
                }
                loadCount++;
                if (loadCount > PROGRESS_MAX) {
                    loadCount = PROGRESS_MIN;
                }
            }
        }

        @Override
        public void completeLoading(RESULT result)
        {
            completed = true;

            if (ProgressPageControl.this.isDisposed()) {
                return;
            }
            visualizeLoading();
            loadCount = 0;
            if (!getProgressControl().progressBar.isDisposed()) {
                getProgressControl().progressBar.setState(SWT.PAUSED);
                getProgressControl().hideControls(true);
            }
        }

    }

}