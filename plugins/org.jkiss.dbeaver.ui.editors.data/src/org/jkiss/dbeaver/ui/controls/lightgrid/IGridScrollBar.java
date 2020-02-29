/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
 * Used by Grid to externalize the scrollbars from the table itself.
 * 
 * @author chris.gross@us.ibm.com
 * @version 1.0.0
 */
public interface IGridScrollBar
{

    public int getWidth();

    public boolean getVisible();

    public void setVisible(boolean visible);

    public int getSelection();

    public void setSelection(int selection);

    /**
     * Sets the receiver's selection, minimum value, maximum value, thumb,
     * increment and page increment all at once.
     * 
     * @param selection selection
     * @param min minimum
     * @param max maximum
     * @param thumb thumb
     * @param increment increment
     * @param pageIncrement page increment
     */
    public void setValues(int selection, int min, int max, int thumb, int increment, int pageIncrement);

    public void handleMouseWheel(Event e);
    
    public void setMinimum(int min);
    
    public int getMinimum();
    
    public void setMaximum(int max);
    
    public int getMaximum();
    
    public void setThumb(int thumb);
    
    public int getThumb();
    
    public void setIncrement(int increment);
    
    public int getIncrement();
    
    public void setPageIncrement(int page);
    
    public int getPageIncrement();
    
    public void addSelectionListener(SelectionListener listener);
    
    public void removeSelectionListener(SelectionListener listener);
}
