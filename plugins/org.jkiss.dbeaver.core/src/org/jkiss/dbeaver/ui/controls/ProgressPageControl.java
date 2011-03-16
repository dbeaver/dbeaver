/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.runtime.ProxyProgressMonitor;
import org.jkiss.dbeaver.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * ItemListControl
 */
public class ProgressPageControl extends Composite //implements IRunnableContext
{
    //static final Log log = LogFactory.getLog(ProgressPageControl.class);

    private final static int PROGRESS_MIN = 0;
    private final static int PROGRESS_MAX = 20;

    private ProgressBar progressBar;
    private ToolBar progressTools;
    private ToolItem stopButton;
    private Label listInfoLabel;

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

        listInfoLabel = new Label(infoGroup, SWT.NONE);
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
        customControls.setLayout(new FillLayout());

        Label phLabel = new Label(customControls, SWT.NONE);
        phLabel.setText(" ");

        return customControls;
    }

    @Override
    public void dispose()
    {
        UIUtils.dispose(listInfoLabel);
        UIUtils.dispose(progressBar);
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

    public class ProgressVisualizer<RESULT> implements ILoadVisualizer<RESULT> {

        private boolean completed = false;
        private String curStatus;

        public Shell getShell() {
            return ProgressPageControl.this.getShell();
        }

        public DBRProgressMonitor overwriteMonitor(final DBRProgressMonitor monitor)
        {
            return new ProxyProgressMonitor(monitor) {
                @Override
                public void beginTask(final String name, int totalWork)
                {
                    super.beginTask(name, totalWork);
                    curStatus = name;
                }

                @Override
                public void done()
                {
                    super.done();
                    curStatus = "";
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
                }
            };
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
                progressBar.setSelection(loadCount);
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