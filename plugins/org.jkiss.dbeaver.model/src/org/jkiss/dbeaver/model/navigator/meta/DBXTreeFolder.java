/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderRegistry;
import org.jkiss.dbeaver.model.connection.DBPEditorContribution;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * DBXTreeFolder
 */
public class DBXTreeFolder extends DBXTreeNode {
    private String type;
    private String label;
    private String description;
    private String optionalItem;

    private boolean isOptional;

    private List<String> contributedCategories = null;
    private ItemType[] itemTypes = null;

    public static class ItemType {
        private String className;
        private String itemType;
        private DBPImage itemIcon;

        private ItemType(String className, String itemType, DBPImage itemIcon) {
            this.className = className;
            this.itemType = itemType;
            this.itemIcon = itemIcon;
        }

        public String getClassName() {
            return className;
        }

        public String getItemType() {
            return itemType;
        }

        public DBPImage getItemIcon() {
            return itemIcon;
        }
    }

    public DBXTreeFolder(AbstractDescriptor source, DBXTreeNode parent, IConfigurationElement config, String type, boolean navigable, boolean virtual, String visibleIf, boolean isOptional) {
        super(source, parent, config, navigable, false, virtual, false, visibleIf, null);
        this.type = type;
        this.label = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.optionalItem = config.getAttribute("optionalItem");
        this.isOptional = isOptional;

        IConfigurationElement[] itemTypesConfig = config.getChildren("itemType");
        if (!ArrayUtils.isEmpty(itemTypesConfig)) {
            List<ItemType> objectCreateTypes = null;
            for (IConfigurationElement it : itemTypesConfig) {
                String itemTypeName = it.getAttribute("type");
                if (!CommonUtils.isEmpty(itemTypeName)) {
                    if (objectCreateTypes == null) {
                        objectCreateTypes = new ArrayList<>();
                    }
                    objectCreateTypes.add(new ItemType(
                        itemTypeName,
                        it.getAttribute("label"),
                        source.iconToImage(it.getAttribute("icon"))));
                }
            }
            if (objectCreateTypes != null) {
                itemTypes = objectCreateTypes.toArray(new ItemType[0]);
            }
        }
    }

    DBXTreeFolder(AbstractDescriptor source, DBXTreeNode parent, DBXTreeFolder folder) {
        super(source, parent, folder);
        this.type = folder.type;
        this.label = folder.label;
        this.description = folder.description;

        this.optionalItem = folder.optionalItem;

        this.isOptional = folder.isOptional;

        this.contributedCategories = folder.contributedCategories == null ? null : new ArrayList<>(folder.contributedCategories);
        this.itemTypes = folder.itemTypes;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIdOrType() {
        String id = getId();
        return !CommonUtils.isEmpty(id) ? id : type;
    }

    public String getOptionalItem() {
        return optionalItem;
    }

    public boolean isOptional() {
        return isOptional;
    }

    @Override
    public String getNodeTypeLabel(@Nullable DBPDataSource dataSource, @Nullable String locale) {
        if (locale == null) {
            return label;
        } else {
            return getConfig().getAttribute("label", locale);
        }
    }

    @Override
    public String getChildrenTypeLabel(@Nullable DBPDataSource dataSource, String locale) {
        return getNodeTypeLabel(dataSource, locale);
    }

    @Override
    public boolean hasChildren(DBNNode context, boolean navigable) {
        boolean hasChildren = super.hasChildren(context, navigable);
        if (!hasChildren) {
            hasChildren = !CommonUtils.isEmpty(contributedCategories);
        }
        return hasChildren;
    }

    @NotNull
    @Override
    public List<DBXTreeNode> getChildren(DBNNode context) {
        List<DBXTreeNode> children = super.getChildren(context);
        if (!CommonUtils.isEmpty(contributedCategories) && context instanceof DBNDatabaseNode) {
            // Add contributed editors
            List<DBXTreeNode> childrenWithContributions = new ArrayList<>(children);
            DBPDataSourceProviderRegistry dspRegistry = DBWorkbench.getPlatform().getDataSourceProviderRegistry();
            DBPDataSourceContainer dataSource = ((DBNDatabaseNode) context).getDataSourceContainer();
            for (String category : contributedCategories) {
                DBPEditorContribution[] editors = dspRegistry.getContributedEditors(category, dataSource);
                for (DBPEditorContribution editor : editors) {
                    if (!editor.isVisible(context)) {
                        continue;
                    }
                    DBXTreeObject editorNode = new DBXTreeObject(
                        getSource(),
                        null, // No parent - otherwise we'll have dups after each call
                        null,
                        null,
                        editor.getLabel(),
                        editor.getDescription(),
                        editor.getEditorId());
                    editorNode.setDefaultIcon(editor.getIcon());
                    childrenWithContributions.add(editorNode);
                }
            }
            return childrenWithContributions;
        }
        return children;
    }

    public DBXTreeItem getChildByPath(String path) {
        for (DBXTreeNode node : getChildren()) {
            if (node instanceof DBXTreeItem && path.equals(((DBXTreeItem) node).getPath())) {
                return (DBXTreeItem) node;
            }
        }
        return null;
    }

    @Override
    protected boolean isVisible(DBNNode context) {
        if (!super.isVisible(context)) {
            return false;
        }
        // If child nodes are only folders and all non visible then parent folder is also not visible
        for (DBXTreeNode childNode : getChildren(context)) {
            if (childNode.isVisible(context)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "Folder " + label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getContributedCategories() {
        return CommonUtils.safeList(contributedCategories);
    }

    public void addContribution(String category) {
        if (contributedCategories == null) {
            contributedCategories = new ArrayList<>();
        }
        contributedCategories.add(category);
    }

    public ItemType[] getItemTypes() {
        return itemTypes;
    }
}
