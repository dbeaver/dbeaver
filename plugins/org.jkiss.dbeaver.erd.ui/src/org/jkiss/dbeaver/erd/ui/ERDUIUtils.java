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
package org.jkiss.dbeaver.erd.ui;

import org.eclipse.gef.palette.PaletteContainer;
import org.eclipse.gef.palette.PaletteEntry;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.erd.model.ERDEntityAttribute;
import org.jkiss.dbeaver.erd.model.ERDObject;
import org.jkiss.dbeaver.erd.ui.editor.ERDViewStyle;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIMessages;
import org.jkiss.dbeaver.erd.ui.model.EntityDiagram;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.utils.CommonUtils;

public class ERDUIUtils {
    private static final Log log = Log.getLog(ERDUIUtils.class);

    public static final boolean OPEN_OBJECT_PROPERTIES = true;

    public static void openObjectEditor(@Nullable EntityDiagram diagram, @NotNull ERDObject object) {
        openObjectEditor(object);
        return;
    }

    public static void openObjectEditor(@NotNull ERDObject object) {
        Object dbObject = object.getObject();
        if (dbObject instanceof DBSObject) {
            UIUtils.runUIJob("Open object editor", monitor -> {
                if (!(dbObject instanceof DBSEntity) && OPEN_OBJECT_PROPERTIES) {
                    openProperties();
                } else {
                    DBNDatabaseNode node = DBNUtils.getNodeByObject(
                        monitor,
                        (DBSObject) dbObject,
                        true
                    );
                    if (node != null) {
                        NavigatorUtils.openNavigatorNode(node, UIUtils.getActiveWorkbenchWindow());
                    }
                }
            });
        }
    }

    public static void openProperties() {
        try {
            IWorkbenchPage activePage = UIUtils.getActiveWorkbenchWindow().getActivePage();
            IViewPart propsView = activePage.showView(IPageLayout.ID_PROP_SHEET);
            if (propsView != null) {
                propsView.setFocus();
            }
        } catch (PartInitException e) {
            DBWorkbench.getPlatformUI().showError("Object open", "Can't open property view", e);
        }
    }

    public static void openOutline() {
        try {
            IWorkbenchPage activePage = UIUtils.getActiveWorkbenchWindow().getActivePage();
            IViewPart propsView = activePage.showView(IPageLayout.ID_OUTLINE);
            if (propsView != null) {
                propsView.setFocus();
            }
        } catch (PartInitException e) {
            DBWorkbench.getPlatformUI().showError("Object open", "Can't open outline view", e);
        }
    }

    public static String getFullAttributeLabel(EntityDiagram diagram, ERDEntityAttribute attribute, boolean includeType) {
        return getFullAttributeLabel(diagram, attribute, includeType, false);
    }

    /**
     *
     * @param diagram diagram
     * @param attribute attribute to provide label from
     * @param includeType is type included in the label
     * @param accessible is accessibility text needed
     * @return attribute label
     */
    public static String getFullAttributeLabel(
        EntityDiagram diagram,
        ERDEntityAttribute attribute,
        boolean includeType,
        boolean accessible
    ) {
        String attributeLabel = attribute.getName();
        if (includeType && diagram.hasAttributeStyle(ERDViewStyle.TYPES)) {
            if (accessible) {
                attributeLabel += NLS.bind(ERDUIMessages.erd_accessibility_attribute_part_type, attribute.getObject().getFullTypeName());
            } else {
                attributeLabel += ": " + attribute.getObject().getFullTypeName();
            }
        }
        if (includeType && diagram.hasAttributeStyle(ERDViewStyle.NULLABILITY)) {
            String type = "";
            if (attribute.getObject().isRequired()) {
                type += " NOT NULL";
            } else if (accessible) {
                type += " CAN BE NULL";
            }
            if (accessible) {
                attributeLabel += NLS.bind(ERDUIMessages.erd_accessibility_attribute_part_nullability, type);
            } else {
                attributeLabel += type;
            }

        }
        if (diagram.hasAttributeStyle(ERDViewStyle.COMMENTS)) {
            String comment = attribute.getObject().getDescription();
            if (!CommonUtils.isEmpty(comment)) {
                if (accessible) {
                    attributeLabel += NLS.bind(ERDUIMessages.erd_accessibility_attribute_part_comments, comment);
                } else {
                    attributeLabel += " - " + comment;
                }
            }
        }
        return attributeLabel;
	}

    @Nullable
    public static PaletteEntry findPaletteEntry(@NotNull PaletteContainer container, @NotNull String id) {
        for (Object child : container.getChildren()) {
            if (child instanceof PaletteEntry && id.equals(((PaletteEntry) child).getId())) {
                return (PaletteEntry) child;
            }
            if (child instanceof PaletteContainer) {
                return findPaletteEntry((PaletteContainer) child, id);
            }
        }
        return null;
    }
}
