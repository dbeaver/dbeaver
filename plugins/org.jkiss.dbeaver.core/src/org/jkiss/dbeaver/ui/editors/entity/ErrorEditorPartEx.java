/*
 * Copyright (C) 2010-2015 Serge Rieder
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
package org.jkiss.dbeaver.ui.editors.entity;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.views.IViewDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * ErrorEditorPartEx
 */
public class ErrorEditorPartEx extends EditorPart {

    private IStatus error;
    private Composite parentControl;

    public ErrorEditorPartEx(IStatus error) {
        this.error = error;
    }

    public void doSave(IProgressMonitor monitor) {
    }

    public void doSaveAs() {
    }

    public void createPartControl(Composite parent) {
        this.parentControl = parent;
        if (error != null) {
            createErrorPane(parent);
        }
    }

    public void init(IEditorSite site, IEditorInput input) {
        setSite(site);
        setInput(input);
        setPartName(input.getName());
    }

    public boolean isDirty() {
        return false;
    }

    public boolean isSaveAsAllowed() {
        return false;
    }

    public void setFocus() {
        parentControl.setFocus();
    }

    public void setPartName(String newName) {
        super.setPartName(newName);
    }

    public void dispose() {
        super.dispose();
        parentControl = null;
    }

    private static final String LOG_VIEW_ID = "org.eclipse.pde.runtime.LogView"; //$NON-NLS-1$
    boolean showingDetails = false;
    private Button detailsButton;
    private Composite detailsArea;
    private Control details = null;

    private void createErrorPane(final Composite parent) {
        Color bgColor = parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
        Color fgColor = parent.getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND);

        parent.setBackground(bgColor);
        parent.setForeground(fgColor);

        GridLayout layout = new GridLayout();

        layout.numColumns = 3;

        int spacing = 8;
        int margins = 8;
        layout.marginBottom = margins;
        layout.marginTop = margins;
        layout.marginLeft = margins;
        layout.marginRight = margins;
        layout.horizontalSpacing = spacing;
        layout.verticalSpacing = spacing;
        parent.setLayout(layout);

        Display d = Display.getCurrent();

        Label imageLabel = new Label(parent, SWT.NONE);
        imageLabel.setBackground(bgColor);
        Image image;
        switch (error.getSeverity()) {
            case IStatus.ERROR: image = d.getSystemImage(SWT.ICON_ERROR); break;
            case IStatus.WARNING: image = d.getSystemImage(SWT.ICON_WARNING); break;
            default: image = d.getSystemImage(SWT.ICON_INFORMATION); break;
        }

        image.setBackground(bgColor);
        imageLabel.setImage(image);
        imageLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER | GridData.VERTICAL_ALIGN_BEGINNING));

        Text text = new Text(parent, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP);
        text.setBackground(bgColor);
        text.setForeground(fgColor);

        text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        text.setText(error.getMessage());

        Composite buttonParent = new Composite(parent, SWT.NONE);
        buttonParent.setBackground(parent.getBackground());
        GridLayout buttonsLayout = new GridLayout();
        buttonsLayout.numColumns = 2;
        buttonsLayout.marginHeight = 0;
        buttonsLayout.marginWidth = 0;
        buttonsLayout.horizontalSpacing = 0;
        buttonParent.setLayout(buttonsLayout);

        detailsButton = new Button(buttonParent, SWT.PUSH);
        detailsButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                showingDetails = !showingDetails;
                updateDetailsText();
            }
        });

        detailsButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, false, false));

        updateDetailsText();

        detailsArea = new Composite(parent, SWT.NONE);
        detailsArea.setBackground(bgColor);
        detailsArea.setForeground(fgColor);
        GridData data = new GridData(GridData.FILL_BOTH);
        data.horizontalSpan = 3;
        data.verticalSpan = 1;
        detailsArea.setLayoutData(data);
        detailsArea.setLayout(new FillLayout());
        parent.layout(true);
    }

    private void updateDetailsText() {
        if (details != null) {
            details.dispose();
            details = null;
        }

        if (showingDetails) {
            detailsButton.setText(IDialogConstants.HIDE_DETAILS_LABEL);
            Text detailsText = new Text(detailsArea, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.READ_ONLY | SWT.LEFT_TO_RIGHT);
            detailsText.setText(getDetails());
            detailsText.setBackground(detailsText.getDisplay().getSystemColor(
                SWT.COLOR_LIST_BACKGROUND));
            details = detailsText;
            detailsArea.layout(true);
        } else {
            detailsButton.setText(IDialogConstants.SHOW_DETAILS_LABEL);
        }
    }


    private String getDetails() {
        if (error.getException() != null) {
            return getStackTrace(error.getException());
        }
        return RuntimeUtils.getStatusText(error);
    }

    private String getStackTrace(Throwable throwable) {
        StringWriter swriter = new StringWriter();
        PrintWriter pwriter = new PrintWriter(swriter);
        throwable.printStackTrace(pwriter);
        pwriter.flush();
        pwriter.close();
        return swriter.toString();
    }

}
