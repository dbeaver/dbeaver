// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.generator;

import java.io.IOException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import java.text.ParseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataKind;
import java.util.Map;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import java.text.SimpleDateFormat;

public class ConstantGenerator extends AbstractMockValueGenerator
{
    private static SimpleDateFormat DATE_FORMAT;
    private Object value;
    
    static {
        ConstantGenerator.DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
    }
    
    @Override
    public void init(final DBSDataManipulator container, final DBSAttributeBase attribute, final Map<String, Object> properties) throws DBException {
        super.init(container, attribute, properties);
        final Object value = properties.get("value");
        if (value != null) {
            if (attribute.getDataKind() == DBPDataKind.DATETIME) {
                try {
                    this.value = ConstantGenerator.DATE_FORMAT.parse((String)value);
                    return;
                }
                catch (ParseException e) {
                    throw new DBException("Can't parse the value '" + value + "' as a date", (Throwable)e);
                }
            }
            this.value = value;
        }
        else if (attribute.getDataKind() == DBPDataKind.STRING) {
            this.value = "";
        }
    }
    
    public Object generateOneValue(final DBRProgressMonitor monitor) throws DBException, IOException {
        if (this.isGenerateNULL()) {
            return null;
        }
        return this.value;
    }
}
