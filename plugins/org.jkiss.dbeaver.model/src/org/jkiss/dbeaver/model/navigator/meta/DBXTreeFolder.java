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
package org.jkiss.dbeaver.model.navigator.meta;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderRegistry;
import org.jkiss.dbeaver.model.connection.DBPEditorContribution;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.runtime.DBWorkbench;
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

    private List<String> contributedCategories = null;

    public DBXTreeFolder(AbstractDescriptor source, DBXTreeNode parent, IConfigurationElement config, String type, boolean navigable, boolean virtual, String visibleIf) {
        super(source, parent, config, navigable, false, virtual, false, visibleIf, null);
        this.type = type;
        this.label = config.getAttribute("label");
        this.description = config.getAttribute("description");
    }

    DBXTreeFolder(AbstractDescriptor source, DBXTreeNode parent, DBXTreeFolder folder) {
        super(source, parent, folder);
        this.type = folder.type;
        this.label = folder.label;
        this.description = folder.description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
}
