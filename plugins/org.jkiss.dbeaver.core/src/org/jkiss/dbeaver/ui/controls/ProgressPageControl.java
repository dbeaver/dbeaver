/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.ProxyProgressMonitor;
import org.jkiss.dbeaver.runtime.load.ILoadVisualizer;

import java.util.ArrayList;

/**
 * ItemListControl
 */
public class ProgressPageControl extends Composite //implements IRunnableContext
{
    static final Log log = LogFactory.getLog(ProgressPageControl.class);

    private final static int PROGRESS_MIN = 0;
    private final static int PROGRESS_MAX = 20;

    private ProgressBar progressBar;
    private ToolBar progressTools;
    private ToolItem stopButton;
    private Text listInfoLabel;

    private int loadCount = 0;

    public ProgressPageControl(
        Composite parent,
        int style)
    {
        super(parent, style);
        GridLayout layout = new GridLayout(1, true);
        //layout.marginHeight = 0;
        //layout.marginWidth = 0;
        //layout.horizontalSpacing = 0;
        //layout.verticalSpacing = 0;
        this.setLayout(layout);
        addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e)
            {
                dispose();
            }
        });
    }

    public void setInfo(String info)
    {
        if (!listInfoLabel.isDisposed()) {
            listInfoLabel.setText(info);
        }
    }

    public final Composite createProgressPanel()
    {
        return createProgressPanel(this);
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

        progressBar = new ProgressBar(infoGroup, SWT.SMOOTH | SWT.HORIZONTAL);
        progressBar.setSize(300, 16);
        progressBar.setState(SWT.NORMAL);
        progressBar.setMinimum(PROGRESS_MIN);
        progressBar.setMaximum(PROGRESS_MAX);
        progressBar.setToolTipText("Loading progress");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        progressBar.setLayoutData(gd);

        progressTools = new ToolBar(infoGroup, SWT.HORIZONTAL);
        stopButton = new ToolItem(progressTools, SWT.PUSH);
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

        progressBar.setVisible(false);
        progressTools.setVisible(false);

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

    @Override
    public void dispose()
    {
        super.dispose();
    }

    protected boolean cancelProgress()
    {
        return false;
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
            return ProgressPageControl.this.isDisposed() ? null : ProgressPageControl.this.getShell();
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
            if (!progressBar.isDisposed()) {
                if (!progressBar.isVisible()) {
                    progressBar.setVisible(true);
                    stopButton.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ELCL_STOP));
                    stopButton.setEnabled(true);
                    progressTools.setVisible(true);
                }
                synchronized (tasksRunning) {
                    TaskInfo taskInfo = getCurTaskInfo();
                    if (taskInfo != null) {
                        progressBar.setMaximum(taskInfo.totalWork);
                        progressBar.setSelection(taskInfo.progress);
                    } else {
                        progressBar.setMaximum(PROGRESS_MAX);
                        progressBar.setSelection(loadCount);
                    }
                }
                if (curStatus != null) {
                    listInfoLabel.setText(curStatus);
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
            if (!progressBar.isDisposed()) {
                progressBar.setState(SWT.PAUSED);
                progressBar.setVisible(false);
                progressTools.setVisible(false);
            }
        }

    }

}