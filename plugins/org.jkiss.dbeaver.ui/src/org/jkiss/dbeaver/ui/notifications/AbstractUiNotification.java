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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

/**
 * Copied from Mylyn sources
 */
public abstract class AbstractUiNotification extends AbstractNotification {
    public AbstractUiNotification(@NotNull String id) {
        super(id);
    }

    public abstract Image getNotificationImage();

    public abstract Image getNotificationKindImage();

    @Nullable
    public abstract NotificationSoundProvider getNotificationSoundProvider();

    public abstract void open();
}
