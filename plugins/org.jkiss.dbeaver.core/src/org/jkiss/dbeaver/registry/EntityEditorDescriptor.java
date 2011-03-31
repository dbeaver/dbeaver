/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * EntityEditorDescriptor
 */
public class EntityEditorDescriptor extends AbstractDescriptor
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.databaseEditor"; //NON-NLS-1

    public static final String POSITION_START = "additions_start"; //NON-NLS-1
    public static final String POSITION_END = "additions_end"; //NON-NLS-1

    private String id;
    private String className;
    private List<String> objectTypes = new ArrayList<String>();
    private boolean main;
    private String name;
    private String description;
    private String position;
    private Image icon;

    private List<Class<?>> objectClasses;
    private Class<?> editorClass;

    EntityEditorDescriptor()
    {
        super(new IContributor() {
            public String getName() {
                return DBeaverConstants.PLUGIN_ID;
            }
        });
        this.id = "default.object.editor";
        this.className = org.jkiss.dbeaver.ui.editors.entity.DefaultObjectEditor.class.getName();
        this.objectTypes = new ArrayList<String>();
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
        this.main = "true".equals(config.getAttribute("main"));
        this.name = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.position = config.getAttribute("position");
        String iconPath = config.getAttribute("icon");
        if (!CommonUtils.isEmpty(iconPath)) {
            this.icon = iconToImage(iconPath);
        }

        {
            String objectType = config.getAttribute("objectType");
            if (objectType != null) {
                objectTypes.add(objectType);
            }
        }
        IConfigurationElement[] typesCfg = config.getChildren("objectType");
        if (typesCfg != null) {
            for (IConfigurationElement typeCfg : typesCfg) {
                String objectType = typeCfg.getAttribute("name");
                if (objectType != null) {
                    objectTypes.add(objectType);
                }
            }
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
        List<Class<?>> objectClasses = this.getObjectClasses();
        for (Class<?> clazz : objectClasses) {
            if (clazz.isAssignableFrom(objectType)) {
                return true;
            }
        }
        return false;
    }

    public List<Class<?>> getObjectClasses()
    {
        if (objectClasses == null) {
            objectClasses = new ArrayList<Class<?>>();
            for (String objectType : objectTypes) {
                Class<?> objectClass = getObjectClass(objectType);
                if (objectClass != null) {
                    objectClasses.add(objectClass);
                }
            }
        }
        return objectClasses;
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
