// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.generator;

import java.io.IOException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.DBException;
import java.util.Date;
import java.text.ParseException;
import java.util.Map;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import java.text.SimpleDateFormat;
import org.jkiss.dbeaver.Log;

public class DateSequenceGenerator extends AbstractMockValueGenerator
{
    private static final Log log;
    private static SimpleDateFormat DATE_FORMAT;
    public static final long DAY_RANGE = 86400000L;
    private long startDate;
    private boolean reverse;
    private long step;
    
    static {
        log = Log.getLog((Class)DateSequenceGenerator.class);
        DateSequenceGenerator.DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
    }
    
    public DateSequenceGenerator() {
        this.startDate = Long.MAX_VALUE;
        this.reverse = false;
        this.step = 86400000L;
    }
    
    @Override
    public void init(final DBSDataManipulator container, final DBSAttributeBase attribute, final Map<String, Object> properties) throws DBException {
        super.init(container, attribute, properties);
        final String fromDate = (String) properties.get("startDate");
        if (fromDate != null) {
            try {
                this.startDate = DateSequenceGenerator.DATE_FORMAT.parse(fromDate).getTime();
            }
            catch (ParseException e) {
                DateSequenceGenerator.log.error((Object)("Error parse Start Date '" + fromDate + "'."), (Throwable)e);
            }
        }
        if (this.startDate == Long.MAX_VALUE) {
            this.startDate = new Date().getTime();
        }
        final Integer step = (Integer) properties.get("step");
        if (step != null) {
            this.step = step * 86400000L;
        }
        final Boolean reverse = (Boolean) properties.get("reverse");
        if (reverse != null) {
            this.reverse = reverse;
        }
    }
    
    public Object generateOneValue(final DBRProgressMonitor monitor) throws DBException, IOException {
        if (this.isGenerateNULL()) {
            return null;
        }
        final long value = this.startDate;
        if (this.reverse) {
            this.startDate -= this.step;
        }
        else {
            this.startDate += this.step;
        }
        return new Date(value);
    }
}
