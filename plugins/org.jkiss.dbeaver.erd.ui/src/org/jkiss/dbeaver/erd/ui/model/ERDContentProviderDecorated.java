/*
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
package org.jkiss.dbeaver.erd.ui.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.erd.model.ERDAttributeVisibility;
import org.jkiss.dbeaver.erd.model.ERDContentProviderDefault;
import org.jkiss.dbeaver.erd.model.ERDDiagram;
import org.jkiss.dbeaver.erd.model.ERDEntity;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;

/**
 * ERD content provider decorated
 */
public class ERDContentProviderDecorated extends ERDContentProviderDefault {

    private static final Log log = Log.getLog(ERDContentProviderDecorated.class);

    public ERDContentProviderDecorated() {
    }

    @Override
    public void fillEntityFromObject(@NotNull DBRProgressMonitor monitor, @NotNull ERDDiagram diagram, List<ERDEntity> otherEntities, @NotNull ERDEntity erdEntity) {
        ERDAttributeVisibility attributeVisibility = ERDAttributeVisibility.ALL;
        if (diagram instanceof ERDContainerDecorated) {
            attributeVisibility = ((ERDContainerDecorated) diagram).getDecorator().supportsAttributeVisibility() ?
                erdEntity.getAttributeVisibility() : ERDAttributeVisibility.ALL;
            if (attributeVisibility == null) {
                EntityDiagram.NodeVisualInfo visualInfo = ((ERDContainerDecorated) diagram).getVisualInfo(erdEntity.getObject(), false);
                if (visualInfo != null) {
                    attributeVisibility = visualInfo.attributeVisibility;
                }
                if (attributeVisibility == null) {
                    attributeVisibility = ((ERDContainerDecorated) diagram).getAttributeVisibility();
                }
            }
        }
        fillEntityFromObject(monitor, erdEntity, attributeVisibility);
    }

}
