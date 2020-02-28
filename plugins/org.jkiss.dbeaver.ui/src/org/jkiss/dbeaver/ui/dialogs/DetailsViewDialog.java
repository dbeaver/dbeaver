/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPImage;

/**
 * A dialog with extendable details area. Similar to ErrorDialog.
 */
public class DetailsViewDialog extends BaseDialog {

	protected Button detailsButton;
	private Control detailsContents = null;

	public DetailsViewDialog(Shell parentShell, String title, @Nullable DBPImage icon) {
		super(parentShell, title, icon);
	}

	protected void buttonPressed(int id) {
		if (id == IDialogConstants.DETAILS_ID) {
			// was the details button pressed?
			toggleDetailsArea();
		} else {
			super.buttonPressed(id);
		}
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Details buttons
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
		createDetailsButton(parent);
	}

	protected void createDetailsButton(Composite parent) {
		detailsButton = createButton(
			parent,
			IDialogConstants.DETAILS_ID,
			getDetailsLabel(true),
			false);
	}

	protected String getDetailsLabel(boolean show) {
		return show ? IDialogConstants.SHOW_DETAILS_LABEL : IDialogConstants.HIDE_DETAILS_LABEL;
	}

	@Override
	protected Composite createDialogArea(Composite parent) {
		Composite composite = super.createDialogArea(parent);
		createMessageArea(composite);

		GridData childData = new GridData(GridData.FILL_BOTH);
		childData.horizontalSpan = 2;
		childData.grabExcessVerticalSpace = false;
		composite.setLayoutData(childData);
		composite.setFont(parent.getFont());

		return composite;
	}

	private void toggleDetailsArea() {
		boolean opened = false;
		Point windowSize = getShell().getSize();
		if (detailsContents != null) {
			detailsContents.dispose();
			detailsContents = null;
			detailsButton.setText(getDetailsLabel(true));
			opened = false;
		} else {
			detailsContents = createDetailsContents((Composite) getContents());
			detailsButton.setText(getDetailsLabel(false));
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
		if (detailsContents == null) {
			Control control = getContents();
			if (control != null && !control.isDisposed()) {
				toggleDetailsArea();
			}
		}
	}

    @Override
	protected boolean isResizable() {
    	return true;
    }

	protected void createMessageArea(Composite composite) {

	}

	protected Control createDetailsContents(Composite composite) {
		return null;
	}

}
