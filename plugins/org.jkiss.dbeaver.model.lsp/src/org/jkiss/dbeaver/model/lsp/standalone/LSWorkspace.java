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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.impl.app.BaseWorkspaceImpl;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.nio.file.Path;
import java.util.List;

final class LSWorkspace extends BaseWorkspaceImpl {
    LSWorkspace(DBPPlatform platform, Path workspacePath) {
        super(platform, workspacePath);
    }

    @NotNull
    @Override
    public String getWorkspaceId() {
        throw new RuntimeException();
    }

    @NotNull
    @Override
    public List<? extends DBPProject> getProjects() {
        return DBWorkbench.getPlatform().getWorkspace().getProjects();
    }

    @Nullable
    @Override
    public DBPProject getProject(@NotNull String projectName) {
        throw new RuntimeException();
    }

    @Override
    public void initializeProjects() {
        throw new RuntimeException();
    }
}
