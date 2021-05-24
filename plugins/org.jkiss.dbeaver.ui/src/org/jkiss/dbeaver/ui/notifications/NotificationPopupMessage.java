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
package org.jkiss.dbeaver.ui.notifications;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.utils.GeneralUtils;

public class NotificationPopupMessage extends DatabaseNotificationPopup {

    private final DBPDataSource dataSource;
    private String messageText;
    private int iconType;

    public NotificationPopupMessage(DBPDataSource dataSource, String text, int iconType) {
        super(PlatformUI.getWorkbench().getDisplay().getActiveShell());

        this.dataSource = dataSource;
        this.messageText = text;
        this.iconType = iconType;
    }

    @Override
    protected String getPopupShellTitle() {
        return dataSource == null ? GeneralUtils.getProductName() : dataSource.getContainer().getName();
    }

    @Override
    protected Image getPopupShellImage(int maximumHeight) {
        boolean hasIcon = iconType == SWT.ICON_ERROR || iconType == SWT.ICON_WARNING || iconType == SWT.ICON_QUESTION;
        if (hasIcon) {
            switch (iconType) {
                case SWT.ICON_ERROR: return DBeaverIcons.getImage(DBIcon.STATUS_ERROR);
                case SWT.ICON_WARNING: return DBeaverIcons.getImage(DBIcon.STATUS_WARNING);
                default:
                    return DBeaverIcons.getImage(DBIcon.STATUS_INFO);
            }
        }
        return null;
    }

    @Override
    protected void createContentArea(Composite composite)
    {
        Label textLabel = new Label(composite, SWT.NONE);
        textLabel.setText(messageText);
    }

    public static void showMessage(DBPDataSource dataSource, String text, long delayClose, int iconType) {
        Display.getDefault().syncExec(() -> {
            NotificationPopupMessage popup = new NotificationPopupMessage(dataSource, text, iconType);
            if (delayClose > 0) {
                popup.setDelayClose(delayClose);
            }

            popup.open();
        });
    }

}