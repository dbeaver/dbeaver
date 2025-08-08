/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.graphics.Image;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;

public abstract class ContextMenuTreeNodeSpecial extends TreeNodeSpecial {
    protected ContextMenuTreeNodeSpecial(DBNNode parent) {
        super(parent);
    }

    @Override
    public Image getImage(Object element) {
        return null;
    }

    public abstract void fillContextMenu(@NotNull MenuManager menu, @NotNull DatabaseNavigatorTree navigatorTree);
}
