/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.ui.DBeaverConstants;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * EntityEditorDescriptor
 */
public class EntityEditorDescriptor extends AbstractDescriptor
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.databaseEditor";

    public static final String POSITION_START = "additions_start";
    public static final String POSITION_END = "additions_end";

    private String id;
    private String className;
    private String objectType;
    private boolean main;
    private String name;
    private String description;
    private String position;
    private Image icon;

    private Class objectClass;
    private Class editorClass;

    EntityEditorDescriptor()
    {
        super(new IContributor() {
            public String getName() {
                return DBeaverConstants.PLUGIN_ID;
            }
        });
        this.id = "default.object.editor";
        this.className = org.jkiss.dbeaver.ui.editors.entity.DefaultObjectEditor.class.getName();
        this.objectType = DBSObject.class.getName();
        this.main = true;
        this.name = "Properties";
        this.description = "Object properties";
        this.position = null;
        this.icon = DBIcon.TREE_DATABASE.getImage();
    }

    public EntityEditorDescriptor(IConfigurationElement config)
    {
        super(config.getContributor());

        this.id = config.getAttribute("id");
        this.className = config.getAttribute("class");
        this.objectType = config.getAttribute("objectType");
        this.main = "true".equals(config.getAttribute("main"));
        this.name = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.position = config.getAttribute("position");
        String iconPath = config.getAttribute("icon");
        if (!CommonUtils.isEmpty(iconPath)) {
            this.icon = iconToImage(iconPath);
        }
    }

    public String getId()
    {
        return id;
    }

/*
    public String getClassName()
    {
        return className;
    }

    public String getObjectType()
    {
        return objectType;
    }
*/

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

    public boolean appliesToType(Class objectType)
    {
        return this.getObjectClass() != null && this.getObjectClass().isAssignableFrom(objectType);
    }

    public Class getObjectClass()
    {
        if (objectClass == null) {
            objectClass = getObjectClass(objectType);
        }
        return objectClass;
    }

    public Class getEditorClass()
    {
        if (editorClass == null) {
            editorClass = getObjectClass(className);
        }
        return editorClass;
    }

    public IEditorPart createEditor()
    {
        Class clazz = getEditorClass();
        if (clazz == null) {
            return null;
        }
        try {
            return (IEditorPart)clazz.newInstance();
        } catch (Exception ex) {
            log.error("Error instantiating entity editor '" + className + "'", ex);
            return null;
        }
    }
}
