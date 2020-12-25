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

    @Override
    public String toString() {
        return id;
    }
}
