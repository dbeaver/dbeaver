package org.jkiss.dbeaver.tools.compare;

import org.jkiss.dbeaver.ui.properties.ObjectPropertyDescriptor;

/**
* Report property
*/
class CompareReportProperty {
    ObjectPropertyDescriptor property;
    Object[] values;

    public CompareReportProperty(ObjectPropertyDescriptor property)
    {
        this.property = property;
    }
}
