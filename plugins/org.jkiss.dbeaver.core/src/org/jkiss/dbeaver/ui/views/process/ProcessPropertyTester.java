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
package org.jkiss.dbeaver.ui.views.process;

import org.eclipse.core.expressions.PropertyTester;
import org.jkiss.dbeaver.model.runtime.DBRProcessController;
import org.jkiss.dbeaver.ui.ActionUtils;

/**
 * ProcessPropertyTester
 */
public class ProcessPropertyTester extends PropertyTester
{

    public static final String NAMESPACE = "org.jkiss.dbeaver.runtime.process";
    public static final String PROP_RUNNING = "running";

    public ProcessPropertyTester() {
        super();
    }

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof DBRProcessController)) {
            return false;
        }
        DBRProcessController controller = (DBRProcessController)receiver;
        if (property.equals(PROP_RUNNING)) {
            return controller.getProcessDescriptor() != null && controller.getProcessDescriptor().isRunning();
        }

        return false;
    }

    public static void firePropertyChange(String propName)
    {
        ActionUtils.evaluatePropertyState(NAMESPACE + "." + propName);
    }

}