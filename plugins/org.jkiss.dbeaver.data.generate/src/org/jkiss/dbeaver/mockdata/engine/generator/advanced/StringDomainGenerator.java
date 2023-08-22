// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.generator.advanced;

import java.io.IOException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import java.util.List;
import org.jkiss.dbeaver.Log;

public class StringDomainGenerator extends AdvancedStringValueGenerator
{
    private static final Log log;
    private static List<String> DOMAINS_NAMES;
    private static int domains;
    
    static {
        log = Log.getLog((Class)StringDomainGenerator.class);
    }
    
    public Object generateOneValue(final DBRProgressMonitor monitor) throws DBException, IOException {
        if (StringDomainGenerator.domains == 0) {
            StringDomainGenerator.DOMAINS_NAMES = this.readDict("domains.txt");
            StringDomainGenerator.domains = StringDomainGenerator.DOMAINS_NAMES.size();
        }
        if (this.isGenerateNULL()) {
            return null;
        }
        return this.tune(StringDomainGenerator.DOMAINS_NAMES.get(this.random.nextInt(StringDomainGenerator.domains)));
    }
}
