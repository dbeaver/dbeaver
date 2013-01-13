package org.jkiss.dbeaver.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.osgi.framework.Bundle;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;

/**
 * Tray icon handler
 */
public class TrayIconHandler {

    static final Log log = LogFactory.getLog(TrayIconHandler.class);

    // AWT tray icon. SWT TrayItem do not support displayMessage function
    private TrayIcon trayItem;

    public void show()
    {
        if (trayItem != null) {
            return;
        }
        Bundle coreBundle = DBeaverActivator.getInstance().getBundle();

        URL logoURL = coreBundle.getEntry(DBIcon.DBEAVER_LOGO.getPath());
        trayItem = new TrayIcon(Toolkit.getDefaultToolkit().getImage(logoURL));
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
            show();
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
