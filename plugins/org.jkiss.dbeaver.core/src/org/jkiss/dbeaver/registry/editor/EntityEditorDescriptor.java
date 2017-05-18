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
package org.jkiss.dbeaver.registry.editor;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.registry.AbstractContextDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInputFactory;
import org.jkiss.dbeaver.ui.editors.entity.properties.ObjectPropertiesEditor;
import org.jkiss.dbeaver.ui.editors.entity.properties.ObjectPropertiesEditorInputFactory;
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
    private ObjectType editorType;
    private ObjectType contributorType;
    private ObjectType inputFactoryType;
    private boolean main;
    private String name;
    private String description;
    private String position;
    private DBPImage icon;
    private Type type;
    private boolean embeddable;

    EntityEditorDescriptor()
    {
        super(null);
        this.id = DEFAULT_OBJECT_EDITOR_ID;
        this.editorType = new ObjectType(ObjectPropertiesEditor.class.getName());
        this.contributorType = null;
        this.inputFactoryType = new ObjectType(ObjectPropertiesEditorInputFactory.class.getName());
        this.main = true;
        this.name = CoreMessages.registry_entity_editor_descriptor_name;
        this.description = CoreMessages.registry_entity_editor_descriptor_description;
        this.position = null;
        this.icon = DBIcon.TREE_DATABASE;
        this.type = Type.editor;
    }

    public EntityEditorDescriptor(IConfigurationElement config)
    {
        super(config);

        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.editorType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CLASS));
        this.contributorType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_CONTRIBUTOR));
        this.inputFactoryType = new ObjectType(config.getAttribute(RegistryConstants.ATTR_INPUT_FACTORY));
        this.main = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_MAIN));
        this.name = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.position = config.getAttribute(RegistryConstants.ATTR_POSITION);
        this.icon = iconToImage(config.getAttribute(RegistryConstants.ATTR_ICON));
        String typeName = config.getAttribute(RegistryConstants.ATTR_TYPE);
        if (!CommonUtils.isEmpty(typeName)) {
            this.type = Type.valueOf(typeName);
        } else {
            this.type = Type.editor;
        }
        this.embeddable = CommonUtils.toBoolean(config.getAttribute(RegistryConstants.ATTR_EMBEDDABLE));
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
