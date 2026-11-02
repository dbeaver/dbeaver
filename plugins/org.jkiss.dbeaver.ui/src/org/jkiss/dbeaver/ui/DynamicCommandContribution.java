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
package org.jkiss.dbeaver.ui;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.services.IServiceLocator;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

public class DynamicCommandContribution extends CompoundContributionItem implements IWorkbenchContribution, IExecutableExtension {

    private String commandId;
    private IServiceLocator serviceLocator;

    @Override
    public void setInitializationData(@NotNull IConfigurationElement config, @Nullable String propertyName, @Nullable Object data) {
        commandId = (String) data;
    }

    @Override
    public void initialize(@NotNull IServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    @NotNull
    @Override
    protected IContributionItem[] getContributionItems() {
        return new IContributionItem[] {
            ActionUtils.makeCommandContribution(serviceLocator, commandId)
        };
    }
}
