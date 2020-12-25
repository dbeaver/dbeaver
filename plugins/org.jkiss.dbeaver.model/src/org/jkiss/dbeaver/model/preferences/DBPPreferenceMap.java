package org.jkiss.dbeaver.model.preferences;

import java.util.Map;

public interface DBPPreferenceMap {

    <T> T getObject(String name);

    Map<String, Object> getPropertyMap();

}
