// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.generator.advanced;

import java.io.IOException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.mockdata.engine.generator.AbstractStringValueGenerator;
import org.jkiss.dbeaver.mockdata.engine.generator.advanced.regex.Xeger;
import org.jkiss.utils.CommonUtils;
import java.util.Map;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;

public class StringRegexGenerator extends AbstractStringValueGenerator
{
    private Xeger xeger;
    
    @Override
    public void init(final DBSDataManipulator container, final DBSAttributeBase attribute, final Map<String, Object> properties) throws DBException {
        super.init(container, attribute, properties);
        String regex = CommonUtils.toString(properties.get("regex"));
        if (CommonUtils.isEmpty(regex)) {
            regex = "[a-zA-Z0-9]*";
        }
        this.xeger = new Xeger(regex);
    }
    
    @Override
    protected Object generateOneValue(final DBRProgressMonitor monitor) throws DBException, IOException {
        if (this.isGenerateNULL()) {
            return null;
        }
        return this.tune(this.xeger.generate());
    }
}
