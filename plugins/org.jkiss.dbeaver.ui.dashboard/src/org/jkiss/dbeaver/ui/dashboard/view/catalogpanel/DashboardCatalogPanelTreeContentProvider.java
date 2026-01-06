/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.dashboard.view.catalogpanel;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.dashboard.DBDashboardContext;
import org.jkiss.dbeaver.model.dashboard.DBDashboardFolder;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardItemConfiguration;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardProviderDescriptor;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardRegistry;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;
import org.jkiss.utils.ArrayUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class DashboardCatalogPanelTreeContentProvider extends TreeContentProvider {

    private final DBPDataSourceContainer dataSourceContainer;
    private final Function<DashboardItemConfiguration, Boolean> itemFilter;
    private final DBPProject project;

    public DashboardCatalogPanelTreeContentProvider(
            DBPDataSourceContainer dataSourceContainer,
            DBPProject project,
            Function<DashboardItemConfiguration, Boolean> itemFilter
    ) {
        this.dataSourceContainer = dataSourceContainer;
        this.itemFilter = itemFilter;
        this.project = project;
    }

    @Override
    public Object[] getElements(Object inputElement) {
        Object[] elements = super.getElements(inputElement);
        if (elements == null) {
            return new Object[]{};
        }
        List<Object> result = new ArrayList<>();
        for (Object element : elements) {
            Object[] children = getChildren(element);
            if (children.length > 0) {
                result.add(element);
            }
        }

        return result.toArray();
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        try {
            DBDashboardContext context;
            if (dataSourceContainer != null) {
                context = new DBDashboardContext(dataSourceContainer);
            } else {
                context = new DBDashboardContext(project);
            }
            if (parentElement instanceof DBDashboardFolder df) {
                List<DBDashboardFolder> subFolders = df.loadSubFolders(new VoidProgressMonitor(), context);
                List<DashboardItemConfiguration> dashboards = new ArrayList<>(df.loadDashboards(new VoidProgressMonitor(), context));
                dashboards.sort(Comparator.comparing(DashboardItemConfiguration::getTitle));
                return ArrayUtils.concatArrays(subFolders.toArray(), dashboards.toArray());
            } else if (parentElement instanceof DashboardProviderDescriptor dpd) {
                List<Object> children = new ArrayList<>();
                if (dpd.isSupportsFolders()) {
                    try {
                        UIUtils.runInProgressDialog(monitor -> children.addAll(dpd.getInstance().loadRootFolders(monitor, dpd, context)));
                    } catch (InvocationTargetException e) {
                        DBWorkbench.getPlatformUI().showError("Folders load error", null, e.getTargetException());
                    }
                    return children.toArray();
                }
                List<DashboardItemConfiguration> predefineDashboardItemConfigurations =
                        new ArrayList<>(DashboardRegistry.getInstance().getDashboardItems(
                                dpd,
                                dataSourceContainer,
                                false));
                if (itemFilter != null) {
                    predefineDashboardItemConfigurations.removeIf(itemFilter::apply);
                }
                predefineDashboardItemConfigurations.sort(Comparator.comparing(DashboardItemConfiguration::getTitle));
                return predefineDashboardItemConfigurations.toArray();
            }
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Error reading dashboard info", null, e);
        }
        return new Object[0];
    }

    @Override
    public boolean hasChildren(Object element) {
        return element instanceof DashboardProviderDescriptor || element instanceof DBDashboardFolder;
    }
}
