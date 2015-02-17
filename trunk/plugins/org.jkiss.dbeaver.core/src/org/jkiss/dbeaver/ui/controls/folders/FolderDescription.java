/*******************************************************************************
 * Copyright (c) 2001, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Mariot Chauvin <mariot.chauvin@obeo.fr> - bug 259553
 *     Amit Joglekar <joglekar@us.ibm.com> - Support for dynamic images (bug 385795)
 *
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

/**
 * Folders composite
 */
public abstract class FolderDescription implements IFolderDescription {

    private String id;
    private String text;
    private Image image;
    private String tooltip;

    public FolderDescription(String id, String text, Image image, String tooltip) {
        this.id = id;
        this.text = text;
        this.image = image;
        this.tooltip = tooltip;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public Image getImage() {
        return image;
    }

    @Override
    public String getTooltip() {
        return tooltip;
    }

    @Override
    public boolean isIndented() {
        return false;
    }
}
