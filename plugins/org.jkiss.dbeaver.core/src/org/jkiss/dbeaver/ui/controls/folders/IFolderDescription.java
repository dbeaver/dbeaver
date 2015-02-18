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
package org.jkiss.dbeaver.ui.controls.folders;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.part.IPage;

/**
 * Represents a tab to be displayed in the tab list in the tabbed property sheet
 * page.
 * 
 * @author Anthony Hunter
 * @author Serge Rieder
 */
public interface IFolderDescription {

    /**
     * Tab unique ID.
     */
    public String getId();

    /**
	 * Get the text label for the tab.
	 * 
	 * @return the text label for the tab.
	 */
	public String getText();

    /**
     * Get the icon image for the tab.
     *
     * @return the icon image for the tab.
     */
    public Image getImage();

    /**
     * Tab tooltip.
     */
    public String getTooltip();

    /**
	 * Determine if this tab is indented.
	 * 
	 * @return <code>true</code> if this tab is indented.
	 */
	public boolean isIndented();

    /**
     * Creates tab contents
     */
    public IFolder getContents();

}
