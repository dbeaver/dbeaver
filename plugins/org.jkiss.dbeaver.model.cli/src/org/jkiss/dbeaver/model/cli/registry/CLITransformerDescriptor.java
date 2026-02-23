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
package org.jkiss.dbeaver.model.cli.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.utils.CommonUtils;
import picocli.CommandLine;

public class CLITransformerDescriptor extends AbstractDescriptor {
    @NotNull
    private final ObjectType transformer;
    @Nullable
    private final ObjectType command;

    public CLITransformerDescriptor(IConfigurationElement config) throws Exception {
        super(config);
        this.transformer = new ObjectType(config, "transformer");
        if (CommonUtils.isNotEmpty(config.getAttribute("command"))) {
            this.command = new ObjectType(config, "command");
        } else {
            this.command = null;
        }
    }

    @Nullable
    public Class<?> getCommandClass() {
        return command != null ? command.getImplClass() : null;
    }

    @NotNull
    public CommandLine.IModelTransformer getTransformer() {
        try {
            return transformer.createInstance(CommandLine.IModelTransformer.class);
        } catch (DBException e) {
            throw new IllegalStateException("Can not create transformer '" + transformer.getImplName() + "'", e);
        }
    }
}

