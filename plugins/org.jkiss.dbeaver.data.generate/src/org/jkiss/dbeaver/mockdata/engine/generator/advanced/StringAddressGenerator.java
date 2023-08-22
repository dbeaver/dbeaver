package org.jkiss.dbeaver.mockdata.engine.generator.advanced;

import java.io.IOException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import java.util.List;
import org.jkiss.dbeaver.Log;

public class StringAddressGenerator extends AdvancedStringValueGenerator
{
    private static final Log log;
    private static List<String> ADDRESSES;
    private static int addresses;
    
    static {
        log = Log.getLog((Class)StringAddressGenerator.class);
    }
    
    public Object generateOneValue(final DBRProgressMonitor monitor) throws DBException, IOException {
        if (StringAddressGenerator.addresses == 0) {
            StringAddressGenerator.ADDRESSES = this.readDict("random_addresses_en.txt");
            StringAddressGenerator.addresses = StringAddressGenerator.ADDRESSES.size();
        }
        if (this.isGenerateNULL()) {
            return null;
        }
        return this.tune(StringAddressGenerator.ADDRESSES.get(this.random.nextInt(StringAddressGenerator.addresses)));
    }
}
