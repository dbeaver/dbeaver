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
package org.jkiss.dbeaver.ui.controls.lightgrid;

import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;

/**
 * Adapts a normal scrollbar to the IScrollBar proxy.
 *
 * @author chris.gross@us.ibm.com
 * @since 2.0.0
 */
class ScrollBarAdapter implements IGridScrollBar
{
    /**
     * Delegates to this scrollbar.
     */
    private ScrollBar scrollBar;
    private boolean vertical;

    /**
     * Contructs this adapter by delegating to the given scroll bar.
     * 
     * @param scrollBar delegate
     */
    public ScrollBarAdapter(ScrollBar scrollBar, boolean vertical)
    {
        super();
        this.scrollBar = scrollBar;
        this.vertical = vertical;
    }

    @Override
    public int getIncrement()
    {
        return scrollBar.getIncrement();
    }

    @Override
    public int getMaximum()
    {
        return scrollBar.getMaximum();
    }

    @Override
    public int getMinimum()
    {
        return scrollBar.getMinimum();
    }

    @Override
    public int getPageIncrement()
    {
        return scrollBar.getPageIncrement();
    }

    @Override
    public int getSelection()
    {
        return scrollBar.getSelection();
    }

    @Override
    public int getThumb()
    {
        return scrollBar.getThumb();
    }

    @Override
    public int getWidth()
    {
        return scrollBar.getSize().x;
    }



    @Override
    public boolean getVisible()
    {
        return scrollBar.getVisible();
    }

    @Override
    public void setIncrement(int value)
    {
        scrollBar.setIncrement(value);
    }

    @Override
    public void setMaximum(int value)
    {
        scrollBar.setMaximum(value);
    }

    @Override
    public void setMinimum(int value)
    {
        scrollBar.setMinimum(value);
    }

    @Override
    public void setPageIncrement(int value)
    {
        scrollBar.setPageIncrement(value);
    }

    @Override
    public void setSelection(int selection)
    {
        scrollBar.setSelection(selection);
    }

    @Override
    public void setThumb(int value)
    {
        scrollBar.setThumb(value);
    }

    @Override
    public void setValues(int selection, int minimum, int maximum, int thumb, int increment,
                          int pageIncrement)
    {
        scrollBar.setValues(selection, minimum, maximum, thumb, increment, pageIncrement);
    }

    @Override
    public void setVisible(boolean visible)
    {
        scrollBar.setVisible(visible);
    }

    @Override
    public void handleMouseWheel(Event e)
    {
    }

    @Override
    public void addSelectionListener(SelectionListener listener)
    {
        scrollBar.addSelectionListener(listener);
    }

    @Override
    public void removeSelectionListener(SelectionListener listener)
    {
        scrollBar.removeSelectionListener(listener);
    }
}
