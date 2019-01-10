/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

/**
 * A null-op scrollbar proxy.  Used when the grid is not showing scrollbars.
 *
 * @author chris.gross@us.ibm.com
 * @since 2.0.0
 */
class NullScrollBar implements IGridScrollBar
{

    @Override
    public int getWidth()
    {
        return 0;
    }

    @Override
    public boolean getVisible()
    {
        return false;
    }

    @Override
    public void setVisible(boolean visible)
    {
    }

    @Override
    public int getSelection()
    {
        return 0;
    }

    @Override
    public void setSelection(int selection)
    {
    }

    @Override
    public void setValues(int selection, int min, int max, int thumb, int increment, int pageIncrement)
    {
    }

    @Override
    public void handleMouseWheel(Event e)
    {
    }

    @Override
    public void setMinimum(int min)
    {
    }

    @Override
    public int getMinimum()
    {
        return 0;
    }

    @Override
    public void setMaximum(int max)
    {
    }

    @Override
    public int getMaximum()
    {
        return 0;
    }

    @Override
    public void setThumb(int thumb)
    {
    }

    @Override
    public int getThumb()
    {
        return 0;
    }

    @Override
    public void setIncrement(int increment)
    {
    }

    @Override
    public int getIncrement()
    {
        return 0;
    }

    @Override
    public void setPageIncrement(int page)
    {
    }

    @Override
    public int getPageIncrement()
    {
        return 0;
    }

    @Override
    public void addSelectionListener(SelectionListener listener)
    { 
    }

    @Override
    public void removeSelectionListener(SelectionListener listener)
    {
   
    }

}
