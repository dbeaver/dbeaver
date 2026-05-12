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

package org.jkiss.dbeaver.ui.navigator.actions;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.dbeaver.ui.navigator.INavigatorNodeActionHandler;

/**
 * Read-only action handler
 */
public abstract class NavigatorNodeActionHandlerAbstract implements INavigatorNodeActionHandler {

    @Override
    public boolean isEnabledFor(@NotNull INavigatorModelView view, @NotNull DBNNode node) {
        return true;
    }

    @Override
    public boolean isSticky(@NotNull INavigatorModelView view, @NotNull DBNNode node) {
        return true;
    }

    @Override
    @Nullable
    public DBPImage getNodeActionIcon(@NotNull INavigatorModelView view, @NotNull DBNNode node) {
        return DBIcon.TYPE_UNKNOWN;
    }

    @Override
    @Nullable
    public String getNodeActionToolTip(@NotNull INavigatorModelView view, @NotNull DBNNode node) {
        return null;
    }

}
