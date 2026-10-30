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
package org.jkiss.dbeaver.model.dashboard.navigator;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.app.DBPResourceHandler;
import org.jkiss.dbeaver.model.dashboard.DashboardConstants;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.utils.RuntimeUtils;

/**
 * DBNDashboard
 */
public class DBNDashboard extends DBNResource {
    private static final DBIcon DASHBOARD_ICON = new DBIcon(RuntimeUtils.makePlatformResourceUrl(
        DashboardConstants.DASHBOARDS_PLUGIN_ID, "icons/dashboard.svg"));

    @Nullable
    private DBPImage image;

    public DBNDashboard(@NotNull DBNNode parentNode, @NotNull IResource resource, @NotNull DBPResourceHandler handler)
        throws DBException, CoreException {
        super(parentNode, resource, handler);
    }

    @NotNull
    @Override
    public DBPImage getResourceNodeIcon() {
        if (image == null) {
            image = DASHBOARD_ICON;
        }
        return image;
    }

}
