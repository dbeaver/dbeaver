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
package org.jkiss.dbeaver.registry;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.rcp.DBeaverNature;
import org.jkiss.dbeaver.model.rcp.RCPProject;
import org.jkiss.utils.ArrayUtils;

public class DesktopDataSourceRegistry<T extends DataSourceDescriptor> extends DataSourceRegistry<T> {

    private static final Log log = Log.getLog(DesktopDataSourceRegistry.class);

    public DesktopDataSourceRegistry(DBPProject project) {
        super(project);
    }

    public DesktopDataSourceRegistry(@NotNull DBPProject project, DataSourceConfigurationManager configurationManager, @NotNull DBPPreferenceStore preferenceStore) {
        super(project, configurationManager, preferenceStore);
    }

    protected void updateProjectNature() {
        if (isMultiUser() || !(getProject() instanceof RCPProject rcpProject)) {
            return;
        }
        try {
            IProject eclipseProject = rcpProject.getEclipseProject();
            if (eclipseProject != null) {
                final IProjectDescription description = eclipseProject.getDescription();
                if (description != null) {
                    String[] natureIds = description.getNatureIds();
                    if (getDataSourceCount() > 0) {
                        // Add nature
                        if (!ArrayUtils.contains(natureIds, DBeaverNature.NATURE_ID)) {
                            description.setNatureIds(ArrayUtils.add(String.class, natureIds, DBeaverNature.NATURE_ID));
                            try {
                                eclipseProject.setDescription(description, new NullProgressMonitor());
                            } catch (CoreException e) {
                                log.debug("Can't set project nature", e);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug(e);
        }
    }

}
