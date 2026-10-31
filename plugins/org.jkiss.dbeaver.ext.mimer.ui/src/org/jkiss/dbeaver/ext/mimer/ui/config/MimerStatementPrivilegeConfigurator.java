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
package org.jkiss.dbeaver.ext.mimer.ui.config;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mimer.model.MimerStatementPrivilege;

/**
 * Collects the grantee ident before a new Mimer SQL statement EXECUTE privilege is created.
 *
 * @author Mimer Information Technology
 */
public class MimerStatementPrivilegeConfigurator extends AbstractMimerConfigurator<MimerStatementPrivilege> {

    @NotNull
    @Override
    protected MimerCreatePage createPage(@NotNull MimerStatementPrivilege privilege, @Nullable Object container) {
        return new MimerCreateStatementPrivilegePage(privilege);
    }
}
