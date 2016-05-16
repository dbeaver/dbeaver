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
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.ui.controls.folders;

import org.jkiss.dbeaver.model.DBPImage;

/**
 * Folder info
 */
public class TabbedFolderInfo {

    private final String id;
    private final String text;
    private final DBPImage image;
    private final String tooltip;
    private final boolean embeddable;
    private final ITabbedFolder contents;

    public TabbedFolderInfo(String id, String text, DBPImage image, String tooltip, boolean embeddable, ITabbedFolder contents) {
        this.id = id;
        this.text = text;
        this.image = image;
        this.tooltip = tooltip;
        this.embeddable = embeddable;
        this.contents = contents;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public DBPImage getImage() {
        return image;
    }

    public String getTooltip() {
        return tooltip;
    }

    public boolean isIndented() {
        return false;
    }

    public boolean isEmbeddable() {
        return embeddable;
    }

    public ITabbedFolder getContents() {
        return contents;
    }
}
