/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.registry;

import com.sun.security.auth.module.NTSystem;
import com.sun.security.auth.module.UnixSystem;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.auth.*;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;

public class BasicWorkspaceSession extends AbstractSMSessionPersistent implements SMSession, SMSessionPrincipal {
    private final DBPWorkspace workspace;
    private String userName;
    private String domainName;

    public BasicWorkspaceSession(@NotNull DBPWorkspace workspace) {
        this.workspace = workspace;
        try {
            if (RuntimeUtils.isWindows()) {
                NTSystem ntSystem = new NTSystem();
                userName = ntSystem.getName();
                domainName = ntSystem.getDomain();
            } else {
                UnixSystem unixSystem = new UnixSystem();
                userName = unixSystem.getUsername();
            }
        } catch (Exception e) {
            // Not supported on this system
        }
        if (CommonUtils.isEmpty(userName)) {
            userName = System.getProperty(StandardConstants.ENV_USER_NAME);
        }
        if (CommonUtils.isEmpty(userName)) {
            userName = "unknown";
        }

        if (CommonUtils.isEmpty(domainName)) {
            if (RuntimeUtils.isWindows()) {
                domainName = System.getenv("USERDOMAIN");
            }
            if (CommonUtils.isEmpty(domainName)) {
                domainName = DBConstants.LOCAL_DOMAIN_NAME;
            }
        }
    }

    @NotNull
    @Override
    public SMAuthSpace getSessionSpace() {
        return workspace;
    }

    @NotNull
    @Override
    public SMSessionContext getSessionContext() {
        return workspace.getAuthContext();
    }

    @Override
    public SMSessionPrincipal getSessionPrincipal() {
        return this;
    }

    @NotNull
    @Override
    public String getSessionId() {
        return workspace.getWorkspaceId();
    }

    @Override
    public boolean isApplicationSession() {
        return true;
    }

    @Nullable
    @Override
    public DBPProject getSingletonProject() {
        return null;
    }

    @Override
    public String getUserDomain() {
        return domainName;
    }

    @Override
    public String getUserName() {
        return userName;
    }
}
