package org.jkiss.dbeaver.model.data;

import java.util.Locale;
import java.util.Map;

/**
 * Data formatter sample
 */
public interface DBDDataFormatterSample {

    Map<String, String> getDefaultProperties(Locale locale);

    Object getSampleValue();

}
