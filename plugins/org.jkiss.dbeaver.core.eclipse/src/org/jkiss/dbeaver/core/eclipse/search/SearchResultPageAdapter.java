package org.jkiss.dbeaver.core.eclipse.search;

import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.ISearchResultPage;
import org.eclipse.search.ui.ISearchResultViewPart;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.IPageSite;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.search.IObjectSearchResultPage;

/**
 * Results page adapter
 */
public class SearchResultPageAdapter implements ISearchResultPage {

    private final IObjectSearchResultPage source;

    private String id;
    private SearchResultAdapter searchResult;
    private Object uiState;
    private ISearchResultViewPart viewPart;
    private IPageSite site;

    public SearchResultPageAdapter(IObjectSearchResultPage source)
    {
        this.source = source;
    }

    @Override
    public Object getUIState()
    {
        return uiState;
    }

    @Override
    public void setInput(ISearchResult search, Object uiState)
    {
        this.searchResult = (SearchResultAdapter) search;
        this.uiState = uiState;
        source.populateObjects(VoidProgressMonitor.INSTANCE, this.searchResult.getObjects());
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
        return searchResult.getLabel();
    }

    @Override
    public IPageSite getSite()
    {
        return site;
    }

    @Override
    public void init(IPageSite site) throws PartInitException
    {
        this.site = site;
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
