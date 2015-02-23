/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.ResourceBundle;

/**
 * Standard confirmation dialog
 */
public class ConfirmationDialog extends MessageDialogWithToggle {

    public static final String PREF_KEY_PREFIX = "org.jkiss.dbeaver.core.confirm."; //$NON-NLS-1$

    public static final String RES_KEY_TITLE = "title"; //$NON-NLS-1$
    public static final String RES_KEY_MESSAGE = "message"; //$NON-NLS-1$
    public static final String RES_KEY_TOGGLE_MESSAGE = "toggleMessage"; //$NON-NLS-1$
    public static final String RES_CONFIRM_PREFIX = "confirm_"; //$NON-NLS-1$

    public ConfirmationDialog(
        Shell parentShell,
        String dialogTitle,
        Image image,
        String message,
        int dialogImageType,
        String[] dialogButtonLabels,
        int defaultIndex,
        String toggleMessage,
        boolean toggleState)
    {
        super(parentShell, dialogTitle, image, message, dialogImageType, dialogButtonLabels, defaultIndex, toggleMessage, toggleState);
    }

    public static int open(
        int kind,
        int imageKind,
        Shell parent,
        String title,
        String message,
        String toggleMessage,
        boolean toggleState,
        String key)
    {
        IPreferenceStore prefStore = DBeaverCore.getGlobalPreferenceStore();
        if (ConfirmationDialog.ALWAYS.equals(prefStore.getString(key))) {
            if (kind == QUESTION || kind == QUESTION_WITH_CANCEL) {
                return IDialogConstants.YES_ID;
            } else {
                return IDialogConstants.OK_ID;
            }
        } else if (ConfirmationDialog.NEVER.equals(prefStore.getString(key))) {
            if (kind == QUESTION || kind == QUESTION_WITH_CANCEL) {
                return IDialogConstants.NO_ID;
            } else {
                return IDialogConstants.CANCEL_ID;
            }
        }
        ConfirmationDialog dialog = new ConfirmationDialog(
            parent == null ? DBeaverUI.getActiveWorkbenchShell() : parent,
            title,
            null, // accept the default window icon
            message,
            imageKind,
            getButtonLabels(kind),
            0,
            toggleMessage,
            toggleState);
        dialog.setPrefStore(prefStore);
        dialog.setPrefKey(key);
        return dialog.open();
    }

    static String[] getButtonLabels(int kind) {
        switch (kind) {
        case ERROR:
        case INFORMATION:
        case WARNING:
            return new String[] { IDialogConstants.OK_LABEL };
        case CONFIRM:
            return new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL };
        case QUESTION:
            return new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL };
        case QUESTION_WITH_CANCEL: {
            return new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL };
        }
        default:
            throw new IllegalArgumentException(
                    "Illegal value for kind in MessageDialog.open()"); //$NON-NLS-1$
        }
    }

    public static boolean confirmAction(Shell shell, String id)
    {
        return confirmActionWithParams(shell, id);
    }

    public static boolean confirmActionWithParams(Shell shell, String id, Object ... args)
    {
        return showConfirmDialog(shell, id, CONFIRM, args) == 0;
    }

    public static int showConfirmDialog(@Nullable Shell shell, String id, int type, Object ... args)
    {
        return showConfirmDialog(shell, id, type, type, args);
    }

    public static int showConfirmDialog(Shell shell, String id, int type, int imageType, Object ... args)
    {
        ResourceBundle bundle = DBeaverActivator.getResourceBundle();
        String titleKey = getResourceKey(id, RES_KEY_TITLE);
        String messageKey = getResourceKey(id, RES_KEY_MESSAGE);
        String toggleKey = getResourceKey(id, RES_KEY_TOGGLE_MESSAGE);
        String prefKey = PREF_KEY_PREFIX + id;

        return open(
            type,
            imageType,
            shell,
            UIUtils.formatMessage(bundle.getString(titleKey), args),
            UIUtils.formatMessage(bundle.getString(messageKey), args),
            UIUtils.formatMessage(bundle.getString(toggleKey), args),
            false,
            prefKey);
    }

    public static String getResourceKey(String id, String key)
    {
        return RES_CONFIRM_PREFIX + id + "_" + key;  //$NON-NLS-1$
    }
}
