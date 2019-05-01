/*******************************************************************************
 * Copyright (c) 2004, 2011 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.jkiss.dbeaver.ui.notifications;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;

/**
 * A popup window that uses the workbench shell image in the title.
 * 
 * @author Steffen Pingel
 */
public class AbstractWorkbenchNotificationPopup extends AbstractNotificationPopup {

	public AbstractWorkbenchNotificationPopup(Display display, int style) {
        super(display, style);
	}

	public AbstractWorkbenchNotificationPopup(Display display) {
		super(display);
	}

	@Override
	protected Image getPopupShellImage(int maximumHeight) {
		return DBeaverIcons.getImage(UIIcon.DBEAVER_LOGO_SMALL);
	}

}