/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.views.IViewDescriptor;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IActionConstants;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

/**
 * @since 3.1
 */
public class ErrorDetailsPart {

	private boolean showingDetails = false;
	private Button detailsButton;
	private Composite detailsArea;
	private Control details = null;
	private IStatus reason;

	public ErrorDetailsPart(final Composite parent, IStatus reason_) {
		Color bgColor = parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
		Color fgColor = parent.getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND);

		parent.setBackground(bgColor);
		parent.setForeground(fgColor);

		this.reason = reason_;
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

		Label imageLabel = new Label(parent, SWT.NONE);
		imageLabel.setBackground(bgColor);
		Image image = getImage();
		if (image != null) {
			image.setBackground(bgColor);
			imageLabel.setImage(image);
			GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER | GridData.VERTICAL_ALIGN_BEGINNING);
			imageLabel.setLayoutData(gridData);
		}

		Text text = new Text(parent, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP);
		text.setBackground(bgColor);
		text.setForeground(fgColor);

		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		text.setText(reason.getMessage());

		Composite buttonParent = new Composite(parent, SWT.NONE);
		buttonParent.setBackground(parent.getBackground());
		GridLayout buttonsLayout = new GridLayout();
		buttonsLayout.numColumns = 2;
		buttonsLayout.marginHeight = 0;
		buttonsLayout.marginWidth = 0;
		buttonsLayout.horizontalSpacing = 0;
		buttonParent.setLayout(buttonsLayout);

		detailsButton = new Button(buttonParent, SWT.PUSH);
		detailsButton.addSelectionListener(widgetSelectedAdapter(e -> showDetails(!showingDetails)));

		GridData gd = new GridData(SWT.BEGINNING, SWT.FILL, false, false);
		detailsButton.setLayoutData(gd);
		detailsButton.setVisible(reason.getException() != null);

		createShowLogButton(buttonParent);

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

	/**
	 * Return the image for the upper-left corner of this part
	 *
	 * @return the image
	 */
	private Image getImage() {
		Display d = Display.getCurrent();

		switch (reason.getSeverity()) {
		case IStatus.ERROR:
			return DBeaverIcons.getImage(DBIcon.STATUS_ERROR);
		case IStatus.WARNING:
			return DBeaverIcons.getImage(DBIcon.STATUS_WARNING);
		default:
			return DBeaverIcons.getImage(DBIcon.STATUS_INFO);
		}
	}

	private void showDetails(boolean shouldShow) {
		if (shouldShow == showingDetails) {
			return;
		}
		this.showingDetails = shouldShow;
		updateDetailsText();
	}

	private void updateDetailsText() {
		if (details != null) {
			details.dispose();
			details = null;
		}

		if (showingDetails) {
			detailsButton.setText("    " + IDialogConstants.HIDE_DETAILS_LABEL + "    ");
			Text detailsText = new Text(detailsArea,
					SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.READ_ONLY | SWT.LEFT_TO_RIGHT);
			detailsText.setText(getDetails(reason));
			detailsText.setBackground(detailsText.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			details = detailsText;
			detailsArea.layout(true);
		} else {
			detailsButton.setText("    " + IDialogConstants.SHOW_DETAILS_LABEL + "    ");
		}
	}

	private String getDetails(IStatus status) {
		if (status.getException() != null) {
			return getStackTrace(status.getException());
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
}
