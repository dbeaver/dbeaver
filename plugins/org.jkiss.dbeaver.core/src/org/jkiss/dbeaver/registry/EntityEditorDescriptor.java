/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlContext;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverConstants;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.editors.entity.ObjectPropertiesEditor;
import org.jkiss.utils.CommonUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.ui.DBIcon;

import java.util.ArrayList;
import java.util.List;

/**
 * EntityEditorDescriptor
 */
public class EntityEditorDescriptor extends AbstractDescriptor
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.databaseEditor"; //NON-NLS-1

    public static final String DEFAULT_OBJECT_EDITOR_ID = "default.object.editor"; //NON-NLS-1

    public static final String POSITION_PROPS = IActionConstants.MB_ADDITIONS_PROPS;
    public static final String POSITION_START = IActionConstants.MB_ADDITIONS_START;
    public static final String POSITION_MIDDLE = IActionConstants.MB_ADDITIONS_MIDDLE;
    public static final String POSITION_END = IActionConstants.MB_ADDITIONS_END;

    private String id;
    private String className;
    private List<ObjectType> objectTypes = new ArrayList<ObjectType>();
    private boolean main;
    private String name;
    private String description;
    private String position;
    private Image icon;

    //private List<Class<?>> objectClasses;
    private Class<?> editorClass;

    private class ObjectType {
        String implName;
        Class<?> implClass;
        Expression expression;

        private ObjectType(String implName)
        {
            this.implName = implName;
        }

        private ObjectType(IConfigurationElement cfg)
        {
            this.implName = cfg.getAttribute(RegistryConstants.ATTR_NAME);
            String condition = cfg.getAttribute(RegistryConstants.ATTR_IF);
            if (!CommonUtils.isEmpty(condition)) {
                try {
                    this.expression = RuntimeUtils.parseExpression(condition);
                } catch (DBException ex) {
                    log.warn("Can't parse object type expression: " + condition, ex);
                }
            }
        }

        public boolean appliesTo(DBPObject object)
        {
            if (implClass == null) {
                implClass = getObjectClass(implName);
            }
            if (implClass == null) {
                return false;
            }
            if (!implClass.isAssignableFrom(object.getClass())) {
                return false;
            }
            if (expression != null) {
                Object result = expression.evaluate(makeContext(object));
                return Boolean.TRUE.equals(result);
            }
            return true;
        }

        private JexlContext makeContext(final DBPObject object)
        {
            return new JexlContext() {
                public Object get(String name)
                {
                    return name.equals("object") ? object : null;
                }

                public void set(String name, Object value)
                {
                    log.warn("Set is not implemented");
                }

                public boolean has(String name)
                {
                    return name.equals("object") && object != null;
                }
            };
        }
    }

    EntityEditorDescriptor()
    {
        super(new IContributor() {
            public String getName() {
                return DBeaverConstants.PLUGIN_ID;
            }
        });
        this.id = DEFAULT_OBJECT_EDITOR_ID;
        this.className = ObjectPropertiesEditor.class.getName();
        this.main = true;
        this.name = "Properties";
        this.description = "Object properties";
        this.position = null;
        this.icon = DBIcon.TREE_DATABASE.getImage();
    }

    public EntityEditorDescriptor(IConfigurationElement config)
    {
        super(config.getContributor());

        this.id = config.getAttribute(RegistryConstants.ATTR_ID);
        this.className = config.getAttribute(RegistryConstants.ATTR_CLASS);
        this.main = CommonUtils.getBoolean(config.getAttribute(RegistryConstants.ATTR_MAIN));
        this.name = config.getAttribute(RegistryConstants.ATTR_LABEL);
        this.description = config.getAttribute(RegistryConstants.ATTR_DESCRIPTION);
        this.position = config.getAttribute(RegistryConstants.ATTR_POSITION);
        String iconPath = config.getAttribute(RegistryConstants.ATTR_ICON);
        if (!CommonUtils.isEmpty(iconPath)) {
            this.icon = iconToImage(iconPath);
        }

        {
            String objectType = config.getAttribute(RegistryConstants.ATTR_OBJECT_TYPE);
            if (objectType != null) {
                objectTypes.add(new ObjectType(objectType));
            }
        }
        IConfigurationElement[] typesCfg = config.getChildren(RegistryConstants.TAG_OBJECT_TYPE);
        if (typesCfg != null) {
            for (IConfigurationElement typeCfg : typesCfg) {
                objectTypes.add(new ObjectType(typeCfg));
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

    public boolean appliesTo(DBPObject object)
    {
        for (ObjectType objectType : objectTypes) {
            if (objectType.appliesTo(object)) {
                return true;
            }
        }
        return false;
    }

/*
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
*/

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
