/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.utils.GeneralUtils;
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

    public static boolean isSupported() {
        return SystemTray.isSupported();
    }

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
        {
            PopupMenu popupMenu = new PopupMenu();
            MenuItem showItem = new MenuItem("Show DBeaver");
            showItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showMainWindow();
                }
            });
            popupMenu.add(showItem);
            trayItem.setPopupMenu(popupMenu);
        }
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
                if (e.getButton() == MouseEvent.BUTTON1) {
                    showMainWindow();
                }
            }
        });


        // Add tooltip and menu to tray icon
        trayItem.setToolTip(GeneralUtils.getProductTitle());

        // Add the trayIcon to system tray/notification
        // area
        try {
            SystemTray.getSystemTray().add(trayItem);
        } catch (AWTException e) {
            log.error(e);
        }
    }

    private void showMainWindow() {
        UIUtils.asyncExec(new Runnable() {
            @Override
            public void run() {
                Shell activeShell = UIUtils.getActiveWorkbenchShell();
                if (activeShell != null){
                    if (activeShell.getMinimized()) {
                        activeShell.setMinimized(false);
                    }
                    activeShell.forceActive();
                }
            }
        });
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
        try {
            if (trayItem == null) {
                try {
                    show();
                } catch (Exception e) {
                    log.warn("Can't show tray item", e);
                    return;
                }
            }
            TrayIcon.MessageType type = switch (status) {
                case IStatus.INFO -> TrayIcon.MessageType.INFO;
                case IStatus.ERROR -> TrayIcon.MessageType.ERROR;
                case IStatus.WARNING -> TrayIcon.MessageType.WARNING;
                default -> TrayIcon.MessageType.NONE;
            };
            trayItem.displayMessage(GeneralUtils.getProductTitle(), message, type);
        } catch (Throwable e) {
            log.error("Error showing tray notification", e);
        }
    }

}
