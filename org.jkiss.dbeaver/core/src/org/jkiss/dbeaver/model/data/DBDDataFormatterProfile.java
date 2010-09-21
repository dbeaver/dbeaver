package org.jkiss.dbeaver.model.data;

import java.text.ParseException;
import java.util.Locale;

/**
 * Data formatter profile
 */
public interface DBDDataFormatterProfile {

    String getProfileName();

    DBDDataFormatter getFormatter(String typeId);
    
}
