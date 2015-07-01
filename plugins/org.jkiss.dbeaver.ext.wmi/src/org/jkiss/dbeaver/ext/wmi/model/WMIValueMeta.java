package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.model.data.DBDValueMeta;
import org.jkiss.wmi.service.WMIQualifier;

import java.util.Collection;

/**
 * Value meta information
 */
public class WMIValueMeta implements DBDValueMeta {

    private Collection<WMIQualifier> qualifiers;

    public WMIValueMeta(Collection<WMIQualifier> qualifiers)
    {
        this.qualifiers = qualifiers;
    }

    public Collection<WMIQualifier> getQualifiers()
    {
        return qualifiers;
    }
}
