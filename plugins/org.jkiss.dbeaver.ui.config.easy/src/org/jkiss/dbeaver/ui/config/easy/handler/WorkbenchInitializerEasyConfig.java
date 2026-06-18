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
package org.jkiss.dbeaver.ui.config.easy.handler;

import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.IWorkbenchWindowInitializer;
import org.jkiss.dbeaver.ui.config.easy.EasyConfigWizardDialog;
import org.jkiss.utils.CommonUtils;

public final class WorkbenchInitializerEasyConfig implements IWorkbenchWindowInitializer {
    private static final boolean SHOW_ON_STARTUP = CommonUtils.getBoolean(System.getProperty("dbeaver.show.easy.config.on.startup"));

    @Override
    public void initializeWorkbenchWindow(@NotNull IWorkbenchWindowConfigurer configurer) {
        if (SHOW_ON_STARTUP) {
            new EasyConfigWizardDialog(configurer.getWindow()).open();
        }
    }
}
