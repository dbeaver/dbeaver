/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.registry.editor;

import org.jkiss.dbeaver.core.Log;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.DBeaverConstants;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.ext.IDatabaseEditorInputFactory;
import org.jkiss.dbeaver.registry.AbstractContextDescriptor;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.editors.entity.properties.ObjectPropertiesEditor;
import org.jkiss.dbeaver.ui.editors.entity.properties.ObjectPropertiesEditorInputFactory;
import org.jkiss.utils.CommonUtils;

/**
 * EntityEditorDescriptor
 */
public class EntityEditorDescriptor extends AbstractContextDescriptor
{
    static final Log log = Log.getLog(EntityEditorDescriptor.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.databaseEditor"; //NON-NLS-1 //$NON-NLS-1$

    public static final String DEFAULT_OBJECT_EDITOR_ID = "default.object.editor"; //NON-NLS-1 //$NON-NLS-1$

    public static final String POSITION_PROPS = IActionConstants.MB_ADDITIONS_PROPS;
    public static final String POSITION_START = IActionConstants.MB_ADDITIONS_START;
    public static final String POSITION_MIDDLE = IActionConstants.MB_ADDITIONS_MIDDLE;
    public static final String POSITION_END = IActionConstants.MB_ADDITIONS_END;
    private boolean embeddable;

    public static enum Type {
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
    private Image icon;
    private Type type;

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
        this.icon = DBIcon.TREE_DATABASE.getImage();
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

    public Image getIcon()
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

}
