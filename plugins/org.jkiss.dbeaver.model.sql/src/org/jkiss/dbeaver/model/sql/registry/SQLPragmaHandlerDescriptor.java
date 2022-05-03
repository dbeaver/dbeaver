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
package org.jkiss.dbeaver.model.sql.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.sql.SQLPragmaHandler;

public class SQLPragmaHandlerDescriptor extends AbstractDescriptor {
    private final String id;
    private final ObjectType type;

    protected SQLPragmaHandlerDescriptor(IConfigurationElement config) {
        super(config);

        this.id = config.getAttribute("id");
        this.type = new ObjectType(config.getAttribute("class"));
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public SQLPragmaHandler createHandler() throws DBException {
        return type.createInstance(SQLPragmaHandler.class);
    }
}
