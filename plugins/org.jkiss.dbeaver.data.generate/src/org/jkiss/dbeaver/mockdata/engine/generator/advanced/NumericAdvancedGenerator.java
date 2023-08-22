// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.generator.advanced;

import java.io.IOException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.mockdata.engine.MockDataUtils;
import org.jkiss.dbeaver.mockdata.engine.generator.AbstractMockValueGenerator;
import org.jkiss.utils.CommonUtils;
import java.util.Map;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;

public class NumericAdvancedGenerator extends AbstractMockValueGenerator
{
    private Double min;
    private Double max;
    private Integer precision;
    private Integer scale;
    
    @Override
    public void init(final DBSDataManipulator container, final DBSAttributeBase attribute, final Map<String, Object> properties) throws DBException {
        super.init(container, attribute, properties);
        Double d = this.getDoubleProperty(properties, "minimum");
        if (d != null) {
            this.min = d;
        }
        d = this.getDoubleProperty(properties, "maximum");
        if (d != null) {
            this.max = d;
        }
        final Object p = properties.get("precision");
        if (p != null) {
            this.precision = CommonUtils.toInt(p);
        }
        final Object s = properties.get("scale");
        if (s != null) {
            this.scale = CommonUtils.toInt(s);
        }
    }
    
    @Override
    protected Object generateOneValue(final DBRProgressMonitor monitor) throws DBException, IOException {
        if (this.isGenerateNULL()) {
            return null;
        }
        Integer attrScale = this.attribute.getScale();
        if (this.scale != null) {
            if (attrScale == null) {
                attrScale = this.scale;
            }
            else if (attrScale > this.scale) {
                attrScale = this.scale;
            }
        }
        Integer attrPrecision = this.attribute.getPrecision();
        if (this.precision != null) {
            if (attrPrecision == null) {
                attrPrecision = this.precision;
            }
            else if (attrPrecision > this.precision) {
                attrPrecision = this.precision;
            }
        }
        return MockDataUtils.generateNumeric(attrPrecision, attrScale, this.min, this.max);
    }
}
