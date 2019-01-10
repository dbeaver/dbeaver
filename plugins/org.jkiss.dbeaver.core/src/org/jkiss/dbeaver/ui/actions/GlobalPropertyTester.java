/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.actions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.IPluginService;
import org.jkiss.dbeaver.ui.ActionUtils;

/**
 * GlobalPropertyTester
 */
public class GlobalPropertyTester extends PropertyTester {
    //static final Log log = LogFactory.get vLog(ObjectPropertyTester.class);

    public static final String NAMESPACE = "org.jkiss.dbeaver.core.global";
    public static final String PROP_STANDALONE = "standalone";
    public static final String PROP_HAS_ACTIVE_PROJECT = "hasActiveProject";
    public static final String PROP_HAS_MULTI_PROJECTS = "hasMultipleProjects";

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        switch (property) {
            case PROP_HAS_MULTI_PROJECTS:
                return DBWorkbench.getPlatform().getLiveProjects().size() > 1;
            case PROP_HAS_ACTIVE_PROJECT:
                return DBWorkbench.getPlatform().getProjectManager().getActiveProject() != null;
            case PROP_STANDALONE:
                return DBWorkbench.getPlatform().getApplication().isStandalone();
        }
        return false;
    }

    public static void firePropertyChange(String propName)
    {
        ActionUtils.evaluatePropertyState(NAMESPACE + "." + propName);
    }

    public static class ResourceListener implements IPluginService, IResourceChangeListener {

        @Override
        public void activateService() {
            ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
        }

        @Override
        public void deactivateService() {
            ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        }

        @Override
        public void resourceChanged(IResourceChangeEvent event) {
            if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
                for (IResourceDelta childDelta : event.getDelta().getAffectedChildren()) {
                    if (childDelta.getResource() instanceof IProject) {
                        if (childDelta.getKind() == IResourceDelta.ADDED || childDelta.getKind() == IResourceDelta.REMOVED) {
                            firePropertyChange(GlobalPropertyTester.PROP_HAS_MULTI_PROJECTS);
                        }
                    }
                }
            }
        }
    }
}
