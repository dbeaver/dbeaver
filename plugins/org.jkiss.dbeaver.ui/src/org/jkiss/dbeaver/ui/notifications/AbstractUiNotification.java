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

/**
 * Copied from Mylyn sources
 */
public abstract class AbstractUiNotification extends AbstractNotification {
    public AbstractUiNotification(String eventId) {
        super(eventId);
    }

    public abstract Image getNotificationImage();

    public abstract Image getNotificationKindImage();

    public abstract void open();
}
