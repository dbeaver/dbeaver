// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.generator.advanced;

import java.io.IOException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.DBException;
import java.util.Map;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import java.util.Random;
import java.util.List;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.mockdata.engine.MockDataUtils;

public class StringEmailGenerator extends StringNameGenerator
{
    private static final Log log;
    private static List<String> MAIL_DOMAINS;
    private static int mailDomains;
    private Random random;
    private int numericSuffixSize;
    
    static {
        log = Log.getLog((Class)StringEmailGenerator.class);
    }
    
    public StringEmailGenerator() {
        this.random = new Random();
        this.numericSuffixSize = 2;
    }
    
    @Override
    public void init(final DBSDataManipulator container, final DBSAttributeBase attribute, final Map<String, Object> properties) throws DBException {
        super.init(container, attribute, properties);
        final Integer suffix = (Integer) properties.get("numericSuffixSize");
        if (suffix != null) {
            this.numericSuffixSize = suffix;
        }
    }
    
    @Override
    public Object generateOneValue(final DBRProgressMonitor monitor) throws DBException, IOException {
        if (StringEmailGenerator.mailDomains == 0) {
            StringEmailGenerator.MAIL_DOMAINS = this.readDict("mail_domains.txt");
            StringEmailGenerator.mailDomains = StringEmailGenerator.MAIL_DOMAINS.size();
        }
        final Object value = super.generateOneValue(monitor);
        if (value == null) {
            return null;
        }
        String name = (String)value;
        if (this.numericSuffixSize > 0) {
            name = String.valueOf(name) + String.format(".%0" + this.numericSuffixSize + "d", MockDataUtils.getRandomInt(0, MockDataUtils.degree(this.numericSuffixSize), this.random));
        }
        final String mailSuffix = "@" + StringEmailGenerator.MAIL_DOMAINS.get(this.random.nextInt(StringEmailGenerator.mailDomains));
        if (this.withSurnames) {
            return String.valueOf(name.replace(' ', '.')) + mailSuffix;
        }
        return String.valueOf(name) + mailSuffix;
    }
}
