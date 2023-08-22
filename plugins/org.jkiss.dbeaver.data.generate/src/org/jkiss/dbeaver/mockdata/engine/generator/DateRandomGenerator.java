// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.generator;

import java.io.IOException;
import java.util.Date;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.DBException;
import java.text.ParseException;
import java.util.Map;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import java.text.SimpleDateFormat;
import org.jkiss.dbeaver.Log;

public class DateRandomGenerator extends AbstractMockValueGenerator
{
    private static final Log log;
    private static SimpleDateFormat DATE_FORMAT;
    public static final long DEFAULT_START_DATE = -946771200000L;
    public static final long DAY_RANGE = 86400000L;
    public static final long YEAR_RANGE = 31536000000L;
    public static final long DEFAULT_RANGE = 3153600000000L;
    private long startDate;
    private long endDate;
    
    static {
        log = Log.getLog((Class)DateRandomGenerator.class);
        DateRandomGenerator.DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
    }
    
    public DateRandomGenerator() {
        this.startDate = Long.MAX_VALUE;
        this.endDate = Long.MAX_VALUE;
    }
    
    @Override
    public void init(final DBSDataManipulator container, final DBSAttributeBase attribute, final Map<String, Object> properties) throws DBException {
        super.init(container, attribute, properties);
        final String fromDate = (String) properties.get("startDate");
        if (fromDate != null) {
            try {
                this.startDate = DateRandomGenerator.DATE_FORMAT.parse(fromDate).getTime();
            }
            catch (ParseException e) {
                DateRandomGenerator.log.error((Object)("Error parse Start Date '" + fromDate + "'."), (Throwable)e);
            }
        }
        final String toDate = (String) properties.get("endDate");
        if (toDate != null) {
            try {
                this.endDate = DateRandomGenerator.DATE_FORMAT.parse(toDate).getTime();
            }
            catch (ParseException e2) {
                DateRandomGenerator.log.error((Object)("Error parse End Date '" + toDate + "'."), (Throwable)e2);
            }
        }
        if (this.startDate != Long.MAX_VALUE && this.endDate != Long.MAX_VALUE && this.startDate > this.endDate) {
            final long l = this.startDate;
            this.startDate = this.endDate;
            this.endDate = l;
        }
        if (this.endDate != Long.MAX_VALUE) {
            this.endDate += 86399999L;
        }
    }
    
    public Object generateOneValue(final DBRProgressMonitor monitor) throws DBException, IOException {
        if (this.isGenerateNULL()) {
            return null;
        }
        if (this.startDate != Long.MAX_VALUE && this.endDate != Long.MAX_VALUE) {
            return new Date(this.startDate + Math.abs(this.random.nextLong()) % (this.endDate - this.startDate));
        }
        if (this.startDate != Long.MAX_VALUE) {
            return new Date(this.startDate + Math.abs(this.random.nextLong()) % 3153600000000L);
        }
        if (this.endDate != Long.MAX_VALUE) {
            return new Date(this.endDate - 3153600000000L + Math.abs(this.random.nextLong()) % 3153600000000L);
        }
        return new Date(-946771200000L + Math.abs(this.random.nextLong()) % 3153600000000L);
    }
}
