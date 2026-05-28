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
package org.jkiss.dbeaver.model.navigator.meta;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * DBXTreeDescriptor
 */
public class DBXTreeDescriptor extends DBXTreeItem {

    private final boolean supportsEntityMerge;

    public DBXTreeDescriptor(
        @NotNull AbstractDescriptor source,
        @Nullable DBXTreeNode parent,
        @Nullable IConfigurationElement config,
        @NotNull String path,
        @Nullable String propertyName,
        boolean optional,
        boolean navigable,
        boolean inline,
        boolean virtual,
        boolean standalone,
        @Nullable String visibleIf,
        @Nullable String recursiveLink
    ) {
        super(source, parent, config, path, propertyName, optional, navigable, inline, virtual, standalone, visibleIf, recursiveLink);

        this.supportsEntityMerge = config != null && CommonUtils.toBoolean(config.getAttribute("supportsEntityMerge"));
    }

    public DBXTreeDescriptor(@NotNull AbstractDescriptor source, @NotNull DBXTreeDescriptor item) {
        super(source, null, item);

        this.supportsEntityMerge = item.supportsEntityMerge;
    }

    public boolean supportsEntityMerge() {
        return supportsEntityMerge;
    }

    /**
     * Find first class which implements baseType.
     * Implementors are property types in DBXTreeItem.
     * Search is performed hierarchically
     */

    @Nullable
    public static Class<?> findImplementorTypeInDataSourceTree(
        @NotNull DBXTreeNode parent,
        @NotNull Class<?> parentClass,
        @NotNull Class<? extends DBSObject> baseType,
        @Nullable DBNNode context
    ) {
        // checked nodes are needed to avoid infinite recursion in case node contains itself somewhere in its children
        return findImplementorTypeInDataSourceTree(
            parent,
            parentClass,
            baseType,
            context,
            Collections.newSetFromMap(new IdentityHashMap<>())
        );
    }


    @Nullable
    private static Class<?> findImplementorTypeInDataSourceTree(
        @NotNull DBXTreeNode parent,
        @NotNull Class<?> parentClass,
        @NotNull Class<? extends DBSObject> baseType,
        @Nullable DBNNode context,
        @NotNull Set<DBXTreeNode> checkedNodes
    ) {
        List<DBXTreeNode> children = parent.getChildren(context);
        {
            for (DBXTreeNode node : children) {
                // skip node if already present in set
                if (!checkedNodes.add(node)) {
                    continue;
                }
                if (node instanceof DBXTreeItem item) {
                    // Check item for a match
                    Class<?> propertyType = findImplementorTypeInItem(parentClass, baseType, item, context, checkedNodes);
                    if (propertyType != null) {
                        return propertyType;
                    }
                } else if (node instanceof DBXTreeFolder folder) {
                    // Browse thru all folder children
                    for (DBXTreeNode folderChild : folder.getChildren(context)) {
                        if (folderChild instanceof DBXTreeItem folderItem) {
                            Class<?> propertyType = findImplementorTypeInItem(
                                parentClass,
                                baseType,
                                folderItem,
                                context,
                                checkedNodes
                            );
                            if (propertyType != null) {
                                return propertyType;
                            }
                        } else {
                            Class<?> result = findImplementorTypeInDataSourceTree(
                                folderChild,
                                parentClass,
                                baseType,
                                context,
                                checkedNodes
                            );
                            if (result != null) {
                                return result;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private static Class<?> findImplementorTypeInItem(
        @NotNull Class<?> parentClass,
        @NotNull Class<? extends DBSObject> baseType,
        @NotNull DBXTreeItem item,
        @Nullable DBNNode context,
        @NotNull Set<DBXTreeNode> checkedNodes
    ) {
        Class<?> propertyType = item.getPropertyOrCollectionItemType(parentClass);
        if (propertyType != null) {
            if (baseType.isAssignableFrom(propertyType)) {
                return propertyType;
            }
            // Try to go deeper
            if (item.hasChildren(null)) {
                Class<?> result = findImplementorTypeInDataSourceTree(item, propertyType, baseType, context, checkedNodes);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

}
