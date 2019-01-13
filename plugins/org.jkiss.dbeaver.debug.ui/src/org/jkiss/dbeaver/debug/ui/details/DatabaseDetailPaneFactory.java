/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017-2018 Alexander Fedorov (alexander.fedorov@jkiss.org)
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

package org.jkiss.dbeaver.debug.ui.details;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.ui.IDetailPane;
import org.eclipse.debug.ui.IDetailPaneFactory;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.jkiss.dbeaver.debug.DBGConstants;
import org.jkiss.dbeaver.debug.ui.internal.DebugUIMessages;

public class DatabaseDetailPaneFactory implements IDetailPaneFactory {

    private Map<String, String> names;
    private Map<String, String> descriptions;

    @Override
    public Set<String> getDetailPaneTypes(IStructuredSelection selection) {
        HashSet<String> set = new HashSet<>();
        if (selection.size() == 1) {
            Object first = selection.getFirstElement();
            if (first instanceof IBreakpoint) {
                IBreakpoint breakpoint = (IBreakpoint) first;
                try {
                    String type = breakpoint.getMarker().getType();
                    if (DBGConstants.BREAKPOINT_ID_DATABASE_LINE.equals(type)) {
                        set.add(DatabaseStandardBreakpointPane.DETAIL_PANE_STANDARD_BREAKPOINT);
                    } else {
                        set.add(DatabaseStandardBreakpointPane.DETAIL_PANE_STANDARD_BREAKPOINT);
                    }
                } catch (CoreException e) {
                }

            }
        }
        return set;
    }

    @Override
    public String getDefaultDetailPane(IStructuredSelection selection) {
        if (selection.size() == 1) {
            Object first = selection.getFirstElement();
            if (first instanceof IBreakpoint) {
                IBreakpoint breakpoint = (IBreakpoint) first;
                try {
                    String type = breakpoint.getMarker().getType();
                    if (DBGConstants.BREAKPOINT_ID_DATABASE_LINE.equals(type)) {
                        return DatabaseStandardBreakpointPane.DETAIL_PANE_STANDARD_BREAKPOINT;
                    } else {
                        return DatabaseStandardBreakpointPane.DETAIL_PANE_STANDARD_BREAKPOINT;
                    }
                } catch (CoreException e) {
                }

            }
        }
        return null;
    }

    @Override
    public IDetailPane createDetailPane(String paneID) {
        if (DatabaseStandardBreakpointPane.DETAIL_PANE_STANDARD_BREAKPOINT.equals(paneID)) {
            return new DatabaseStandardBreakpointPane();
        }
        return null;
    }

    @Override
    public String getDetailPaneName(String paneID) {
        return getNames().get(paneID);
    }

    protected Map<String, String> getNames() {
        if (names == null) {
            names.put(DatabaseStandardBreakpointPane.DETAIL_PANE_STANDARD_BREAKPOINT,
                    DebugUIMessages.DatabaseStandardBreakpointPane_name);
        }
        return names;
    }

    @Override
    public String getDetailPaneDescription(String paneID) {
        return getDescriptions().get(paneID);
    }

    protected Map<String, String> getDescriptions() {
        if (descriptions == null) {
            descriptions.put(DatabaseStandardBreakpointPane.DETAIL_PANE_STANDARD_BREAKPOINT,
                    DebugUIMessages.DatabaseStandardBreakpointPane_description);
        }
        return descriptions;
    }

}
