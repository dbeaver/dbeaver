/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui;

import org.eclipse.core.runtime.IStatus;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

/**
 * Tray icon handler
 */
public class TrayIconHandler {

    private static final Log log = Log.getLog(TrayIconHandler.class);

    // AWT tray icon. SWT TrayItem do not support displayMessage function
    private TrayIcon trayItem;

    public void show()
    {
        if (trayItem != null) {
            return;
        }

        File logoFile;
        try {
            logoFile = RuntimeUtils.getPlatformFile(UIIcon.DBEAVER_LOGO.getLocation());
        } catch (IOException e) {
            log.error(e);
            return;
        }
        trayItem = new TrayIcon(Toolkit.getDefaultToolkit().getImage(logoFile.getAbsolutePath()));
        trayItem.setImageAutoSize(true);
        trayItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {

            }
        });
        trayItem.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e)
            {
            }
        });


        // Add tooltip and menu to tray icon
        trayItem.setToolTip(DBeaverCore.getProductTitle());

        // Add the trayIcon to system tray/notification
        // area
        try {
            SystemTray.getSystemTray().add(trayItem);
        } catch (AWTException e) {
            log.error(e);
        }
    }

    public void hide()
    {
        if (trayItem != null) {
            SystemTray.getSystemTray().remove(trayItem);
            trayItem = null;
        }
    }

    public void notify(String message, int status)
    {
        if (trayItem == null) {
            try {
                show();
            } catch (Exception e) {
                log.warn("Can't show tray item", e);
                return;
            }
        }
        TrayIcon.MessageType type;
        switch (status) {
            case IStatus.INFO: type = TrayIcon.MessageType.INFO; break;
            case IStatus.ERROR: type = TrayIcon.MessageType.ERROR; break;
            case IStatus.WARNING: type = TrayIcon.MessageType.WARNING; break;
            default: type = TrayIcon.MessageType.NONE; break;
        }
        trayItem.displayMessage(DBeaverCore.getProductTitle(), message, type);
    }

}
