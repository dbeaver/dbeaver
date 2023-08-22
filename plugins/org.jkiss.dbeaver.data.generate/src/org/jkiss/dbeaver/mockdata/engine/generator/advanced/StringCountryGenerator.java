// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.generator.advanced;

import java.io.IOException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import java.util.List;
import org.jkiss.dbeaver.Log;

public class StringCountryGenerator extends AdvancedStringValueGenerator
{
    private static final Log log;
    private static List<String> COUNTRIES;
    private static int countries;
    
    static {
        log = Log.getLog((Class)StringCountryGenerator.class);
    }
    
    public Object generateOneValue(final DBRProgressMonitor monitor) throws DBException, IOException {
        if (StringCountryGenerator.countries == 0) {
            StringCountryGenerator.COUNTRIES = this.readDict("countries_en.txt");
            StringCountryGenerator.countries = StringCountryGenerator.COUNTRIES.size();
        }
        if (this.isGenerateNULL()) {
            return null;
        }
        return this.tune(StringCountryGenerator.COUNTRIES.get(this.random.nextInt(StringCountryGenerator.countries)));
    }
}
