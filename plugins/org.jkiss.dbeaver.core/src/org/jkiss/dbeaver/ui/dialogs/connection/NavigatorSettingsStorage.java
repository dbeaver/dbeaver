package org.jkiss.dbeaver.ui.dialogs.connection;

import org.jkiss.dbeaver.model.navigator.DBNBrowseSettings;

public interface NavigatorSettingsStorage {
    DBNBrowseSettings getNavigatorSettings();

    void setNavigatorSettings(DBNBrowseSettings settings);
}
