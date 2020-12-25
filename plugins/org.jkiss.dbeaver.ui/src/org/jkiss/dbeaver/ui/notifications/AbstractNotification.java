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

import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.utils.CommonUtils;

import java.util.Date;

/**
 * Copied from Mylyn sources
 */
public abstract class AbstractNotification implements Comparable<AbstractNotification>, IAdaptable {
    private final String eventId;

    public AbstractNotification(String eventId) {
        this.eventId = eventId;
    }

    public int compareTo(AbstractNotification o) {
        return o == null ? 1 : CommonUtils.compare(this.getDate(), o.getDate());
    }

    public String getEventId() {
        return this.eventId;
    }

    public abstract Date getDate();

    public abstract String getDescription();

    public abstract String getLabel();

    public Object getToken() {
        return null;
    }
}
