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
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.StringUtils;

import java.util.Objects;

/**
 * A dialog to display one or more errors to the user, as contained in an
 * <code>IStatus</code> object. If an error contains additional detailed
 * information then a Details button is automatically supplied, which shows or
 * hides an error details viewer when pressed by the user.
 * *
 * Originally copied from org.eclipse.jface.dialogs.ErrorDialog
 */
public class BaseErrorDialog extends BaseDialog {

    private static final String NESTING_INDENT = "  "; //$NON-NLS-1$
    public static final int DEFAULT_MESSAGE_WIDTH = 80;
    private static final int DETAILS_MESSAGE_WIDTH = 130;

    protected String message;

    private Button detailsButton;
    private final String title;
    private Text detailsText;

    /**
     * Filter mask for determining which status items to display.
     */
    private final int displayMask;
    private IStatus status;

    private boolean shouldIncludeTopLevelErrorInDetails = false;
    private Composite detailPanel;

    public BaseErrorDialog(
        @NotNull Shell parentShell,
        @Nullable String dialogTitle,
        @Nullable String message,
        @NotNull IStatus status,
        int displayMask
    ) {
        super(parentShell, dialogTitle, DBIcon.STATUS_ERROR);
        this.title = dialogTitle == null ? JFaceResources
            .getString("Problem_Occurred") : //$NON-NLS-1$
            dialogTitle;
        this.message = message == null ? status.getMessage()
            : JFaceResources.format("Reason", message, status.getMessage()); //$NON-NLS-1$
        this.status = status;
        this.displayMask = displayMask;
    }

    @Nullable
    protected Text getDetailsText() {
        return detailsText;
    }

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
    protected void createButtonsForButtonBar(@NotNull Composite parent, int alignment) {
        if (alignment == SWT.LEAD) {
            createDetailsButton(parent);
        } else {
            createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        }
    }

    protected void createDetailsButton(@NotNull Composite parent) {
        if (shouldShowDetailsButton()) {
            detailsButton = createButton(parent, IDialogConstants.DETAILS_ID,
                IDialogConstants.SHOW_DETAILS_LABEL, false
            );
        }
    }

    @Override
    protected Composite createContents(@NotNull Composite parent) {
        Composite contents = (Composite) super.createContents(parent);

        createDropDownList(contents);

        return contents;
    }

    @NotNull
    @Override
    protected Composite createDialogArea(@NotNull Composite parent) {
        Composite dialogArea = super.createDialogArea(parent);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        dialogArea.setLayoutData(gd);

        // create composite
        // create image
        DBPImage image = getImage();
        ((GridLayout) dialogArea.getLayout()).numColumns++;
        Label imageLabel = new Label(dialogArea, SWT.NULL);
        imageLabel.setImage(DBeaverIcons.getImage(image));
        GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.BEGINNING).applyTo(imageLabel);

