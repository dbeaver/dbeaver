/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.impl.app;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.app.DBPApplicationWorkbench;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.meta.ComponentReference;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;
import org.jkiss.dbeaver.runtime.ui.console.ConsoleUserInterface;

import java.util.Objects;

/**
 * ApplicationWorkbenchImpl
 */
public class ApplicationWorkbenchImpl implements DBPApplicationWorkbench {
    private static final ConsoleUserInterface CONSOLE_USER_INTERFACE = new ConsoleUserInterface();

    @ComponentReference(required = true, postProcessMethod = "initialize")
    public DBPPlatform platformInstance;
    @ComponentReference(postProcessMethod = "initialize")
    public DBPPlatformUI platformUIInstance;

    public ApplicationWorkbenchImpl() {
    }

    @NotNull
    @Override
    public DBPPlatform getPlatform() {
        return platformInstance;
    }

    @NotNull
    @Override
    public DBPPlatformUI getPlatformUI() {
        return Objects.requireNonNullElse(platformUIInstance, CONSOLE_USER_INTERFACE);
    }

}
