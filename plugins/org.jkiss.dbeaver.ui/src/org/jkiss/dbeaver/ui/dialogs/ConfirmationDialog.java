/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.PreferenceStoreDelegate;

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

    private boolean hideToggle;

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
        this.hideToggle = toggleMessage == null;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Control dialogArea = super.createDialogArea(parent);
        if (hideToggle) {
            getToggleButton().setVisible(false);
        }
        return dialogArea;
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
        DBPPreferenceStore prefStore = DBWorkbench.getPlatform().getPreferenceStore();
        if (toggleMessage != null) {
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
        }
        ConfirmationDialog dialog = new ConfirmationDialog(
            parent == null ? UIUtils.getActiveWorkbenchShell() : parent,
            title,
            null, // accept the default window icon
            message,
            imageKind,
            getButtonLabels(kind),
            getDefaultIndex(kind, imageKind),
            toggleMessage,
            toggleState);
        dialog.setPrefStore(new PreferenceStoreDelegate(prefStore));
        dialog.setPrefKey(key);
        return dialog.open();
    }

    public static String[] getButtonLabels(int kind) {
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

    public static int getDefaultIndex(int kind, int imageKind) {
        switch (kind) {
            case ERROR:
            case INFORMATION:
            case WARNING:
                return 0;
            case CONFIRM:
                if (imageKind == WARNING) {
                    return 1;
                } else {
                    return 0;
                }
            case QUESTION:
                return 1;
            case QUESTION_WITH_CANCEL: {
                return 2;
            }
            default:
                throw new IllegalArgumentException(
                    "Illegal value for kind in MessageDialog.open()"); //$NON-NLS-1$
        }
    }

    public static boolean confirmAction(ResourceBundle bundle, Shell shell, String id)
    {
        return confirmActionWithParams(bundle, shell, id);
    }

    public static boolean confirmActionWithParams(ResourceBundle bundle, Shell shell, String id, Object ... args)
    {
        return showConfirmDialog(bundle, shell, id, CONFIRM, args) == IDialogConstants.OK_ID;
    }

    public static int showConfirmDialog(ResourceBundle bundle, @Nullable Shell shell, String id, int type, Object ... args)
    {
        return showConfirmDialogEx(bundle, shell, id, type, type, args);
    }

    public static String getSavedPreference(String id) {
        DBPPreferenceStore prefStore = DBWorkbench.getPlatform().getPreferenceStore();
        return prefStore.getString(PREF_KEY_PREFIX + id);
    }

    public static int showConfirmDialogEx(ResourceBundle bundle, Shell shell, String id, int type, int imageType, Object... args)
    {
        String titleKey = getResourceKey(id, RES_KEY_TITLE);
        String messageKey = getResourceKey(id, RES_KEY_MESSAGE);
        String toggleKey = getResourceKey(id, RES_KEY_TOGGLE_MESSAGE);
        String prefKey = PREF_KEY_PREFIX + id;

        String toggleMessage;
        try {
            toggleMessage = bundle.getString(toggleKey);
        } catch (Exception e) {
            toggleMessage = null;
        }

        return open(
            type,
            imageType,
            shell,
            UIUtils.formatMessage(bundle.getString(titleKey), args),
            UIUtils.formatMessage(bundle.getString(messageKey), args),
            toggleMessage == null ? null : UIUtils.formatMessage(toggleMessage, args),
            false,
            prefKey);
    }

    /**
     * Confirmation with disabled NEVER answer.
     */
    public static int showConfirmDialogNoToggle(ResourceBundle bundle, Shell shell, String id, int type, int imageType, Object... args)
    {
        String titleKey = getResourceKey(id, RES_KEY_TITLE);
        String messageKey = getResourceKey(id, RES_KEY_MESSAGE);
        String toggleKey = getResourceKey(id, RES_KEY_TOGGLE_MESSAGE);
        String prefKey = PREF_KEY_PREFIX + id;

        DBPPreferenceStore prefStore = DBWorkbench.getPlatform().getPreferenceStore();
        if (ConfirmationDialog.NEVER.equals(prefStore.getString(prefKey))) {
            prefStore.setToDefault(prefKey);
        }

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
