/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

import org.eclipse.jface.dialogs.IDialogLabelKeys;
import org.eclipse.jface.resource.JFaceResources;
import org.jkiss.code.NotNull;

/**
 * Standard labels provided by JFace's resource bundle.
 */
public enum UIStandardLabel {
    OK(IDialogLabelKeys.OK_LABEL_KEY),
    CANCEL(IDialogLabelKeys.CANCEL_LABEL_KEY),
    YES(IDialogLabelKeys.YES_LABEL_KEY),
    NO(IDialogLabelKeys.NO_LABEL_KEY),
    YES_TO_ALL(IDialogLabelKeys.YES_TO_ALL_LABEL_KEY),
    NO_TO_ALL(IDialogLabelKeys.NO_TO_ALL_LABEL_KEY),
    SKIP(IDialogLabelKeys.SKIP_LABEL_KEY),
    STOP(IDialogLabelKeys.STOP_LABEL_KEY),
    ABORT(IDialogLabelKeys.ABORT_LABEL_KEY),
    RETRY(IDialogLabelKeys.RETRY_LABEL_KEY),
    IGNORE(IDialogLabelKeys.IGNORE_LABEL_KEY),
    PROCEED(IDialogLabelKeys.PROCEED_LABEL_KEY),
    OPEN(IDialogLabelKeys.OPEN_LABEL_KEY),
    CLOSE(IDialogLabelKeys.CLOSE_LABEL_KEY),
    SHOW_DETAILS(IDialogLabelKeys.SHOW_DETAILS_LABEL_KEY),
    HIDE_DETAILS(IDialogLabelKeys.HIDE_DETAILS_LABEL_KEY),
    BACK(IDialogLabelKeys.BACK_LABEL_KEY),
    NEXT(IDialogLabelKeys.NEXT_LABEL_KEY),
    FINISH(IDialogLabelKeys.FINISH_LABEL_KEY),
    HELP(IDialogLabelKeys.HELP_LABEL_KEY),
    HELP_TOOLTIP("helpToolTip"),
    RESTORE_DEFAULTS("defaults"),
    APPLY("apply"),
    APPLY_AND_CLOSE("PreferencesDialog.okButtonLabel"),
    BROWSE("openBrowse"),
    CHANGE("openChange"),
    COPY("copy"),
    ERROR("error"),
    WARNING("warning"),
    INFO("info"),
    QUESTION("question");

    private final String resourceKey;

    UIStandardLabel(@NotNull String resourceKey) {
        this.resourceKey = resourceKey;
    }

    /**
     * Returns the localized JFace label, preserving any mnemonic markers.
     */
    @NotNull
    public String getLabel() {
        return JFaceResources.getString(resourceKey);
    }
}
