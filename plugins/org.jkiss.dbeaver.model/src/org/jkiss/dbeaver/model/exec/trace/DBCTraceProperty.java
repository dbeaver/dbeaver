package org.jkiss.dbeaver.model.exec.trace;

import org.jkiss.dbeaver.model.meta.Property;

public interface DBCTraceProperty {

    @Property(name = "Name", viewable = true, order = 1)
    public String getName();

    @Property(name = "Value", viewable = true, order = 2)
    public String getValue();

    @Property(name = "Description", viewable = true, order = 3)
    public String getDescription();
}
