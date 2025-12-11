/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp
 *
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of DBeaver Corp and its suppliers, if any.
 * The intellectual and technical concepts contained
 * herein are proprietary to DBeaver Corp and its suppliers
 * and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from DBeaver Corp.
 */
package org.jkiss.dbeaver.model.lsp.standalone;

import org.eclipse.core.runtime.Plugin;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;

/**
 * @implNote This class is public so OSGi can instantiate it.
 */
public final class LSBundleActivator extends Plugin {
    private static LSBundleActivator instance = null;

    @Nullable
    private DBPPreferenceStore preferenceStore;

    /**
     * @implNote This constructor is public so OSGi can instantiate it.
     */
    public LSBundleActivator() {
        saveInstance();
    }

    // Tricks linters that are afraid of `this` escaping from constructor
    private void saveInstance() {
        instance = this;
    }

    static LSBundleActivator instance() {
        return instance;
    }

    @NotNull
    DBPPreferenceStore preferenceStore() {
        if (preferenceStore == null) {
            preferenceStore = new BundlePreferenceStore(getBundle());
        }
        return preferenceStore;
    }
}
