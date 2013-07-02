package org.jkiss.dbeaver.ui.search;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;

public class DatabaseSearchView extends ViewPart
{
    public static final String VIEW_ID = "org.jkiss.dbeaver.ui.search.DatabaseSearchView";

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

    public void addResults(IObjectSearchResultPage resultPage, String title, String toolTip, Image icon)
    {
        resultPage.createControl(resultsFolder);

        CTabItem tabItem = new CTabItem(resultsFolder, SWT.NONE);
        tabItem.setData(resultPage);
        tabItem.setControl(resultPage.getControl());
        tabItem.setText(title);
        tabItem.setImage(icon);
        tabItem.setToolTipText(toolTip);
        resultsFolder.setSelection(tabItem);
    }

    public IObjectSearchResultPage openResultPage(ObjectSearchProvider provider, boolean newTab) throws DBException
    {
        for (CTabItem item : resultsFolder.getItems()) {
            item.dispose();
        }
        IObjectSearchResultPage resultsPage = provider.createResultsPage();
        addResults(resultsPage, provider.getLabel(), provider.getDescription(), provider.getIcon());
        return resultsPage;
    }
}
