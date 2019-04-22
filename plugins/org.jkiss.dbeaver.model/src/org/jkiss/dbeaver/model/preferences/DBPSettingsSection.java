package org.jkiss.dbeaver.model.preferences;

public interface DBPSettingsSection {

    String getName();

    DBPSettingsSection getSection(String sectionName);
    DBPSettingsSection[] getSections();

    DBPSettingsSection addNewSection(String name);
    void addSection(DBPSettingsSection section);

    String get(String key);
    String[] getArray(String key);
    boolean getBoolean(String key);
    double getDouble(String key) throws NumberFormatException;
    float getFloat(String key) throws NumberFormatException;
    int getInt(String key) throws NumberFormatException;
    long getLong(String key) throws NumberFormatException;

    void put(String key, String[] value);
    void put(String key, double value);
    void put(String key, float value);
    void put(String key, int value);
    void put(String key, long value);
    void put(String key, String value);
    void put(String key, boolean value);

}
