package org.jkiss.dbeaver.registry;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.core.DBeaverCore;

/**
 * EntityEditorDescriptor
 */
public class EntityEditorDescriptor extends AbstractDescriptor
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.databaseEditor";

    public static final String POSITION_START = "additions_start";
    public static final String POSITION_END = "additions_end";

    private EntityEditorsRegistry registry;

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

    public EntityEditorDescriptor(EntityEditorsRegistry registry, IConfigurationElement config)
    {
        super(config.getContributor());
        this.registry = registry;

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

    public EntityEditorsRegistry getRegistry()
    {
        return registry;
    }

    public String getId()
    {
        return id;
    }

    public String getClassName()
    {
        return className;
    }

    public String getObjectType()
    {
        return objectType;
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

    public boolean appliesToType(Class objectType)
    {
        return this.getObjectClass() != null && this.getObjectClass().isAssignableFrom(objectType);
    }

    public Class getObjectClass()
    {
        if (objectClass == null) {
            try {
                objectClass = DBeaverCore.getInstance().getPlugin().getBundle().loadClass(objectType);
            } catch (ClassNotFoundException ex) {
                // do nothing
            }
            if (objectClass == null) {
                try {
                    objectClass = getContributorBundle().loadClass(objectType);
                } catch (ClassNotFoundException ex) {
                    log.error("Can't determine object class '" + objectType + "'", ex);
                }
            }
        }
        return objectClass;
    }

    public Class getEditorClass()
    {
        if (editorClass == null) {
            try {
                editorClass = getContributorBundle().loadClass(className);
            } catch (ClassNotFoundException ex) {
                log.error("Can't load editor class '" + className + "'", ex);
            }
        }
        return editorClass;
    }

    public IEditorPart createEditor()
    {
        if (getEditorClass() == null) {
            return null;
        }
        try {
            return (IEditorPart)getEditorClass().newInstance();
        } catch (Exception ex) {
            log.error("Error instantiating entity editor '" + className + "'", ex);
            return null;
        }
    }
}
