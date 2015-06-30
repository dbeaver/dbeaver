/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.actions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.*;
import org.jkiss.dbeaver.core.DBeaverCore;
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
        if (property.equals(PROP_HAS_MULTI_PROJECTS)) {
            return DBeaverCore.getInstance().getLiveProjects().size() > 1;
        } else if (property.equals(PROP_HAS_ACTIVE_PROJECT)) {
            return DBeaverCore.getInstance().getProjectRegistry().getActiveProject() != null;
        } else if (property.equals(PROP_STANDALONE)) {
            return DBeaverCore.isStandalone();
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
