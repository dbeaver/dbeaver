/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.actions;

import org.eclipse.core.expressions.PropertyTester;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ui.ActionUtils;

/**
 * GlobalPropertyTester
 */
public class GlobalPropertyTester extends PropertyTester
{
    //static final Log log = LogFactory.get vLog(ObjectPropertyTester.class);

    public static final String NAMESPACE = "org.jkiss.dbeaver.core.global";
    public static final String PROP_STANDALONE = "standalone";
    public static final String PROP_HAS_ACTIVE_PROJECT = "hasActiveProject";
    public static final String PROP_HAS_MULTI_PROJECTS = "hasMultipleProjects";

    public GlobalPropertyTester() {
        super();
    }

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

}
