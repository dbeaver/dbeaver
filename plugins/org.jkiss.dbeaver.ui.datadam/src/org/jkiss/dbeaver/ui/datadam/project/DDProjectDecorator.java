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
package org.jkiss.dbeaver.ui.datadam.project;

import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.datadam.DDProductConfig;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIIcons;

/** Replaces the icon of projects connected to the current DataDam account in the Project Navigator. */
public class DDProjectDecorator extends LabelProvider implements ILightweightLabelDecorator {
    public static final String DECORATOR_ID = "org.jkiss.dbeaver.ui.datadam.project.decorator";

    private static final Log log = Log.getLog(DDProjectDecorator.class);

    @Override
    public void decorate(Object element, IDecoration decoration) {
        if (!(element instanceof DBNProject projectNode) || !DDProductConfig.isIntegrationEnabled()) {
            return;
        }
        DDProjectUIManager manager = DDProjectUIManager.getInstance();
        if (!manager.isDDReady()) {
            return;
        }
        try {
            if (manager.getProjectManager().isBound(projectNode.getProject())) {
                decoration.addOverlay(DBeaverIcons.getImageDescriptor(DDTrackingUIIcons.PROJECT_DATADAM), IDecoration.REPLACE);
            }
        } catch (DBException e) {
            log.debug("Error checking DataDam project binding", e);
        }
    }
}
