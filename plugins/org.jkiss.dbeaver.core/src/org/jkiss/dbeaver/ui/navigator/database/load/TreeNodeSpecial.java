/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.navigator.database.load;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;

import java.util.HashMap;
import java.util.Map;

/**
 * Special node with some custom behavior.
 * It is not navigator node.
 */
public abstract class TreeNodeSpecial implements ILabelProvider {

    public static final Object LOADING_FAMILY = new Object();

    private DBNNode parent;
    private boolean disposed;

    protected TreeNodeSpecial(DBNNode parent) {
        this.parent = parent;
    }

    public DBNNode getParent() {
        return parent;
    }

    public boolean isDisposed() {
        return disposed;
    }

    public void dispose(Object parent) {
        disposed = true;
    }

    @Override
    public void addListener(ILabelProviderListener listener) {
    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
        return false;
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
    }

    @Override
    public void dispose() {
    }

    public boolean handleDefaultAction(DatabaseNavigatorTree tree) {
        // do nothing
        return false;
    }

}