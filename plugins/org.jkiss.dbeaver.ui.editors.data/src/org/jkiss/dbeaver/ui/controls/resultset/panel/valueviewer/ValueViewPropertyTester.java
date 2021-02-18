/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.resultset.panel.valueviewer;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPanel;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.controls.resultset.handler.ResultSetHandlerMain;

/**
 * ValueViewPropertyTester
 */
public class ValueViewPropertyTester extends PropertyTester
{
    public static final String NAMESPACE = "org.jkiss.dbeaver.core.resultset.panel.valueView";

    public static final String PROP_ACTIVE = "active";

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        ResultSetViewer rsv = (ResultSetViewer) ResultSetHandlerMain.getActiveResultSet((IWorkbenchPart) receiver);
        return rsv != null && checkResultSetProperty(rsv, property, expectedValue);
    }

    private boolean checkResultSetProperty(ResultSetViewer rsv, String property, Object expectedValue)
    {
        IResultSetPanel visiblePanel = rsv.getVisiblePanel();
        if (visiblePanel instanceof ValueViewerPanel) {
            switch (property) {
                case PROP_ACTIVE:
                    return true;
            }
        }
        return false;
    }

}