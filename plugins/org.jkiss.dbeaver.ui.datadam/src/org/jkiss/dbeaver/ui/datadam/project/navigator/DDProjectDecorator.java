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
package org.jkiss.dbeaver.ui.datadam.project.navigator;

import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.datadam.DDProductConfig;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectManager.BindingStatus;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIIcons;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectUIManager;

/** Replaces the icon of projects connected to the current DataDam account in the Project Navigator. */
public class DDProjectDecorator extends LabelProvider implements ILabelDecorator {
    public static final String DECORATOR_ID = "org.jkiss.dbeaver.ui.datadam.project.decorator";

    private static final Log log = Log.getLog(DDProjectDecorator.class);

    @Nullable
    @Override
    public Image decorateImage(@Nullable Image image, @Nullable Object element) {
        if (!(element instanceof DBNProject projectNode) || !DDProductConfig.isIntegrationEnabled()) {
            return null;
        }
        DDProjectUIManager manager = DDProjectUIManager.getInstance();
        if (!manager.isProjectSyncReady()) {
            return null;
        }
        try {
            if (manager.getProjectManager().getBindingStatus(projectNode.getProject()) == BindingStatus.CURRENT_ACCOUNT) {
                return DBeaverIcons.getImage(DDTrackingUIIcons.PROJECT_DATADAM);
            }
        } catch (DBException | IllegalStateException e) {
            log.debug("Error checking DataDam project binding", e);
        }
        return null;
    }

    @Nullable
    @Override
    public String decorateText(@Nullable String text, @Nullable Object element) {
        return null;
    }
}
