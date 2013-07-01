package org.jkiss.dbeaver.ui.search;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;

public class SearchDatabaseObjectsView extends ViewPart
{
    public static final String VIEW_ID = "org.jkiss.dbeaver.core.searchResults";

    @Override
    public void createPartControl(Composite parent)
    {
        Composite group = UIUtils.createPlaceholder(parent, 1);

        UIUtils.setHelp(group, IHelpContextIds.CTX_SEARCH_RESULTS);
    }

    @Override
    public void setFocus()
    {

    }

}
