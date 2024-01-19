/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.erd.model;

import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class ERDContext {
    private final DBRProgressMonitor monitor;
    private final DBPDataSourceContainer dataSourceContainer;
    private final DBNModel navigatorModel;

    private final List<String> icons = new ArrayList<>();
    private final Map<ERDElement<?>, ElementSaveInfo> elementInfoMap = new IdentityHashMap<>();

    public ERDContext(DBRProgressMonitor monitor, DBPDataSourceContainer dataSourceContainer, DBNModel navigatorModel) {
        this.monitor = monitor;
        this.dataSourceContainer = dataSourceContainer;
        this.navigatorModel = navigatorModel;
    }

    public DBRProgressMonitor getMonitor() {
        return monitor;
    }

    public DBPDataSourceContainer getDataSourceContainer() {
        return dataSourceContainer;
    }

    public List<String> getIcons() {
        return icons;
    }

    public int getIconIndex(DBPImage image) {
        String icon = image.getLocation();
        int iconIndex = icons.indexOf(icon);

        if (iconIndex == -1) {
            iconIndex = icons.size();
            icons.add(icon);
        }
        return iconIndex;
    }

    public int addElementInfo(ERDElement<?> element) {
        ElementSaveInfo info = new ElementSaveInfo(element, elementInfoMap.size());
        elementInfoMap.put(element, info);
        return info.objectId;
    }

    public int getElementInfo(ERDElement<?> element) {
        ElementSaveInfo info = elementInfoMap.get(element);
        return info == null ? -1 : info.objectId;
    }

    public DBNModel getNavigatorModel() {
        return navigatorModel;
    }

    private static class ElementSaveInfo {
        final ERDElement<?> element;
        final int objectId;

        private ElementSaveInfo(ERDElement<?> element, int objectId)
        {
            this.element = element;
            this.objectId = objectId;
        }
    }

}