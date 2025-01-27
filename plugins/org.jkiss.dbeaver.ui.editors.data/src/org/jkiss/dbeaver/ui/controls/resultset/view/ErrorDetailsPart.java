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
package org.jkiss.dbeaver.ui.controls.resultset.view;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetContainerExt;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.dialogs.EditTextDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

/**
 * @since 3.1
 */
class ErrorDetailsPart {

    private final Composite parent;
    private final IStatus reason;
    private final IResultSetContainerExt resultSetContainer;

    ErrorDetailsPart(final Composite parent, IStatus reason_, @Nullable IResultSetContainerExt resultSetContainer) {
        this.parent = parent;
        this.resultSetContainer = resultSetContainer;
        Color bgColor = parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
        Color fgColor = parent.getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND);

        parent.setBackground(bgColor);
        parent.setForeground(fgColor);

        this.reason = reason_;
        GridLayout layout = new GridLayout();

        layout.numColumns = 2;
        layout.makeColumnsEqualWidth = false;

        int spacing = 8;
        int margins = 8;
        layout.marginBottom = margins;
        layout.marginTop = margins;
        layout.marginLeft = margins;
        layout.marginRight = margins;
        layout.horizontalSpacing = spacing;
        layout.verticalSpacing = spacing;
        parent.setLayout(layout);

        Label imageLabel = new Label(parent, SWT.NONE);
        imageLabel.setBackground(bgColor);
        Image image = getImage();
        if (image != null) {
            image.setBackground(bgColor);
            imageLabel.setImage(image);
            GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER | GridData.VERTICAL_ALIGN_BEGINNING);
            imageLabel.setLayoutData(gridData);
        }

        Text text = new Text(parent, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL);
        text.setBackground(bgColor);
        text.setForeground(fgColor);

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
        GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        gd.horizontalSpan = 2;
        buttonParent.setLayoutData(gd);
        buttonParent.setBackground(parent.getBackground());
        GridLayout buttonsLayout = new GridLayout();
        buttonsLayout.numColumns = 3;
        buttonsLayout.marginHeight = 0;
        buttonsLayout.marginWidth = 0;
        buttonsLayout.horizontalSpacing = 0;
        buttonParent.setLayout(buttonsLayout);

        Button detailsButton = new Button(buttonParent, SWT.PUSH);
        detailsButton.setText(IDialogConstants.SHOW_DETAILS_LABEL);
        detailsButton.addSelectionListener(widgetSelectedAdapter(e -> showDetails()));

        detailsButton.setLayoutData(new GridData(
            SWT.BEGINNING, SWT.FILL, false, false));
        detailsButton.setVisible(reason.getException() != null);

        createShowLogButton(buttonParent);
        createGoToErrorButton(buttonParent);
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
        Button button = new Button(parent, SWT.PUSH);
        button.addSelectionListener(widgetSelectedAdapter(e -> {
            try {
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(IActionConstants.LOG_VIEW_ID);
            } catch (CoreException ce) {
                StatusManager.getManager().handle(ce, WorkbenchPlugin.PI_WORKBENCH);
            }
        }));
        final Image image = descriptor.getImageDescriptor().createImage();
        button.setImage(image);
        button.setToolTipText(WorkbenchMessages.ErrorLogUtil_ShowErrorLogTooltip);
        button.addDisposeListener(e -> image.dispose());
    }

    private void createGoToErrorButton(@NotNull Composite parent) {
        Button button = new Button(parent, SWT.PUSH);
        button.addSelectionListener(widgetSelectedAdapter(e -> {
            String message = reason.getMessage();
            if (CommonUtils.isNotEmpty(message)) {
                resultSetContainer.showCurrentError();
            }
        }));
        button.setImage(DBeaverIcons.getImage(UIIcon.BUTTON_GO_TO_ERROR));
        button.setToolTipText(ResultSetMessages.error_part_button_go_to_error);
        button.setVisible(reason.getException() != null && resultSetContainer != null);
    }
}
