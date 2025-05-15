/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.resultset.view;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.views.IViewDescriptor;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetContainer;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetErrorAction;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetErrorActionDescriptor;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetErrorActionRegistry;
import org.jkiss.dbeaver.ui.dialogs.EditTextDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @since 3.1
 */
class ErrorDetailsPart {
    private static final Log log = Log.getLog(ErrorDetailsPart.class);

    private final Composite parent;
    private final IStatus reason;

    ErrorDetailsPart(final Composite parent, IStatus reason, @NotNull IResultSetContainer resultSetContainer) {
        this.parent = parent;
        this.reason = reason;

        parent.setLayout(GridLayoutFactory.fillDefaults().extendedMargins(5, 0, 0, 0).numColumns(2).create());

        Label imageLabel = new Label(parent, SWT.NONE);
        imageLabel.setImage(getImage());
        imageLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER | GridData.VERTICAL_ALIGN_BEGINNING));

        Text text = new Text(parent, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL);
        text.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        text.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));

        text.setLayoutData(new GridData(GridData.FILL_BOTH));
        text.setText(GeneralUtils.normalizeLineSeparators(reason.getMessage()));
        text.setFont(UIUtils.getMonospaceFont());

        text.addListener(SWT.Resize, e -> {
            final Point size = text.getSize();
            if (size.y > 100) {
                // Can't use the setSize here - will revalidate every time the parent is resized
                ((GridData) text.getLayoutData()).heightHint = 100;
                parent.layout(true);
            }
        });

        Composite buttonParent = new Composite(parent, SWT.NONE);
        buttonParent.setLayout(GridLayoutFactory.fillDefaults().numColumns(3).create());
        buttonParent.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(2, 1).create());
        buttonParent.setBackground(parent.getBackground());

        // Spacer
        new Label(buttonParent, SWT.NONE).setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

        for (ResultSetErrorActionDescriptor descriptor : ResultSetErrorActionRegistry.getInstance().getActions()) {
            IResultSetErrorAction action;
            try {
                action = descriptor.createInstance();
            } catch (DBException e) {
                log.error("Can't create error action '" + descriptor.getLabel() + "'", e);
                continue;
            }
            if (!action.isVisible(resultSetContainer, this.reason)) {
                continue;
            }
            UIUtils.createDialogButton(
                buttonParent,
                descriptor.getLabel(),
                descriptor.getIcon(),
                descriptor.getDescription(),
                SelectionListener.widgetSelectedAdapter(e -> action.perform(resultSetContainer, this.reason))
            );
            ((GridLayout) buttonParent.getLayout()).numColumns++;
        }

        createShowLogButton(buttonParent);
        createDetailsButton(buttonParent);
    }

    /**
     * Return the image for the upper-left corner of this part
     *
     * @return the image
     */
    private Image getImage() {
        return switch (reason.getSeverity()) {
            case IStatus.ERROR -> DBeaverIcons.getImage(DBIcon.STATUS_ERROR);
            case IStatus.WARNING -> DBeaverIcons.getImage(DBIcon.STATUS_WARNING);
            default -> DBeaverIcons.getImage(DBIcon.STATUS_INFO);
        };
    }

    private void showDetails() {
        EditTextDialog dialog = new EditTextDialog(
            parent.getShell(),
            "Error details",
            getDetails(reason),
            true);
        dialog.setMonospaceFont(true);
        dialog.setAutoSize(true);
        dialog.open();
    }

    private String getDetails(IStatus status) {
        if (status.getException() != null) {
            return GeneralUtils.normalizeLineSeparators(getStackTrace(status.getException()));
        }

        return ""; //$NON-NLS-1$
    }

    private String getStackTrace(Throwable throwable) {
        StringWriter swriter = new StringWriter();
        try (PrintWriter pwriter = new PrintWriter(swriter)) {
            throwable.printStackTrace(pwriter);
            pwriter.flush();
        }
        return swriter.toString();
    }

    private void createShowLogButton(Composite parent) {
        IViewDescriptor descriptor = PlatformUI.getWorkbench().getViewRegistry().find(IActionConstants.LOG_VIEW_ID);
        if (descriptor == null) {
            return;
        }
        Button button = UIUtils.createDialogButton(
            parent,
            "Show log",
            null,
            WorkbenchMessages.ErrorLogUtil_ShowErrorLogTooltip,
            SelectionListener.widgetSelectedAdapter(e -> {
                try {
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(IActionConstants.LOG_VIEW_ID);
                } catch (CoreException ce) {
                    StatusManager.getManager().handle(ce, WorkbenchPlugin.PI_WORKBENCH);
                }
            })
        );
        final Image image = descriptor.getImageDescriptor().createImage();
        button.setImage(image);
        button.addDisposeListener(e -> image.dispose());
    }

    private void createDetailsButton(@NotNull Composite parent) {
        if (reason.getException() == null) {
            return;
        }
        UIUtils.createDialogButton(
            parent,
            IDialogConstants.SHOW_DETAILS_LABEL,
            SelectionListener.widgetSelectedAdapter(e -> showDetails())
        );
    }
}
