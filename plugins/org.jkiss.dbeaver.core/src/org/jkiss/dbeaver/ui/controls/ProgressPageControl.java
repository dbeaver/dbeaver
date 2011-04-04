/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.ext.ui.ISearchContextProvider;
import org.jkiss.dbeaver.ext.ui.ISearchTextRunner;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.ProxyProgressMonitor;
import org.jkiss.dbeaver.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.ui.UIUtils;

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
    private ProgressPageControl externalPageControl = null;
    private Composite controlComposite;

    private String curInfo;
    private String curSearchText;
    private Color searchColor;

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
        searchColor = new Color(getDisplay(), 255, 128, 128);
    }

    @Override
    public GridLayout getLayout()
    {
        return (GridLayout)super.getLayout();
    }

    public void setInfo(String info)
    {
        this.curInfo = info;
        if (externalPageControl != null) {
            externalPageControl.setInfo(info);
        } else if (!listInfoLabel.isDisposed()) {
            listInfoLabel.setText(info);
        }
    }

    public void restoreState()
    {
        if (curInfo != null) {
            setInfo(curInfo);
        }
        //getProgressControl().hideControls();
    }

    public final Composite createProgressPanel()
    {
        return createProgressPanel(this);
    }

    public final void substituteProgressPanel(ProgressPageControl externalPageControl)
    {
        this.externalPageControl = externalPageControl;
    }

    ProgressPageControl getProgressControl()
    {
        return externalPageControl != null ? externalPageControl : this;
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
        if (this.externalPageControl != null) {
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
        phLabel.setText(" ");

        return customControls;
    }

    private void hideControls()
    {
        for (Control child : controlComposite.getChildren()) {
            child.dispose();
        }
        progressBar = null;
        searchText = null;
    }

    private void createProgressControls()
    {
        if (progressBar != null) {
            return;
        }
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
        searchText.setBackground(searchColor);
        searchText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        searchText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e)
            {
                if (e.character == SWT.ESC) {
                    hideControls();
                }
            }
        });
        searchText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e)
            {
                curSearchText = searchText.getText();
            }
        });

        ToolBar searchTools = new ToolBar(controlComposite, SWT.HORIZONTAL);
        ToolItem searchButton = new ToolItem(searchTools, SWT.PUSH);
        searchButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_FORWARD));
        searchButton.setToolTipText("Search");
        searchButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
            }
        });

        ToolItem closeButton = new ToolItem(searchTools, SWT.PUSH);
        closeButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ELCL_REMOVE));
        closeButton.setToolTipText("Close search panel");
        closeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                hideControls();
            }
        });

        controlComposite.layout();

    }

    @Override
    public void dispose()
    {
        UIUtils.dispose(searchColor);

        super.dispose();
    }

    protected boolean cancelProgress()
    {
        return false;
    }

    protected ISearchTextRunner getSearcher()
    {
        return null;
    }

    public boolean isSearchPossible()
    {
        return getSearcher() != null;
    }

    public boolean isSearchEnabled()
    {
        return getProgressControl().progressBar == null;
    }

    public void performSearch(SearchType searchType)
    {
        getProgressControl().createSearchControls();
        getProgressControl().searchText.setFocus();
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
                getProgressControl().hideControls();
            }
        }

    }

}