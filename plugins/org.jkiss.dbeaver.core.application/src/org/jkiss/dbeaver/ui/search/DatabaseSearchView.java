package org.jkiss.dbeaver.ui.search;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.ui.INavigatorModelView;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.CompositeSelectionProvider;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.ProxyPageSite;
import org.jkiss.dbeaver.ui.UIUtils;

public class DatabaseSearchView extends ViewPart implements INavigatorModelView {
    public static final String VIEW_ID = "org.jkiss.dbeaver.ui.search.DatabaseSearchView";

    private CTabFolder resultsFolder;
    private IObjectSearchResultPage curPage;
    private CompositeSelectionProvider selectionProvider;

    @Override
    public void createPartControl(Composite parent)
    {
        resultsFolder = new CTabFolder(parent, SWT.TOP);
        resultsFolder.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                activateCurrentPage();
            }
        });

        UIUtils.setHelp(resultsFolder, IHelpContextIds.CTX_SEARCH_RESULTS);
        selectionProvider = new CompositeSelectionProvider();
        getSite().setSelectionProvider(selectionProvider);
    }

    private void activateCurrentPage()
    {
        int index = resultsFolder.getSelectionIndex();
        if (index >= 0) {
            IObjectSearchResultPage page = (IObjectSearchResultPage) resultsFolder.getItem(index).getData();
            if (page != null && page != curPage) {
                page.setFocus();

                Control pageControl = page.getControl();
                if (page instanceof INavigatorModelView) {
                    pageControl = ((INavigatorModelView) page).getNavigatorViewer().getControl();
                }
                selectionProvider.trackProvider(pageControl, page.getSite().getSelectionProvider());
                curPage = page;
            }
        } else {
            curPage = null;
        }
    }

    @Override
    public void setFocus()
    {
        resultsFolder.setFocus();
        activateCurrentPage();
    }

    public IObjectSearchResultPage openResultPage(ObjectSearchProvider provider, IObjectSearchQuery query, boolean newTab) throws DBException
    {
        if (!newTab) {
            // Remove all existing tabs
            for (CTabItem item : resultsFolder.getItems()) {
                item.dispose();
            }
        }
        IObjectSearchResultPage resultPage = provider.createResultsPage();
        try {
            resultPage.init(new ProxyPageSite(getViewSite()));
        } catch (PartInitException e) {
            throw new DBException("Can't initialize page", e);
        }
        resultPage.createControl(resultsFolder);

        CTabItem tabItem = new CTabItem(resultsFolder, SWT.NONE);
        tabItem.setData(resultPage);
        tabItem.setControl(resultPage.getControl());
        tabItem.setText(query.getLabel());
        tabItem.setImage(provider.getIcon());
        tabItem.setToolTipText(provider.getDescription());
        tabItem.setShowClose(true);
        resultsFolder.setSelection(tabItem);

        activateCurrentPage();

        return resultPage;
    }

    @Override
    public DBNNode getRootNode()
    {
        INavigatorModelView view = getActivePage(INavigatorModelView.class);
        return view == null ? null : view.getRootNode();
    }

    @Nullable
    @Override
    public Viewer getNavigatorViewer()
    {
        INavigatorModelView view = getActivePage(INavigatorModelView.class);
        return view == null ? null : view.getNavigatorViewer();
    }

    <T> T getActivePage(Class<T> pageType)
    {
        int selectionIndex = resultsFolder.getSelectionIndex();
        if (selectionIndex < 0) {
            return null;
        }
        CTabItem item = resultsFolder.getItem(selectionIndex);
        IObjectSearchResultPage page = (IObjectSearchResultPage) item.getData();
        return RuntimeUtils.getObjectAdapter(page, pageType);
    }

}
