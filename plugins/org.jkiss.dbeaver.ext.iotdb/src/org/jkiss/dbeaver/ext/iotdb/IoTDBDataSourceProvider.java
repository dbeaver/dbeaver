package org.jkiss.dbeaver.ext.iotdb;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.iotdb.model.IoTDBDataSource;
import org.jkiss.dbeaver.ext.iotdb.model.meta.IoTDBMetaModel;
import org.jkiss.dbeaver.ext.generic.GenericDataSourceProvider;
import org.jkiss.dbeaver.ext.iotdb.model.meta.IoTDBTableMetaModel;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DatabaseURL;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

public class IoTDBDataSourceProvider extends GenericDataSourceProvider {

    @NotNull
    @Override
    public DBPDataSource openDataSource(@NotNull DBRProgressMonitor monitor,
                                        @NotNull DBPDataSourceContainer container) throws DBException {
        String url = container.getConnectionConfiguration().getUrl();
        if (url.endsWith("?sql_dialect=table")) {
            return new IoTDBDataSource(monitor, container, new IoTDBTableMetaModel(), false);
        }
        return new IoTDBDataSource(monitor, container, new IoTDBMetaModel(), true);
    }

    private static String makePropPattern(String prop) {
        return "{" + prop + "}";
    }

    private boolean useRawUrl(DBPConnectionConfiguration connectionInfo) {
        return !CommonUtils.isEmpty(connectionInfo.getUrl()) &&
                CommonUtils.isEmpty(connectionInfo.getHostPort()) &&
                CommonUtils.isEmpty(connectionInfo.getHostName()) &&
                CommonUtils.isEmpty(connectionInfo.getServerName());
    }

    private String buildUrlFromTemplate(DBPConnectionConfiguration connectionInfo, String urlTemplate) throws DBException {
        DatabaseURL.MetaURL metaURL = DatabaseURL.parseSampleURL(urlTemplate);
        StringBuilder url = new StringBuilder();
        for (String component : metaURL.getUrlComponents()) {
            String newComponent = component;
            if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                newComponent = newComponent.replace(makePropPattern(DBConstants.PROP_HOST), connectionInfo.getHostName());
            }
            if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                newComponent = newComponent.replace(makePropPattern(DBConstants.PROP_PORT), connectionInfo.getHostPort());
            }
            if (!CommonUtils.isEmpty(connectionInfo.getServerName())) {
                newComponent = newComponent.replace(makePropPattern("sqlDialect"), connectionInfo.getServerName());
            }
            newComponent = newComponent.replace(makePropPattern(DBConstants.PROP_USER), CommonUtils.notEmpty(connectionInfo.getUserName()));

            if (newComponent.startsWith("[")) {
                if (!newComponent.equals(component)) {
                    url.append(newComponent.substring(1, newComponent.length() - 1));
                }
            } else {
                url.append(newComponent);
            }
        }
        return url.toString();
    }

    @Override
    public String getConnectionURL(DBPDriver driver,
                                   DBPConnectionConfiguration connectionInfo) {
        String urlTemplate = driver.getSampleURL();
        if (useRawUrl(connectionInfo)) {
            return connectionInfo.getUrl();
        }
        if (CommonUtils.isEmptyTrimmed(urlTemplate)) {
            return connectionInfo.getUrl();
        }

        try {
            return buildUrlFromTemplate(connectionInfo, urlTemplate);
        } catch (DBException e) {
            log.error(e);
            return null;
        }
    }
}
