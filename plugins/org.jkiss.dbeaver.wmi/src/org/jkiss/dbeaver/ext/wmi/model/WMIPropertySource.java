package org.jkiss.dbeaver.ext.wmi.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.jkiss.dbeaver.ui.properties.DefaultPropertyLabelProvider;
import org.jkiss.wmi.service.WMIException;
import org.jkiss.wmi.service.WMIQualifiedObject;
import org.jkiss.wmi.service.WMIQualifier;

import java.util.Collection;

/**
 * Property source based on WMI qualifiers
 */
public abstract class WMIPropertySource implements IPropertySource
{
    static final Log log = LogFactory.getLog(WMIPropertySource.class);
    private static final IPropertyDescriptor[] EMPTY_PROPERTIES = new IPropertyDescriptor[0];

    protected abstract WMIQualifiedObject getQualifiedObject();

    public Object getEditableValue()
    {
        return this;//getQualifiedObject();
    }

    public IPropertyDescriptor[] getPropertyDescriptors()
    {
        try {
            WMIQualifiedObject qualifiedObject = getQualifiedObject();
            if (qualifiedObject == null) {
                return EMPTY_PROPERTIES;
            }
            Collection<WMIQualifier> qualifiers = qualifiedObject.getQualifiers();
            IPropertyDescriptor[] result = new IPropertyDescriptor[qualifiers.size()];
            int index = 0;
            for (WMIQualifier qualifier : qualifiers) {
                String name = qualifier.getName();
                PropertyDescriptor prop = new PropertyDescriptor(name, name);
                prop.setLabelProvider(DefaultPropertyLabelProvider.INSTANCE);
                prop.setCategory("WMI");
                result[index++] = prop;
            }
            return result;
        } catch (WMIException e) {
            log.error(e);
            return EMPTY_PROPERTIES;
        }
    }

    public Object getPropertyValue(Object id)
    {
        try {
            return getQualifiedObject().getQualifier(id.toString());
        } catch (WMIException e) {
            log.error(e);
            return null;
        }
    }

    public boolean isPropertySet(Object id)
    {
        try {
            return getQualifiedObject().getQualifier(id.toString()) != null;
        } catch (WMIException e) {
            log.error(e);
            return false;
        }
    }

    public void resetPropertyValue(Object id)
    {

    }

    public void setPropertyValue(Object id, Object value)
    {

    }

}
