/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.entity;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInputFactory;
import org.jkiss.dbeaver.ui.editors.entity.properties.ObjectPropertiesEditor;
import org.jkiss.dbeaver.ui.editors.entity.properties.ObjectPropertiesEditorInputFactory;
import org.jkiss.dbeaver.ui.internal.UINavigatorActivator;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.utils.CommonUtils;

/**
 * EntityEditorDescriptor
 */
public class EntityEditorDescriptor extends AbstractContextDescriptor
{
    private static final Log log = Log.getLog(EntityEditorDescriptor.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.databaseEditor"; //NON-NLS-1 //$NON-NLS-1$

    public static final String DEFAULT_OBJECT_EDITOR_ID = "default.object.editor"; //NON-NLS-1 //$NON-NLS-1$

    public static final String POSITION_PROPS = IActionConstants.MB_ADDITIONS_PROPS;
    public static final String POSITION_START = IActionConstants.MB_ADDITIONS_START;
    public static final String POSITION_MIDDLE = IActionConstants.MB_ADDITIONS_MIDDLE;
    public static final String POSITION_END = IActionConstants.MB_ADDITIONS_END;

    public enum Type {
        editor,
        folder
    }

    private String id;
    private AbstractDescriptor.ObjectType editorType;
    private AbstractDescriptor.ObjectType contributorType;
    private AbstractDescriptor.ObjectType inputFactoryType;
    private boolean main;
    private String name;
    private String description;
    private String position;
    private DBPImage icon;
    private Type type;
    private boolean embeddable;

    EntityEditorDescriptor()
    {
        super(UINavigatorActivator.PLUGIN_ID);
        this.id = DEFAULT_OBJECT_EDITOR_ID;
        this.editorType = new AbstractDescriptor.ObjectType(ObjectPropertiesEditor.class.getName());
        this.contributorType = null;
        this.inputFactoryType = new AbstractDescriptor.ObjectType(ObjectPropertiesEditorInputFactory.class.getName());
        this.main = true;
        this.name = UINavigatorMessages.registry_entity_editor_descriptor_name;
        this.description = UINavigatorMessages.registry_entity_editor_descriptor_description;
        this.position = null;
        this.icon = DBIcon.TREE_DATABASE;
        this.type = Type.editor;
    }

    public EntityEditorDescriptor(IConfigurationElement config)
    {
        super(config);

        this.id = config.getAttribute("id");
        this.editorType = new AbstractDescriptor.ObjectType(config.getAttribute("class"));
        this.contributorType = new AbstractDescriptor.ObjectType(config.getAttribute("contributor"));
        this.inputFactoryType = new AbstractDescriptor.ObjectType(config.getAttribute("inputFactory"));
        this.main = CommonUtils.getBoolean(config.getAttribute("main"));
        this.name = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.position = config.getAttribute("position");
        this.icon = iconToImage(config.getAttribute("icon"));
        String typeName = config.getAttribute("type");
        if (!CommonUtils.isEmpty(typeName)) {
            this.type = Type.valueOf(typeName);
        } else {
            this.type = Type.editor;
        }
        this.embeddable = CommonUtils.toBoolean(config.getAttribute("embeddable"));
    }

    public String getId()
    {
        return id;
    }

    public boolean isMain()
    {
        return main;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public String getPosition()
    {
        return position;
    }

    public DBPImage getIcon()
    {
        return icon;
    }

    public Type getType()
    {
        return type;
    }

    public boolean isEmbeddable() {
        return embeddable;
    }

    public Class<? extends IEditorActionBarContributor> getContributorClass()
    {
        return contributorType == null || contributorType.getImplName() == null ? null : contributorType.getObjectClass(IEditorActionBarContributor.class);
    }

    public IEditorInput getNestedEditorInput(IDatabaseEditorInput mainInput)
    {
        if (inputFactoryType == null || inputFactoryType.getImplName() == null) {
            return mainInput;
        }
        try {
            IDatabaseEditorInputFactory instance = inputFactoryType.createInstance(IDatabaseEditorInputFactory.class);
            if (instance != null) {
                return instance.createNestedEditorInput(mainInput);
            }
        } catch (Exception e) {
            log.error("Error instantiating input factory", e);
        }
        return mainInput;
    }

    public IEditorPart createEditor()
    {
        try {
            return editorType.createInstance(IEditorPart.class);
        } catch (Exception ex) {
            log.error("Error instantiating entity editor '" + editorType.getImplName() + "'", ex); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }
    }

    @Override
    public String toString() {
        return id;
    }
}
