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

package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.PreferenceStoreDelegate;
import org.jkiss.dbeaver.ui.registry.ConfirmationRegistry;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

/**
 * Standard confirmation dialog
 */
public class ConfirmationDialog extends MessageDialogWithToggle {

    public static final String PREF_KEY_PREFIX = "org.jkiss.dbeaver.core.confirm."; //$NON-NLS-1$

    private final boolean hideToggle;

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

    @Override
    protected void initializeBounds() {
        super.initializeBounds();
    }

    /**
     * Retrieves persisted confirmation state for the given key.
     *
     * @param id   identifier of a confirmation
     * @param kind kind of the confirmation
     * @return {@code true} if the persisted answer is "okay" or "yes",
     * {@code false} if the persisted answer is "no",
     * or {@code null} is no persisted answer is present
     */
    @Nullable
    public static Boolean getPersistedState(@NotNull String id, int kind) {
        String key = ConfirmationDialog.PREF_KEY_PREFIX + id;
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        if (ConfirmationDialog.ALWAYS.equals(store.getString(key))) {
            return true;
        } else if (ConfirmationDialog.NEVER.equals(store.getString(key))) {
            // These dialog all have OK and maybe CANCEL buttons.
            // It makes no sense to return CANCEL_ID here as it's not a valid decision like YES or NO
            return kind != QUESTION && kind != QUESTION_WITH_CANCEL;
        } else {
            return null;
        }
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
                    // These dialog all have OK and maybe CANCEL buttons.
                    // It makes no sense to return CANCEL_ID here as it's not a valid decision like YES or NO
                    return IDialogConstants.OK_ID;
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
        //$NON-NLS-1$
        return switch (kind) {
            case ERROR, INFORMATION, WARNING -> new String[]{IDialogConstants.OK_LABEL};
            case CONFIRM -> RuntimeUtils.isMacOS() ?
                new String[]{IDialogConstants.CANCEL_LABEL, IDialogConstants.OK_LABEL} :
                new String[]{IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL};
            case QUESTION -> RuntimeUtils.isMacOS() ?
                new String[]{IDialogConstants.NO_LABEL, IDialogConstants.YES_LABEL} :
                new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL};
            case QUESTION_WITH_CANCEL -> RuntimeUtils.isMacOS() ?
                new String[]{IDialogConstants.CANCEL_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.YES_LABEL } :
                new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL};
            default -> throw new IllegalArgumentException(
                "Illegal value for kind in MessageDialog.open()"); //$NON-NLS-1$
        };
    }

    public static int getDefaultIndex(int kind, int imageKind) {
        switch (kind) {
            case ERROR:
            case INFORMATION:
            case WARNING:
                return 0;
            case CONFIRM:
                if (imageKind == WARNING) {
                    return RuntimeUtils.isMacOS() ? 0 : 1;
                } else {
                    return RuntimeUtils.isMacOS() ? 1 : 0;
                }
            case QUESTION:
                return RuntimeUtils.isMacOS() ? 0 : 1;
            case QUESTION_WITH_CANCEL: {
                return RuntimeUtils.isMacOS() ? 0 : 2;
            }
            default:
                throw new IllegalArgumentException(
                    "Illegal value for kind in MessageDialog.open()"); //$NON-NLS-1$
        }
    }

    public static int confirmAction(@Nullable Shell shell, @NotNull String id, int type, @NotNull Object... args) {
        return ConfirmationRegistry.getInstance().confirmAction(shell, id, type, -1, args);
    }

    public static int confirmAction(@Nullable Shell shell, int imageType, @NotNull String id, int type, @NotNull Object... args) {
        return ConfirmationRegistry.getInstance().confirmAction(shell, id, type, imageType, args);
    }

    public static String getSavedPreference(String id) {
        DBPPreferenceStore prefStore = DBWorkbench.getPlatform().getPreferenceStore();
        return prefStore.getString(PREF_KEY_PREFIX + id);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        super.buttonPressed(buttonId);
        IPreferenceStore prefStore = getPrefStore();
        String prefKey = getPrefKey();

        if (buttonId != IDialogConstants.CANCEL_ID && getToggleState() && prefStore != null && CommonUtils.isNotEmpty(prefKey)) {
            if (buttonId == IDialogConstants.NO_ID) {
                prefStore.setValue(prefKey, ConfirmationDialog.NEVER);
            } else {
                prefStore.setValue(prefKey, ConfirmationDialog.ALWAYS);
            }
        }
    }
}
