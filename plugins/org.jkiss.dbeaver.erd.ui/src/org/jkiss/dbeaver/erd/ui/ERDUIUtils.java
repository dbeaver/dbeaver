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
package org.jkiss.dbeaver.erd.ui;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.erd.model.ERDEntityAttribute;
import org.jkiss.dbeaver.erd.model.ERDObject;
import org.jkiss.dbeaver.erd.ui.editor.ERDViewStyle;
import org.jkiss.dbeaver.erd.ui.model.EntityDiagram;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.utils.CommonUtils;

public class ERDUIUtils
{
    private static final Log log = Log.getLog(ERDUIUtils.class);

    public static void openObjectEditor(@NotNull ERDObject object) {
        if (object.getObject() instanceof DBSObject) {
            UIUtils.runUIJob("Open object editor", monitor -> {
                DBNDatabaseNode node = DBNUtils.getNodeByObject(
                    monitor,
                    (DBSObject) object.getObject(),
                    true
                );
                if (node != null) {
                    NavigatorUtils.openNavigatorNode(node, UIUtils.getActiveWorkbenchWindow());
                }
            });
        }
    }

    public static String getFullAttributeLabel(EntityDiagram diagram, ERDEntityAttribute attribute, boolean includeType) {
        String attributeLabel = attribute.getName();
        if (includeType && diagram.hasAttributeStyle(ERDViewStyle.TYPES)) {
            attributeLabel += ": " + attribute.getObject().getFullTypeName();
        }
        if (includeType && diagram.hasAttributeStyle(ERDViewStyle.NULLABILITY)) {
            if (attribute.getObject().isRequired()) {
                attributeLabel += " NOT NULL";
            }
        }
        if (diagram.hasAttributeStyle(ERDViewStyle.COMMENTS)) {
            String comment = attribute.getObject().getDescription();
            if (!CommonUtils.isEmpty(comment)) {
                attributeLabel += " - " + comment;
            }
        }
        return attributeLabel;
	}

}
