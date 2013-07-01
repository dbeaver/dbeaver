package org.jkiss.dbeaver.ui.search;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;

public class SearchDatabaseObjectsView extends ViewPart
{
    public static final String VIEW_ID = "org.jkiss.dbeaver.ui.search.SearchDatabaseObjectsView";

    private CTabFolder resultsFolder;

    @Override
    public void createPartControl(Composite parent)
    {
        resultsFolder = new CTabFolder(parent, SWT.TOP);

        UIUtils.setHelp(resultsFolder, IHelpContextIds.CTX_SEARCH_RESULTS);
    }

    @Override
    public void setFocus()
    {
        resultsFolder.setFocus();
    }

    public void addResults(IObjectSearchResultPage resultPage)
    {
        resultPage.createControl(resultsFolder);

        CTabItem tabItem = new CTabItem(resultsFolder, SWT.NONE);
        tabItem.setData(resultPage);
        tabItem.setControl(resultPage.getControl());
        //tabItem.setText(resultPage.getTitle());
    }

}
