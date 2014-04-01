/*
 * Copyright (C) 2010-2014 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.INavigationLocation;

/**
 * Row data
 */
public class ResultSetNavigationLocation implements INavigationLocation {

    private ResultSetViewer viewer;

    public ResultSetNavigationLocation(ResultSetViewer viewer) {
        this.viewer = viewer;
    }

    @Override
    public void dispose() {

    }

    @Override
    public void releaseState() {

    }

    @Override
    public void saveState(IMemento memento) {

    }

    @Override
    public void restoreState(IMemento memento) {

    }

    @Override
    public void restoreLocation() {

    }

    @Override
    public boolean mergeInto(INavigationLocation currentLocation) {
        return false;
    }

    @Override
    public Object getInput() {
        return viewer;
    }

    @Override
    public String getText() {
        return viewer.toString();
    }

    @Override
    public void setInput(Object input) {
        this.viewer = (ResultSetViewer)input;

    }

    @Override
    public void update() {

    }
}
