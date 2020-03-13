/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.dashboard.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.ui.dashboard.control.DashboardRenderer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardConstants;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardDataType;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardMapQuery;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardViewType;
import org.jkiss.utils.CommonUtils;

/**
 * DashboardMapQueryDescriptor
 */
public class DashboardMapQueryDescriptor extends AbstractContextDescriptor implements DashboardMapQuery
{
    private String id;
    private String queryText;
    private long updatePeriod;

    DashboardMapQueryDescriptor(
        IConfigurationElement config)
    {
        super(config);

        this.id = config.getAttribute("id");
        // FIXME: use getValueAsIs because getValue fails in multi-language environment
        this.queryText = config.getValueAsIs();
        this.updatePeriod = CommonUtils.toInt(config.getAttribute("updatePeriod"), DashboardConstants.DEF_DASHBOARD_UPDATE_PERIOD);
    }

    @Override
    @NotNull
    public String getId() {
        return id;
    }

    @Override
    public long getUpdatePeriod() {
        return updatePeriod;
    }

    @Override
    public String getQueryText() {
        return queryText;
    }

    @Override
    public String toString() {
        return id;
    }

}
