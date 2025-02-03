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
package org.jkiss.dbeaver.ui.navigator.project;

import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.navigator.INavigatorFilter;

public class SimpleNavigatorTreeFilter implements INavigatorFilter {
    @Override
    public boolean filterFolders() {
        return false;
    }

    @Override
    public boolean isLeafObject(Object object) {
        return object instanceof DBNNode node && !node.hasChildren(true);
    }

    @Override
    public boolean filterObjectByPattern(Object object) {
        // Filter only leaf items
        return isLeafObject(object);
    }

    @Override
    public boolean select(Object toTest) {
        return true;
    }
}
