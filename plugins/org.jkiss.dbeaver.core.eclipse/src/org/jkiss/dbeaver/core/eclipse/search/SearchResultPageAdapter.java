package org.jkiss.dbeaver.core.eclipse.search;

import org.eclipse.search.ui.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.IPageSite;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.search.IObjectSearchResultPage;

/**
 * Results page adapter
 */
public abstract class SearchResultPageAdapter implements ISearchResultPage {

    private final IObjectSearchResultPage source;

    private String id;
    private ISearchResult searchResult;
    private Object uiState;
    private ISearchResultViewPart viewPart;
    private ISearchResultListener resultListener;

    public SearchResultPageAdapter(IObjectSearchResultPage source)
    {
        this.source = source;
        this.resultListener = new ISearchResultListener() {
            @Override
            public void searchResultChanged(SearchResultEvent e)
            {
                if (e.getSearchResult() instanceof SearchResultAdapter) {
                    final SearchResultAdapter resultAdapter = (SearchResultAdapter) e.getSearchResult();
                    UIUtils.runInUI(null, new Runnable() {
                        @Override
                        public void run()
                        {
                            SearchResultPageAdapter.this.source.populateObjects(VoidProgressMonitor.INSTANCE, resultAdapter.getObjects());
                        }
                    });
                }
            }
        };
    }

    @Override
    public Object getUIState()
    {
        return uiState;
    }

    @Override
    public void setInput(ISearchResult search, Object uiState)
    {
        if (this.searchResult != null) {
            this.searchResult.removeListener(this.resultListener);
        }
        this.searchResult = search;
        this.uiState = uiState;
        if (this.searchResult != null) {
            this.searchResult.addListener(this.resultListener);
        }
        if (this.searchResult == null) {
            source.clearObjects();
        }
    }

    @Override
    public void setViewPart(ISearchResultViewPart part)
    {
        this.viewPart = part;
    }

    @Override
    public void restoreState(IMemento memento)
    {

    }

    @Override
    public void saveState(IMemento memento)
    {

    }

    @Override
    public void setID(String id)
    {
        this.id = id;
    }

    @Override
    public String getID()
    {
        return this.id;
    }

    @Override
    public String getLabel()
    {
        return searchResult == null ? "" : searchResult.getLabel();
    }

    @Override
    public IPageSite getSite()
    {
        return source.getSite();
    }

    @Override
    public void init(IPageSite site) throws PartInitException
    {
        source.init(site);
    }

    @Override
    public void createControl(Composite parent)
    {
        source.createControl(parent);
    }

    @Override
    public void dispose()
    {
        source.dispose();
    }

    @Override
    public Control getControl()
    {
        return source.getControl();
    }

    @Override
    public void setActionBars(IActionBars actionBars)
    {
        source.setActionBars(actionBars);
    }

    @Override
    public void setFocus()
    {
        source.setFocus();
    }
}
