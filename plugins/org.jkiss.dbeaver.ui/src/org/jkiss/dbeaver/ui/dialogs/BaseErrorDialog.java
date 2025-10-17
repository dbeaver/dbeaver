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
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IconAndMessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.controls.StyledTextUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.Objects;

/**
 * A dialog to display one or more errors to the user, as contained in an
 * <code>IStatus</code> object. If an error contains additional detailed
 * information then a Details button is automatically supplied, which shows or
 * hides an error details viewer when pressed by the user.
 * *
 * Originally copied from org.eclipse.jface.dialogs.ErrorDialog
 */
public class BaseErrorDialog extends IconAndMessageDialog {

    private static final String NESTING_INDENT = "  "; //$NON-NLS-1$

    private Button detailsButton;
    private final String title;
    private StyledText detailsText;
    private boolean listCreated = false;

    /**
     * Filter mask for determining which status items to display.
     */
    private int displayMask = 0xFFFF;
    private IStatus status;

    private boolean shouldIncludeTopLevelErrorInDetails = false;

    public BaseErrorDialog(
        @NotNull Shell parentShell,
        @Nullable String dialogTitle,
        @Nullable String message,
        @NotNull IStatus status,
        int displayMask
    ) {
        super(parentShell);
        this.title = dialogTitle == null ? JFaceResources
            .getString("Problem_Occurred") : //$NON-NLS-1$
            dialogTitle;
        this.message = message == null ? status.getMessage()
            : JFaceResources.format("Reason", message, status.getMessage()); //$NON-NLS-1$
        this.status = status;
        this.displayMask = displayMask;
    }

    /*
     * Handles the pressing of the Ok or Details button in this dialog. If the
     * Ok button was pressed then close this dialog. If the Details button was
     * pressed then toggle the displaying of the error details area. Note that
     * the Details button will only be visible if the error being displayed
     * specifies child details.
     */
    @Override
    protected void buttonPressed(int id) {
        if (id == IDialogConstants.DETAILS_ID) {
            // was the details button pressed?
            toggleDetailsArea();
        } else {
            super.buttonPressed(id);
        }
    }

    @Override
    protected void configureShell(@NotNull Shell shell) {
        super.configureShell(shell);
        shell.setText(title);
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        // create OK and Details buttons
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createDetailsButton(parent);
    }

    protected void createDetailsButton(@NotNull Composite parent) {
        if (shouldShowDetailsButton()) {
            detailsButton = createButton(parent, IDialogConstants.DETAILS_ID,
                IDialogConstants.SHOW_DETAILS_LABEL, false
            );
        }
    }

    @Override
    protected Control createDialogArea(@NotNull Composite parent) {
        // Create a composite with standard margins and spacing
        // Add the messageArea to this composite so that as subclasses add widgets to the messageArea
        // and dialogArea, the number of children of parent remains fixed and with consistent layout.
        // Fixes bug #240135
        Composite composite = new Composite(parent, SWT.NONE);
        createMessageArea(composite);
        GridLayout layout = new GridLayout();
        layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        layout.numColumns = 2;
        composite.setLayout(layout);
        GridData childData = new GridData(GridData.FILL_BOTH);
        childData.horizontalSpan = 2;
        childData.grabExcessVerticalSpace = false;
        composite.setLayoutData(childData);
        composite.setFont(parent.getFont());

        return composite;
    }

    @Override
    protected void createDialogAndButtonArea(@NotNull Composite parent) {
        super.createDialogAndButtonArea(parent);
        if (this.dialogArea instanceof Composite dialogComposite) {
            // Create a label if there are no children to force a smaller layout
            if (dialogComposite.getChildren().length == 0) {
                new Label(dialogComposite, SWT.NULL);
            }
        }
    }

    @Override
    protected Image getImage() {
        if (status != null) {
            if (status.getSeverity() == IStatus.WARNING) {
                return getWarningImage();
            }
            if (status.getSeverity() == IStatus.INFO) {
                return getInfoImage();
            }
        }
        // If it was not a warning or an error then return the error image
        return getErrorImage();
    }

