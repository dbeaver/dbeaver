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

import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.app.DBACertificateStorage;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.impl.app.AbstractApplication;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.qm.QMRegistry;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.BasePlatformImpl;

import java.net.URISyntaxException;
import java.nio.file.Path;

final class LSPlatform extends BasePlatformImpl {
    @Nullable
    private DBPWorkspace workspace;

    @Override
    protected void initialize() {
        // Added in order for DI to work. See org.jkiss.dbeaver.model.impl.app.ApplicationWorkbenchImpl.platformInstance
        // and org.jkiss.dbeaver.utils.RuntimeUtils.injectComponentReferences
        super.initialize();
    }

    @Override
    protected LSBundleActivator getProductPlugin() {
        return LSBundleActivator.instance();
    }

    @NotNull
    @Override
    public DBPApplication getApplication() {
        return AbstractApplication.getInstance();
    }

    @NotNull
    @Override
    public DBPWorkspace getWorkspace() {
        if (workspace == null) {
            try {
                workspace = new LSWorkspace(this, Path.of(Platform.getInstanceLocation().getURL().toURI()));
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return workspace;
    }

    @NotNull
    @Override
    public QMRegistry getQueryManager() {
        throw new RuntimeException();
    }

    @NotNull
    @Override
    public DBPPreferenceStore getPreferenceStore() {
        return getProductPlugin().preferenceStore();
    }

    @NotNull
    @Override
    public DBACertificateStorage getCertificateStorage() {
        throw new RuntimeException();
    }

    @NotNull
    @Override
    public Path getTempFolder(@NotNull DBRProgressMonitor monitor, @NotNull String name) {
        throw new RuntimeException();
    }

    @Override
    public boolean isShuttingDown() {
        return false;
    }
}
