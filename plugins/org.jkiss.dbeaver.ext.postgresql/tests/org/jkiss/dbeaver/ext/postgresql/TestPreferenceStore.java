package org.jkiss.dbeaver.ext.postgresql;

import org.jkiss.dbeaver.model.impl.preferences.SimplePreferenceStore;

import java.io.IOException;

public class TestPreferenceStore extends SimplePreferenceStore {

    @Override
    public void save() throws IOException {}

    @Override
    public String getString(String name) {
        return "";
    }
}
