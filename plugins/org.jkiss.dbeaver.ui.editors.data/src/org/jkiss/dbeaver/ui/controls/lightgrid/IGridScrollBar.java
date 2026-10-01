/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import org.jkiss.code.NotNull;

/**
 * Used by Grid to externalize the scrollbars from the table itself.
 */
public interface IGridScrollBar {

    int getWidth();

    boolean getVisible();

    void setVisible(boolean visible);

    int getSelection();

    void setSelection(int selection);

    /**
     * Sets the receiver's selection, minimum value, maximum value, thumb,
     * increment and page increment all at once.
     */
    void setValues(int selection, int min, int max, int thumb, int increment, int pageIncrement);

    void handleMouseWheel(Event e);
    
    void setMinimum(int min);
    
    int getMinimum();
    
    void setMaximum(int max);
    
    int getMaximum();
    
    void setThumb(int thumb);
    
    int getThumb();
    
    void setIncrement(int increment);
    
    int getIncrement();

    void addSelectionListener(@NotNull SelectionListener listener);
    
    void removeSelectionListener(@NotNull SelectionListener listener);
}
