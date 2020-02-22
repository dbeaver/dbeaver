/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ui.navigator;

import org.eclipse.jface.viewers.Viewer;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.navigator.DBNNode;

/**
 * INavigatorView
 */
public interface INavigatorModelView
{
    String NAVIGATOR_CONTEXT_ID = "org.jkiss.dbeaver.ui.context.navigator";
    String NAVIGATOR_VIEW_CONTEXT_ID = "org.jkiss.dbeaver.ui.context.navigator.view";
    // Control-based handler activation disabled for now (as it requires workbench site and thus doesn't work in dialogs)
    // However current handlers also don't work in dialogs as they are mapped on activePart.
    // TODO: find some solution.
    String NAVIGATOR_CONTROL_ID = "org.jkiss.dbeaver.core.ui.navigator.control";

    DBNNode getRootNode();
    
    @Nullable
    Viewer getNavigatorViewer();

}
