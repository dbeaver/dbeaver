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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPAdaptable;
import org.jkiss.utils.CommonUtils;

import java.util.Date;

/**
 * Copied from Mylyn sources
 */
public abstract class AbstractNotification implements Comparable<AbstractNotification>, DBPAdaptable {
    private final String id;

    public AbstractNotification(@NotNull String id) {
        this.id = id;
    }

    public int compareTo(AbstractNotification o) {
        return o == null ? 1 : CommonUtils.compare(this.getDate(), o.getDate());
    }

    @NotNull
    public String getId() {
        return this.id;
    }

    public abstract Date getDate();

    public abstract String getDescription();

    public abstract String getLabel();

    public Object getToken() {
        return null;
    }
}
