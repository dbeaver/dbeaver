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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.menu.MToolControl;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;

import java.util.HashSet;

/** Removes restored standalone trims whose items are now hosted by the status-bar extension point. */
public class StatusBarModelProcessor {
    @Execute
    public void process(@NotNull MApplication application, @NotNull EModelService modelService) {
        var itemIds = new HashSet<String>();
        for (var config : Platform.getExtensionRegistry().getConfigurationElementsFor(DBeaverStatusBarTrim.EXTENSION_ID)) {
            if ("item".equals(config.getName())) {
                String id = config.getAttribute(RegistryConstants.ATTR_ID);
                if (!CommonUtils.isEmpty(id)) {
                    itemIds.add(id);
                }
            }
        }
        for (MToolControl control : modelService.findElements(application, null, MToolControl.class, null)) {
            if (itemIds.contains(control.getElementId()) && control.getParent() != null) {
                control.getParent().getChildren().remove(control);
            }
        }
    }
}
