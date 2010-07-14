/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * ItemListControl
 */
public class ProgressPageControl extends Composite
{
    static final Log log = LogFactory.getLog(ProgressPageControl.class);

    private final static int PROGRESS_MIN = 0;
    private final static int PROGRESS_MAX = 10;

    protected final IWorkbenchPart workbenchPart;
    private ProgressBar progressBar;
    private Label listInfoLabel;

    private int loadCount = 0;

    public ProgressPageControl(
        Composite parent,
        int style,
        IWorkbenchPart workbenchPart)
    {
        super(parent, style);
        this.workbenchPart = workbenchPart;
        this.setLayout(new GridLayout(1, true));
    }

    public void setInfo(String info)
    {
        if (!listInfoLabel.isDisposed()) {
            listInfoLabel.setText(info);
        }
    }

    protected int getProgressCellCount()
    {
        return 2;
    }

    public final Composite createProgressPanel()
    {
        return createProgressPanel(this);
    }

    protected Composite createProgressPanel(Composite container)
    {
        Composite infoGroup = new Composite(container, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        infoGroup.setLayoutData(gd);
        GridLayout gl = new GridLayout(getProgressCellCount(), false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        infoGroup.setLayout(gl);

        listInfoLabel = new Label(infoGroup, SWT.NONE);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        listInfoLabel.setLayoutData(gd);

        progressBar = new ProgressBar(infoGroup, SWT.SMOOTH | SWT.HORIZONTAL);
        progressBar.setSize(300, 16);
        progressBar.setState(SWT.NORMAL);
        progressBar.setMinimum(PROGRESS_MIN);
        progressBar.setMaximum(PROGRESS_MAX);
        progressBar.setToolTipText("Loading progress");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        progressBar.setLayoutData(gd);

        return infoGroup;
    }

    @Override
    public void dispose()
    {
        UIUtils.dispose(listInfoLabel);
        UIUtils.dispose(progressBar);
        super.dispose();
    }

    public <RESULT> ProgressVisualizer<RESULT> createVisualizer()
    {
        return new ProgressVisualizer<RESULT>();
    }

    protected class ProgressVisualizer<RESULT> implements ILoadVisualizer<RESULT> {

        private boolean completed = false;

        public Shell getShell() {
            return workbenchPart.getSite().getShell();
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
                }
                progressBar.setSelection(loadCount++ % PROGRESS_MAX);
            }
        }

        public void completeLoading(RESULT result)
        {
            completed = true;

            if (ProgressPageControl.this.isDisposed()) {
                return;
            }
            visualizeLoading();
            if (!progressBar.isDisposed()) {
                progressBar.setState(SWT.PAUSED);
                progressBar.setVisible(false);
            }
        }

    }

}