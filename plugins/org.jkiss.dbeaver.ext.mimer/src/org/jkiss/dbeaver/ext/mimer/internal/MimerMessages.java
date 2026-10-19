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
package org.jkiss.dbeaver.ext.mimer.internal;

import org.eclipse.osgi.util.NLS;

/**
 * NLS message bundle for hardcoded Java-side UI text that lives in the model plugin's {@code
 * edit} package - a handful of classes there (see {@code MimerCascadeDropUtil}/{@code
 * MimerFileRenameUtil}) call {@code DBWorkbench.getPlatformUI()} directly to confirm a
 * destructive action before running it, the same way core's own edit-layer classes do. That text
 * is exactly the same category {@code org.jkiss.dbeaver.ext.mimer.ui.internal.MimerUIMessages}
 * already covers in the {@code .ui} plugin, just unreachable from here - the model plugin has no
 * dependency on {@code .ui} (and shouldn't gain one just for this). Named {@code MimerMessages},
 * not {@code MimerUIMessages}, matching the plain-model-plugin naming convention sibling plugins
 * already use for this (e.g. {@code OracleMessages}, {@code PostgreSQLMessages} - no "UI" suffix
 * on the model-plugin-level bundle).
 *
 * @author Mimer Information Technology
 */
public final class MimerMessages extends NLS {
    private static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.mimer.internal.MimerMessages"; //$NON-NLS-1$

    // MimerCascadeDropUtil
    public static String action_cascade_drop_title;
    public static String action_cascade_drop_message;
    public static String action_cascade_drop_confirm_choice;
    public static String action_cascade_drop_cancel_choice;
    public static String action_cascade_drop_remember_option;

    // MimerFileRenameUtil
    public static String action_file_rename_title;
    public static String action_file_rename_message;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, MimerMessages.class);
    }

    private MimerMessages() {
    }
}
