package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.dbeaver.model.data.DBDDataFormatterSample;

import java.util.Locale;
import java.util.Map;

public class NumberFormatSample implements DBDDataFormatterSample {

    public Map<String, String> getDefaultProperties(Locale locale)
    {
        return null;
    }

    public Object getSampleValue()
    {
        return 1234567890.012345;
    }

}
