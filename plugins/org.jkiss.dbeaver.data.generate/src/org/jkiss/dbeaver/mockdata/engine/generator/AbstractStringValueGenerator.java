// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.generator;

import org.jkiss.dbeaver.DBException;
import java.util.Map;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;

public abstract class AbstractStringValueGenerator extends AbstractMockValueGenerator
{
    private boolean lowercase;
    private boolean uppercase;
    
    @Override
    public void init(final DBSDataManipulator container, final DBSAttributeBase attribute, final Map<String, Object> properties) throws DBException {
        super.init(container, attribute, properties);
        final Boolean lowercase = (Boolean) properties.get("lowercase");
        if (lowercase != null) {
            this.lowercase = lowercase;
        }
        final Boolean uppercase = (Boolean) properties.get("uppercase");
        if (uppercase != null) {
            this.uppercase = uppercase;
        }
    }
    
    protected String tune(final String value) {
        if (value == null) {
            return null;
        }
        if (this.uppercase) {
            return value.toUpperCase();
        }
        if (this.lowercase) {
            return value.toLowerCase();
        }
        return value;
    }
}
