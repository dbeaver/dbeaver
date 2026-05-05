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

import java.util.List;

/**
 * Copied from Mylyn sources
 */
public class NotificationSinkEvent {
    private final List<AbstractNotification> notifications;

    public NotificationSinkEvent(List<AbstractNotification> notifications) {
        this.notifications = notifications;
    }

    public List<AbstractNotification> getNotifications() {
        return this.notifications;
    }
}
