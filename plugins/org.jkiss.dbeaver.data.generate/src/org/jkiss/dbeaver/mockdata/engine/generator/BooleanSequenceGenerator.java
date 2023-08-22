
package org.jkiss.dbeaver.mockdata.engine.generator;

import java.io.IOException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.DBException;
import java.util.Map;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.mockdata.engine.internal.MockDataMessages;

public class BooleanSequenceGenerator extends AbstractMockValueGenerator
{
    private boolean value;
    private ORDER order;
    
    @Override
    public void init(final DBSDataManipulator container, final DBSAttributeBase attribute, final Map<String, Object> properties) throws DBException {
        super.init(container, attribute, properties);
        final String o = (String) properties.get("order");
        if (o != null) {
            this.order = ORDER.find(o);
        }
        final Boolean initial = (Boolean) properties.get("initial");
        if (initial != null) {
            this.value = !initial;
        }
    }
    
    public Object generateOneValue(final DBRProgressMonitor monitor) throws DBException, IOException {
        if (this.isGenerateNULL()) {
            return null;
        }
        switch (this.order) {
            case CONSTANT: {
                return this.value;
            }
            case ALTERNATELY: {
                this.value = !this.value;
                return this.value;
            }
            default: {
                return null;
            }
        }
    }
    
    private enum ORDER
    {
        ALTERNATELY("ALTERNATELY", 0, MockDataMessages.tools_mockdata_generator_boolean_sequence_prop_order_value_alternately), 
        CONSTANT("CONSTANT", 1, MockDataMessages.tools_mockdata_generator_boolean_sequence_prop_order_value_constant);
        
        private String label;
        
        private ORDER(final String name, final int ordinal, final String label) {
            this.label = label;
        }
        
        @Override
        public String toString() {
            return this.label;
        }
        
        public static ORDER find(final String label) {
            ORDER[] values;
            for (int length = (values = values()).length, i = 0; i < length; ++i) {
                final ORDER order = values[i];
                if (order.label.equalsIgnoreCase(label)) {
                    return order;
                }
            }
            return null;
        }
    }
}
