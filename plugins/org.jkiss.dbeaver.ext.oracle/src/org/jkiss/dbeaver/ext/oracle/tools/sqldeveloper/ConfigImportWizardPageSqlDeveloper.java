/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.ext.oracle.tools.sqldeveloper;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.import_config.wizards.ConfigImportWizardPage;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportConnectionInfo;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportData;
import org.jkiss.dbeaver.ext.oracle.Activator;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FilenameFilter;
import java.util.LinkedHashMap;
import java.util.Map;


public class ConfigImportWizardPageSqlDeveloper extends ConfigImportWizardPage {

    public static final String SQLD_HOME_FOLDER = "SQL Developer";
    public static final String SQLD_CONFIG_FILE = "connections.xml";

    public static final String SQLD_SYSCONFIG_FOLDER = "system";
    public static final String SQLD_CONNECTIONS_FOLDER = "o.jdeveloper.db.connection.";

    protected ConfigImportWizardPageSqlDeveloper()
    {
        super("SQLDeveloper");
        setTitle("SQL Developer");
        setDescription("Import Oracle SQL Developer connections");
        setImageDescriptor(Activator.getImageDescriptor("icons/sqldeveloper_big.png"));
    }

    @Override
    protected void loadConnections(ImportData importData) throws DBException {
        File homeFolder = RuntimeUtils.getUserHomeDir();
        File sqlDevHome = new File(homeFolder, "AppData/Roaming/" + SQLD_HOME_FOLDER);
        if (!sqlDevHome.exists()) {
            sqlDevHome = new File(homeFolder, "Application Data/" + SQLD_HOME_FOLDER);
            if (!sqlDevHome.exists()) {
                throw new DBException("SQL Developer installation not found");
            }
        }
        final File[] sysConfFolders = sqlDevHome.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains(SQLD_SYSCONFIG_FOLDER);
            }
        });
        if (sysConfFolders == null || sysConfFolders.length == 0) {
            throw new DBException("SQL Developer config not found");
        }
        for (File sysConfFolder : sysConfFolders) {
            final File[] connectionFolders = sysConfFolder.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.contains(SQLD_CONNECTIONS_FOLDER);
                }
            });
            if (connectionFolders == null || connectionFolders.length != 1) {
                continue;
            }
            final File connectionFolder = connectionFolders[0];
            final File connectionsFile = new File(connectionFolder, SQLD_CONFIG_FILE);
            if (!connectionsFile.exists()) {
                continue;
            }
            parseConnections(connectionsFile, importData);
        }
    }

    private void parseConnections(File connectionsFile, ImportData importData) throws DBException {
        try {
            Document configDocument = XMLUtils.parseDocument(connectionsFile);

            for (Element refElement : XMLUtils.getChildElementList(configDocument.getDocumentElement(), "Reference")) {
                final String conName = refElement.getAttribute("name");
                if (CommonUtils.isEmpty(conName)) {
                    continue;
                }

                final Map<String, String> propsMap = new LinkedHashMap<>();
                final Element refAddressesElement = XMLUtils.getChildElement(refElement, "RefAddresses");
                if (refAddressesElement != null) {
                    for (Element refAddr : XMLUtils.getChildElementList(refAddressesElement, "StringRefAddr")) {
                        String addrType = refAddr.getAttribute("addrType");
                        String addrContent = XMLUtils.getChildElementBody(refAddr, "Contents");
                        if (!CommonUtils.isEmpty(addrType) && !CommonUtils.isEmpty(addrContent)) {
                            propsMap.put(addrType, addrContent);
                        }
                    }
                }
                String host = propsMap.get("hostname");
                String port = propsMap.get("port");
                String sid = propsMap.get("sid");
                String serviceName = propsMap.get("serviceName");
                String user = propsMap.get("user");
                String role = propsMap.get("role");
                String osAuth = propsMap.get("OS_AUTHENTICATION");
                String url = propsMap.get("customUrl");

                if (CommonUtils.isEmpty(host) && CommonUtils.isEmpty(url)) {
                    continue;
                }
                //ImportConnectionInfo connectionInfo = new ImportConnectionInfo();
                //connectionInfo.setProperty();
                //importData.addConnection(connectionInfo);
            }
        } catch (XMLException e) {
            throw new DBException("Configuration parse error: " + e.getMessage());
        }
    }

}
