package org.jkiss.dbeaver.utils;

public class GlobalPreferenceStore extends AbstractPreferenceStore{


    @Override
    public boolean contains(String name) {
        return false;
    }

    @Override
    public boolean getBoolean(String name) {
        return false;
    }

    @Override
    public double getDouble(String name) {
        return 0;
    }

    @Override
    public float getFloat(String name) {
        return 0;
    }

    @Override
    public int getInt(String name) {
        return 0;
    }

    @Override
    public long getLong(String name) {
        return 0;
    }

    @Override
    public String getString(String name) {
        return null;
    }

    @Override
    public boolean getDefaultBoolean(String name) {
        return false;
    }

    @Override
    public double getDefaultDouble(String name) {
        return 0;
    }

    @Override
    public float getDefaultFloat(String name) {
        return 0;
    }

    @Override
    public int getDefaultInt(String name) {
        return 0;
    }

    @Override
    public long getDefaultLong(String name) {
        return 0;
    }

    @Override
    public String getDefaultString(String name) {
        return null;
    }

    @Override
    public boolean isDefault(String name) {
        return false;
    }

    @Override
    public boolean needsSaving() {
        return false;
    }

    @Override
    public void setDefault(String name, double value) {

    }

    @Override
    public void setDefault(String name, float value) {

    }

    @Override
    public void setDefault(String name, int value) {

    }

    @Override
    public void setDefault(String name, long value) {

    }

    @Override
    public void setDefault(String name, String defaultObject) {

    }

    @Override
    public void setDefault(String name, boolean value) {

    }

    @Override
    public void setToDefault(String name) {

    }

    @Override
    public void setValue(String name, double value) {

    }

    @Override
    public void setValue(String name, float value) {

    }

    @Override
    public void setValue(String name, int value) {

    }

    @Override
    public void setValue(String name, long value) {

    }

    @Override
    public void setValue(String name, String value) {

    }

    @Override
    public void setValue(String name, boolean value) {

    }
}
