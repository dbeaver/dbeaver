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
import org.jkiss.dbeaver.ext.mimer.model.MimerSystemPrivilege;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Collects the privilege type and Grant Option before a new Mimer SQL system privilege grant is
 * created. Unlike every other privilege configurator in this plugin, the create page also needs
 * the container (the User/Group/Program the privilege is being granted to) directly - {@link
 * MimerSystemPrivilege} has no owner reference of its own to derive it from at construction time.
 *
 * @author Mimer Information Technology
 */
public class MimerSystemPrivilegeConfigurator extends AbstractMimerConfigurator<MimerSystemPrivilege> {

    @NotNull
    @Override
    protected MimerCreatePage createPage(@NotNull MimerSystemPrivilege privilege, @Nullable Object container) {
        return new MimerCreateSystemPrivilegePage(privilege, (DBSObject) container);
    }
}
