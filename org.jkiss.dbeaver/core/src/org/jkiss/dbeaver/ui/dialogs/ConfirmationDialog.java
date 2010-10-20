/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.core.DBeaverCore;

import java.util.ResourceBundle;

/**
 * Standard confirmation dialog
 */
public class ConfirmationDialog extends MessageDialogWithToggle {

    public static final String PREF_KEY_PREFIX = "org.jkiss.dbeaver.core.confirm."; //$NON-NLS-1$

    public static final String RES_KEY_TITLE = "title"; //$NON-NLS-1$
    public static final String RES_KEY_MESSAGE = "message"; //$NON-NLS-1$
    public static final String RES_KEY_TOGGLE_MESSAGE = "toggleMessage"; //$NON-NLS-1$

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
        Shell parent,
        String title,
        String message,
        String toggleMessage,
        boolean toggleState,
        String key)
    {
        IPreferenceStore prefStore = DBeaverCore.getInstance().getGlobalPreferenceStore();
        if (ConfirmationDialog.ALWAYS.equals(prefStore.getString(key))) {
            return 0;
        }
        ConfirmationDialog dialog = new ConfirmationDialog(
            parent,
            title,
            null, // accept the default window icon
            message,
            kind,
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
        ResourceBundle bundle = DBeaverActivator.getInstance().getResourceBundle();
        String titleKey = getResourceKey(id, RES_KEY_TITLE);
        String messageKey= getResourceKey(id, RES_KEY_MESSAGE);
        String toggleKey = getResourceKey(id, RES_KEY_TOGGLE_MESSAGE);
        String prefKey = PREF_KEY_PREFIX + id;

        return open(
            CONFIRM,
            shell,
            bundle.getString(titleKey),
            bundle.getString(messageKey),
            bundle.getString(toggleKey),
            false,
            prefKey) == 0;
    }

    public static String getResourceKey(String id, String key)
    {
        return "Confirm." + id + "." + key;  //$NON-NLS-1$
    }
}
