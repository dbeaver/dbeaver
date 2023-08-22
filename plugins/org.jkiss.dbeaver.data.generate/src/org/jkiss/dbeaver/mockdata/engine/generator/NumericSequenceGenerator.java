// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.generator;

import java.io.IOException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.mockdata.engine.MockDataUtils;

import java.util.Map;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;

public class NumericSequenceGenerator extends AbstractMockValueGenerator
{
    private long start;
    private long step;
    private boolean reverse;
    
    public NumericSequenceGenerator() {
        this.start = 1L;
        this.step = 1L;
        this.reverse = false;
    }
    
    @Override
    public void init(final DBSDataManipulator container, final DBSAttributeBase attribute, final Map<String, Object> properties) throws DBException {
        super.init(container, attribute, properties);
        final Long startProperty = this.getLongProperty(properties, "start");
        if (startProperty != null) {
            this.start = startProperty;
        }
        final Long stepProperty = this.getLongProperty(properties, "step");
        if (stepProperty != null) {
            this.step = stepProperty;
        }
        Boolean reverse = (Boolean) properties.get("reverse");
        if (reverse != null) {
            this.reverse = reverse;
        }
    }
    
    public Object generateOneValue(final DBRProgressMonitor monitor) throws DBException, IOException {
        if (this.isGenerateNULL()) {
            return null;
        }
        final long value = this.start;
        if (this.reverse) {
            this.start -= this.step;
        }
        else {
            this.start += this.step;
        }
        final Integer precision = this.attribute.getPrecision();
        if (precision == null || precision < MockDataUtils.INTEGER_PRECISION) {
            return (int)value;
        }
        if (precision < MockDataUtils.BYTE_PRECISION) {
            return (byte)value;
        }
        if (precision < MockDataUtils.SHORT_PRECISION) {
            return (short)value;
        }
        if (precision < MockDataUtils.LONG_PRECISION) {
            return new Long(value);
        }
        return value;
    }
}
