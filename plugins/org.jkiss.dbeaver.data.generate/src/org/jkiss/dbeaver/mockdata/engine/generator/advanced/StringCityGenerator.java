// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.generator.advanced;

import java.io.IOException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import java.util.List;
import org.jkiss.dbeaver.Log;

public class StringCityGenerator extends AdvancedStringValueGenerator
{
    private static final Log log;
    private static List<String> CITIES;
    private static int cities;
    
    static {
        log = Log.getLog((Class)StringCityGenerator.class);
    }
    
    public Object generateOneValue(final DBRProgressMonitor monitor) throws DBException, IOException {
        if (StringCityGenerator.cities == 0) {
            StringCityGenerator.CITIES = this.readDict("cities_en.txt");
            StringCityGenerator.cities = StringCityGenerator.CITIES.size();
        }
        if (this.isGenerateNULL()) {
            return null;
        }
        return this.tune(StringCityGenerator.CITIES.get(this.random.nextInt(StringCityGenerator.cities)));
    }
}
