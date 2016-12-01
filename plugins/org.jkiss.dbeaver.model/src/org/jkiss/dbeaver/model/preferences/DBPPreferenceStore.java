package org.jkiss.dbeaver.model.preferences;

import java.io.IOException;

public interface DBPPreferenceStore {

    boolean contains(String name);

    boolean getBoolean(String name);
    double getDouble(String name);
    float getFloat(String name);
    int getInt(String name);
    long getLong(String name);
    String getString(String name);

    boolean getDefaultBoolean(String name);
    double getDefaultDouble(String name);
    float getDefaultFloat(String name);
    int getDefaultInt(String name);
    long getDefaultLong(String name);
    String getDefaultString(String name);

    boolean isDefault(String name);

    boolean needsSaving();

    void setDefault(String name, double value);
    void setDefault(String name, float value);
    void setDefault(String name, int value);
    void setDefault(String name, long value);
    void setDefault(String name, String defaultObject);
    void setDefault(String name, boolean value);
    void setToDefault(String name);

    void setValue(String name, double value);
    void setValue(String name, float value);
    void setValue(String name, int value);
    void setValue(String name, long value);
    void setValue(String name, String value);
    void setValue(String name, boolean value);

    void addPropertyChangeListener(DBPPreferenceListener listener);
    void removePropertyChangeListener(DBPPreferenceListener listener);
    void firePropertyChangeEvent(String name, Object oldValue, Object newValue);

    void save() throws IOException;

}
