package org.jkiss.dbeaver.debug.ui.details;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.ui.IDetailPane;
import org.eclipse.debug.ui.IDetailPaneFactory;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.jkiss.dbeaver.debug.core.DebugCore;
import org.jkiss.dbeaver.debug.internal.ui.DebugUIMessages;

public class DatabaseDetailPaneFactory implements IDetailPaneFactory {

    private Map<String, String> names;
    private Map<String, String> descriptions;

    @Override
    public Set<String> getDetailPaneTypes(IStructuredSelection selection)
    {
        HashSet<String> set = new HashSet<>();
        if (selection.size() == 1) {
            IBreakpoint b = (IBreakpoint) selection.getFirstElement();
            try {
                String type = b.getMarker().getType();
                if (DebugCore.BREAKPOINT_DATABASE_LINE.equals(type)) {
                    set.add(DatabaseStandardBreakpointPane.DETAIL_PANE_STANDARD_BREAKPOINT);
                } else {
                    set.add(DatabaseStandardBreakpointPane.DETAIL_PANE_STANDARD_BREAKPOINT);
                }
            } catch (CoreException e) {
            }
        }
        return set;
    }

    @Override
    public String getDefaultDetailPane(IStructuredSelection selection)
    {
        if (selection.size() == 1) {
            IBreakpoint b = (IBreakpoint) selection.getFirstElement();
            try {
                String type = b.getMarker().getType();
                if (DebugCore.BREAKPOINT_DATABASE_LINE.equals(type)) {
                    return DatabaseStandardBreakpointPane.DETAIL_PANE_STANDARD_BREAKPOINT;
                } else {
                    return DatabaseStandardBreakpointPane.DETAIL_PANE_STANDARD_BREAKPOINT;
                }
            } catch (CoreException e) {
            }
        }
        return null;
    }

    @Override
    public IDetailPane createDetailPane(String paneID)
    {
        if (DatabaseStandardBreakpointPane.DETAIL_PANE_STANDARD_BREAKPOINT.equals(paneID)) {
            return new DatabaseStandardBreakpointPane();
        }
        return null;
    }

    @Override
    public String getDetailPaneName(String paneID)
    {
        return getNames().get(paneID);
    }

    protected Map<String, String> getNames()
    {
        if (names == null) {
            names.put(DatabaseStandardBreakpointPane.DETAIL_PANE_STANDARD_BREAKPOINT,
                    DebugUIMessages.DatabaseStandardBreakpointPane_name);
        }
        return names;
    }

    @Override
    public String getDetailPaneDescription(String paneID)
    {
        return getDescriptions().get(paneID);
    }

    protected Map<String, String> getDescriptions()
    {
        if (descriptions == null) {
            descriptions.put(DatabaseStandardBreakpointPane.DETAIL_PANE_STANDARD_BREAKPOINT,
                    DebugUIMessages.DatabaseStandardBreakpointPane_description);
        }
        return descriptions;
    }

}
