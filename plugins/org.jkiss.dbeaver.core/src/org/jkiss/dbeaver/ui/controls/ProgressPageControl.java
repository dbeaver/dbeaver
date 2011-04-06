/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.ui.ISearchContextProvider;
import org.jkiss.dbeaver.ext.ui.ISearchExecutor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.ProxyProgressMonitor;
import org.jkiss.dbeaver.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.ViewUtils;

import java.util.ArrayList;

/**
 * ItemListControl
 */
public class ProgressPageControl extends Composite implements ISearchContextProvider
{
    static final Log log = LogFactory.getLog(ProgressPageControl.class);

    private final static int PROGRESS_MIN = 0;
    private final static int PROGRESS_MAX = 20;

    private Text listInfoLabel;

    private ProgressBar progressBar;
    private Text searchText;

    private int loadCount = 0;
    private ProgressPageControl ownerPageControl = null;
    private ProgressPageControl childPageControl = null;
    private Composite controlComposite;

    private String curInfo;
    private String curSearchText;
    private volatile Job curSearchJob;

    private Color searchNotFoundColor;

    public ProgressPageControl(
        Composite parent,
        int style)
    {
        super(parent, style);
        GridLayout layout = new GridLayout(1, true);
        if ((style & SWT.SHEET) != 0) {
            layout.marginHeight = 0;
            layout.marginWidth = 0;
        }
        //layout.horizontalSpacing = 0;
        //layout.verticalSpacing = 0;
        this.setLayout(layout);
        addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e)
            {
                dispose();
            }
        });
        searchNotFoundColor = new Color(getDisplay(), 255, 128, 128);
    }

    @Override
    public GridLayout getLayout()
    {
        return (GridLayout)super.getLayout();
    }

    public void setInfo(String info)
    {
        this.curInfo = info;
        if (ownerPageControl != null) {
            ownerPageControl.setInfo(info);
        } else if (!listInfoLabel.isDisposed()) {
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
        if (this.childPageControl != null && progressPageControl != null) {
            log.warn("Overwrite of child page control '" + this.childPageControl);
        }
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
        Composite container = new Composite(this, SWT.BORDER);
        container.setLayout(new FillLayout());
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalIndent = 0;
        gd.verticalIndent = 0;
        container.setLayoutData(gd);

        return container;
    }

    protected Composite createProgressPanel(Composite container)
    {
        if (this.ownerPageControl != null) {
            throw new IllegalStateException("Can't create page control while substitution control already set");
        }
        Composite infoGroup = new Composite(container, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        infoGroup.setLayoutData(gd);
        GridLayout gl = new GridLayout(4, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        infoGroup.setLayout(gl);

        listInfoLabel = new Text(infoGroup, SWT.READ_ONLY);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.minimumWidth = 100;
        listInfoLabel.setLayoutData(gd);

        controlComposite = new Composite(infoGroup, SWT.NONE);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        //gd.heightHint = listInfoLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT, false).y + gl.verticalSpacing;
        controlComposite.setLayoutData(gd);
        controlComposite.setLayout(new GridLayout(1, false));
        Label ctrlLabel = new Label(controlComposite, SWT.NONE);
        ctrlLabel.setText(" ");

        Composite customControls = new Composite(infoGroup, SWT.NONE);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
        customControls.setLayoutData(gd);
        gl = new GridLayout(2, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        customControls.setLayout(gl);

        Label phLabel = new Label(customControls, SWT.NONE);
        phLabel.setText("");

        return customControls;
    }

    private void hideControls(boolean showDefaultControls)
    {
        if (controlComposite.isDisposed()) {
            return;
        }
        controlComposite.setRedraw(false);
        try {
// Delete all controls created in controlComposite
            if (!controlComposite.isDisposed()) {
                for (Control child : controlComposite.getChildren()) {
                    child.dispose();
                }
            }

            // Nullify all controls
            progressBar = null;
            searchText = null;

                // Create default controls toolbar
            if (showDefaultControls) {
                GridLayout layout = new GridLayout(1, false);
                layout.marginHeight = 0;
                layout.marginWidth = 0;
                layout.verticalSpacing = 0;
                controlComposite.setLayout(layout);

                ToolBarManager searchTools = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL);
                if (isSearchPossible() && isSearchEnabled()) {
                    searchTools.add(ViewUtils.makeCommandContribution(
                        DBeaverCore.getInstance().getWorkbench(),
                        IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE,
                        "Search item(s)",
                        DBIcon.SEARCH.getImageDescriptor()));
                }
                ToolBar toolbar = searchTools.createControl(controlComposite);
                toolbar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END));
                controlComposite.layout();
            }
        } finally {
            controlComposite.setRedraw(true);
        }
    }

    private void createProgressControls()
    {
        if (progressBar != null) {
            return;
        }
        hideControls(false);
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.verticalSpacing = 0;
        controlComposite.setLayout(layout);
        progressBar = new ProgressBar(controlComposite, SWT.SMOOTH | SWT.HORIZONTAL);
        progressBar.setSize(300, 16);
        progressBar.setState(SWT.NORMAL);
        progressBar.setMinimum(PROGRESS_MIN);
        progressBar.setMaximum(PROGRESS_MAX);
        progressBar.setToolTipText("Loading progress");
        progressBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        ToolBar progressTools = new ToolBar(controlComposite, SWT.HORIZONTAL);
        final ToolItem stopButton = new ToolItem(progressTools, SWT.PUSH);
        stopButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ELCL_STOP));
        stopButton.setToolTipText("Cancel current operation");
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
        controlComposite.layout();
    }

    private void createSearchControls()
    {
        if (searchText != null) {
            return;
        }
        hideControls(false);
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.verticalSpacing = 0;
        controlComposite.setLayout(layout);

        searchText = new Text(controlComposite, SWT.BORDER);
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
            public void modifyText(ModifyEvent e)
            {
                curSearchText = searchText.getText();
                if (curSearchJob == null) {
                    curSearchJob = new UIJob("Search") {
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

        //ToolBar searchTools = new ToolBar(controlComposite, SWT.HORIZONTAL);
        ToolBarManager searchTools = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL);
        searchTools.add(ViewUtils.makeCommandContribution(
            DBeaverCore.getInstance().getWorkbench(),
            IWorkbenchActionDefinitionIds.FIND_NEXT,
            null,
            DBIcon.ARROW_DOWN.getImageDescriptor()));
        searchTools.add(ViewUtils.makeCommandContribution(
            DBeaverCore.getInstance().getWorkbench(),
            IWorkbenchActionDefinitionIds.FIND_PREVIOUS,
            null,
            DBIcon.ARROW_UP.getImageDescriptor()));
        //ToolItem closeButton = new ToolItem(searchTools, SWT.PUSH);
        searchTools.add(new Action("Close search panel", PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE)) {
            @Override
            public void run()
            {
                cancelSearch(true);
            }
        });
        searchTools.createControl(controlComposite);


        controlComposite.layout();

    }

    @Override
    public void dispose()
    {
        UIUtils.dispose(searchNotFoundColor);

        super.dispose();
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

    public boolean isSearchPossible()
    {
        return getSearchRunner() != null;
    }

    public boolean isSearchEnabled()
    {
        return getProgressControl().progressBar == null;
    }

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
        if (curInfo != null) {
            setInfo(curInfo);
        }

        if (this.ownerPageControl != null) {
            if (active) {
                this.ownerPageControl.setChildControl(this);
            } else {
                this.ownerPageControl.setChildControl(null);
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
        private final java.util.List<TaskInfo> tasksRunning = new ArrayList<TaskInfo>();

        public Shell getShell() {
            return getProgressControl().isDisposed() ? null : getProgressControl().getShell();
        }

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
                    curStatus = "";
                    synchronized (tasksRunning) {
                        if (tasksRunning.isEmpty()) {
                            log.warn("Task end when no tasks are running");
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

        public boolean isCompleted()
        {
            return completed;
        }

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