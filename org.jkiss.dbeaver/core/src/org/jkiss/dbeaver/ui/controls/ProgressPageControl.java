/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * ItemListControl
 */
public class ProgressPageControl extends Composite
{
    static final Log log = LogFactory.getLog(ProgressPageControl.class);

    private final static int PROGRESS_MIN = 0;
    private final static int PROGRESS_MAX = 20;

    protected final IWorkbenchPart workbenchPart;
    private ProgressBar progressBar;
    private ToolBar progressTools;
    private ToolItem stopButton;
    private Label listInfoLabel;

    private int loadCount = 0;

    public ProgressPageControl(
        Composite parent,
        int style,
        IWorkbenchPart workbenchPart)
    {
        super(parent, style);
        this.workbenchPart = workbenchPart;
        GridLayout layout = new GridLayout(1, true);
        //layout.marginHeight = 0;
        //layout.marginWidth = 0;
        //layout.horizontalSpacing = 0;
        //layout.verticalSpacing = 0;
        this.setLayout(layout);
    }

    public IWorkbenchPart getWorkbenchPart()
    {
        return workbenchPart;
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
        customControls.setLayout(new FillLayout(SWT.HORIZONTAL));

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

    public ProgressVisualizer<?> createVisualizer()
    {
        return new ProgressVisualizer<Object>();
    }

    protected class ProgressVisualizer<RESULT> implements ILoadVisualizer<RESULT> {

        private boolean completed = false;

        public Shell getShell() {
            return UIUtils.getShell(workbenchPart);
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