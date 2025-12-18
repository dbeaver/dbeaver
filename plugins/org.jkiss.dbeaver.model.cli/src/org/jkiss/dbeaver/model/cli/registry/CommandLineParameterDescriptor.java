/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.eclipse.core.runtime.Platform;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Bundle;

public class CommandLineParameterDescriptor {
    private final boolean exitAfterExecute;
    private final boolean exclusiveMode;
    private final boolean forceNewInstance;
    private final Class<?> implClass;


    public CommandLineParameterDescriptor(IConfigurationElement config) throws Exception {
        this.exitAfterExecute = CommonUtils.toBoolean(config.getAttribute("exitAfterExecute"));
        this.exclusiveMode = CommonUtils.toBoolean(config.getAttribute("exclusiveMode"));
        this.forceNewInstance = CommonUtils.toBoolean(config.getAttribute("forceNewInstance"));
        Bundle cBundle = Platform.getBundle(config.getContributor().getName());
        this.implClass = cBundle.loadClass(config.getAttribute("handler"));
    }


    public boolean isExclusiveMode() {
        return exclusiveMode;
    }

    public boolean isExitAfterExecute() {
        return exitAfterExecute;
    }

    public boolean isForceNewInstance() {
        return forceNewInstance;
    }

    public Class<?> getImplClass() {
        return implClass;
    }
}