        // create message
        {
            ((GridLayout) dialogArea.getLayout()).numColumns++;

            Text messageText = new Text(dialogArea, SWT.READ_ONLY | SWT.WRAP | SWT.MULTI);
            messageText.setText(message);
            applyMessageSizes(messageText, false, DEFAULT_MESSAGE_WIDTH);
        }
        return dialogArea;
    }

    private void applyMessageSizes(@NotNull Text messageText, boolean fillVertical, int messageWidthHintChars) {
        GridData gd = new GridData(fillVertical ? GridData.FILL_BOTH : GridData.FILL_HORIZONTAL);
        gd.minimumWidth = IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH;

        int fontHeight = UIUtils.getFontHeight(messageText);
        int maxDialogWidth = fontHeight * messageWidthHintChars;

        Point textSize;
        GC gc = new GC(messageText);
        try {
            gc.setFont(messageText.getFont());
            double charsPerLine = (double) maxDialogWidth / gc.getFontMetrics().getAverageCharacterWidth();
            String wrappedMessage = StringUtils.wrap(messageText.getText(), (int) charsPerLine);
            textSize = gc.textExtent(wrappedMessage);
            //textSize.y = wrappedMessage.split("\n").length * messageText.getLineHeight();
            textSize.y += 5; // Just in case. On MacOS height should be a bit more to avoid truncation
            textSize.x += fontHeight * 2;
            if (RuntimeUtils.isMacOS()) {
                textSize.x += fontHeight * 8;
            }

        } finally {
            gc.dispose();
        }

        gd.heightHint = Math.min(textSize.y, fontHeight * 10);
        gd.widthHint = Math.min(textSize.x, maxDialogWidth);

        gd.grabExcessHorizontalSpace = true;
        messageText.setLayoutData(gd);
    }

    @NotNull
    @Override
    public DBPImage getImage() {
        if (status != null) {
            if (status.getSeverity() == IStatus.WARNING) {
                return DBIcon.STATUS_WARNING;
            }
            if (status.getSeverity() == IStatus.INFO) {
                return DBIcon.STATUS_INFO;
            }
        }
        // If it was not a warning or an error then return the error image
        return DBIcon.STATUS_ERROR;
    }

    protected boolean isDetailsVisible() {
        return detailPanel.isVisible();
    }

    protected void createDropDownList(@NotNull Composite parent) {
        // create the list
        detailPanel = super.createDialogPanelWithMargins(parent, true);
        detailsText = new Text(detailPanel, SWT.BORDER | SWT.WRAP | SWT.READ_ONLY | SWT.V_SCROLL | SWT.MULTI);
        // fill the list
        GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL
            | GridData.GRAB_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL
            | GridData.GRAB_VERTICAL);
        data.widthHint = 100;
        detailPanel.setVisible(false);
        ((GridData)detailPanel.getLayoutData()).exclude = true;
        //data.horizontalSpan = ((GridLayout)parent.getLayout()).numColumns;
        detailsText.setLayoutData(data);

        populateList();

        applyMessageSizes(detailsText, true, DETAILS_MESSAGE_WIDTH);
    }

    @Override
    public int open() {
        if (shouldDisplay(status, displayMask)) {
            return super.open();
        }
        setReturnCode(OK);
        return OK;
    }

    private void populateList() {
        populateList(status, 0, shouldIncludeTopLevelErrorInDetails);
        int rowCount = 10;//Math.min(detailsText.getText().split("\n").length, 10) + 1;
        ((GridData)detailsText.getLayoutData()).heightHint = rowCount * UIUtils.getFontHeight(detailsText);
    }

    private boolean listContentExists() {
        return listContentExists(status, shouldIncludeTopLevelErrorInDetails);
    }

    private void populateList(
        @NotNull IStatus status,
        int nesting,
        boolean includeStatus
    ) {
        if (!status.matches(displayMask)) {
            return;
        }

        Throwable t = status.getException();
        boolean incrementNesting = false;

        String statusMessage = null;
        if (includeStatus) {
            StringBuilder sb = new StringBuilder();
            sb.append(NESTING_INDENT.repeat(Math.max(0, nesting)));
            statusMessage = status.getMessage();
            sb.append(statusMessage.trim());
            sb.append("\n");
            detailsText.append(sb.toString());
            incrementNesting = true;
        }

        if (!(t instanceof CoreException) && t != null) {
            // Include low-level exception message
            String message = GeneralUtils.makeStandardErrorMessage(t);
            if (!Objects.equals(statusMessage, message)) {
                String sb = NESTING_INDENT.repeat(Math.max(0, nesting))
                    + message.trim()
                    + "\n";
                detailsText.append(sb);
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
        IStatus[] children = status.getChildren();
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
        boolean opened = false;
        if (isDetailsVisible()) {
            detailPanel.setVisible(false);
            ((GridData)detailPanel.getLayoutData()).exclude = true;
            detailsButton.setText(IDialogConstants.SHOW_DETAILS_LABEL);
        } else if (getDialogArea() != null) {
            detailPanel.setVisible(true);
            ((GridData)detailPanel.getLayoutData()).exclude = false;
            detailsButton.setText(IDialogConstants.HIDE_DETAILS_LABEL);
            opened = true;
        }
        getShell().layout(true, true);
        Point newSize = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);
        if (opened) {
            UIUtils.resizeShell(getShell());
        } else {
            getShell().setSize(new Point(newSize.x, newSize.y));
        }
    }

    protected final void showDetailsArea() {
        if (!isDetailsVisible()) {
            toggleDetailsArea();
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
        repopulateList();
    }

    private void repopulateList() {
        if (detailsText != null && !detailsText.isDisposed()) {
            detailsText.setText("");
            populateList();
        }
    }

}