    protected StyledText createDropDownList(@NotNull Composite parent) {
        // create the list
        detailsText = new StyledText(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL
            | SWT.MULTI);
        // fill the list
        populateList();
        GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL
            | GridData.GRAB_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL
            | GridData.GRAB_VERTICAL);
        data.heightHint = 150;
        data.horizontalSpan = 2;
        detailsText.setLayoutData(data);
        detailsText.setFont(parent.getFont());
        listCreated = true;
        StyledTextUtils.fillDefaultStyledTextContextMenu(detailsText);
        return detailsText;
    }

    @Override
    public int open() {
        if (shouldDisplay(status, displayMask)) {
            return super.open();
        }
        setReturnCode(OK);
        return OK;
    }

    public static int openError(Shell parent, String dialogTitle, String message, IStatus status) {
        return openError(parent, dialogTitle, message, status,
            IStatus.OK | IStatus.INFO | IStatus.WARNING | IStatus.ERROR
        );
    }

    public static int openError(Shell parentShell, String title, String message, IStatus status, int displayMask) {
        BaseErrorDialog dialog = new BaseErrorDialog(parentShell, title, message, status, displayMask);
        return dialog.open();
    }

    private void populateList() {
        populateList(status, 0, shouldIncludeTopLevelErrorInDetails);
    }

    private boolean listContentExists() {
        return listContentExists(status, shouldIncludeTopLevelErrorInDetails);
    }

    private void populateList(
        IStatus buildingStatus,
        int nesting,
        boolean includeStatus
    ) {
        if (!buildingStatus.matches(displayMask)) {
            return;
        }

        Throwable t = buildingStatus.getException();
        boolean incrementNesting = false;

        String statusMessage = null;
        if (includeStatus) {
            StringBuilder sb = new StringBuilder();
            sb.append(NESTING_INDENT.repeat(Math.max(0, nesting)));
            statusMessage = buildingStatus.getMessage();
            sb.append(statusMessage.trim());
            sb.append("\n");
            detailsText.append(sb.toString());
            incrementNesting = true;
        }

        if (!(t instanceof CoreException) && t != null) {
            // Include low-level exception message
            String message = GeneralUtils.makeStandardErrorMessage(t);
            if (!Objects.equals(statusMessage, message)) {
                StringBuilder sb = new StringBuilder();
                sb.append(NESTING_INDENT.repeat(Math.max(0, nesting)));
                if (message == null) {
                    message = t.toString();
                }

                sb.append(message.trim());
                sb.append("\n");
                detailsText.append(sb.toString());
                incrementNesting = true;
            }
        }

        if (incrementNesting) {
            nesting++;
        }

        // Look for a nested core exception
        if (t instanceof CoreException ce) {
            IStatus eStatus = ce.getStatus();
            // Only print the exception message if it is not contained in the
            // parent message
            if (message == null || !message.contains(eStatus.getMessage())) {
                populateList(eStatus, nesting, false);
            }
        }

        // Look for child status
        IStatus[] children = buildingStatus.getChildren();
        for (IStatus element : children) {
            populateList(element, nesting, false);
        }
    }

    private boolean listContentExists(
        IStatus buildingStatus,
        boolean includeStatus
    ) {
        if (!buildingStatus.matches(displayMask)) {
            return false;
        }

        Throwable t = buildingStatus.getException();
        if (includeStatus) {
            return true;
        }

        if (t != null && !(t instanceof CoreException)) {
            return true;
        }

        boolean result = false;

        // Look for a nested core exception
        if (t != null) {
            CoreException ce = (CoreException) t;
            IStatus eStatus = ce.getStatus();
            // Gets exception message if it is not contained in the
            // parent message
            if (message == null || !message.contains(eStatus.getMessage())) {
                result |= listContentExists(eStatus, true);
            }
        }

        // Look for child status
        IStatus[] children = buildingStatus.getChildren();
        for (IStatus element : children) {
            result |= listContentExists(element, true);
        }

        return result;
    }

    protected static boolean shouldDisplay(@NotNull IStatus status, int mask) {
        IStatus[] children = status.getChildren();
        if (children == null || children.length == 0) {
            return status.matches(mask);
        }
        for (IStatus element : children) {
            if (element.matches(mask)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Toggles the unfolding of the details area. This is triggered by the user
     * pressing the details button.
     */
    private void toggleDetailsArea() {
        boolean opened;
        Point windowSize = getShell().getSize();
        if (listCreated) {
            detailsText.dispose();
            listCreated = false;
            detailsButton.setText(IDialogConstants.SHOW_DETAILS_LABEL);
            opened = false;
        } else {
            detailsText = createDropDownList((Composite) getContents());
            detailsButton.setText(IDialogConstants.HIDE_DETAILS_LABEL);
            getContents().getShell().layout();
            opened = true;
        }
        Point newSize = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);
        int diffY = newSize.y - windowSize.y;
        // increase the dialog height if details were opened and such increase is necessary
        // decrease the dialog height if details were closed and empty space appeared
        if ((opened && diffY > 0) || (!opened && diffY < 0)) {
            getShell().setSize(new Point(windowSize.x, windowSize.y + (diffY)));
        }
    }

    protected final void showDetailsArea() {
        if (!listCreated) {
            Control control = getContents();
            if (control != null && !control.isDisposed()) {
                toggleDetailsArea();
            }
        }
    }

    protected boolean shouldShowDetailsButton() {
        return listContentExists();
    }

    protected final void setStatus(IStatus status) {
        if (this.status != status) {
            this.status = status;
        }
        shouldIncludeTopLevelErrorInDetails = true;
        if (listCreated) {
            repopulateList();
        }
    }

    private void repopulateList() {
        if (detailsText != null && !detailsText.isDisposed()) {
            detailsText.setText("");
            populateList();
        }
    }

    @Override
    protected int getColumnCount() {
        return 3;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

}
