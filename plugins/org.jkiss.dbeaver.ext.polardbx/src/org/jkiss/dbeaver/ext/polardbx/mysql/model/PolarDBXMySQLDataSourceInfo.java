/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0
 */
package org.jkiss.dbeaver.ext.polardbx.mysql.model;

import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceInfo;
import org.osgi.framework.Version;

public class PolarDBXMySQLDataSourceInfo extends JDBCDataSourceInfo {

    private final PolarDBXMySQLDataSource dataSource;

    public PolarDBXMySQLDataSourceInfo(PolarDBXMySQLDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super(metaData);
        this.dataSource = dataSource;
    }

    @Override
    public boolean supportsMultipleResults() {
        return true;
    }

    @Override
    public boolean needsTableMetaForColumnResolution() {
        return true;
    }

    @Override
    public String getDatabaseProductVersion() {
        return dataSource.getServerVersion();
    }

    @Override
    public Version getDatabaseVersion() {
        try {
            String productVersion = this.getDatabaseProductVersion();
            if (productVersion == null) {
                return new Version(0, 0, 0);
            }

            // Make sure version detection has completed; if not detected yet, detect it first.
            if (dataSource.getServerVersion().isEmpty()) {
                // If the DataSource has not completed version detection, use the default version parsing logic.
                // In this case we cannot determine whether it is the Standard Edition, so use generic parsing.
                return parseVersionFromProductVersion(productVersion);
            }

            // If it is a PolarDB-X Standard Edition, use the Standard Edition version number.
            if (dataSource.isPolarDBXStandardEdition()) {
                String standardVersion = dataSource.getPolarDBXStandardVersion();
                if (standardVersion != null) {
                    String[] versionParts = standardVersion.split("\\.");
                    int major = Integer.parseInt(versionParts[0]);
                    int minor = versionParts.length > 1 ? Integer.parseInt(versionParts[1]) : 0;
                    int patch = versionParts.length > 2 ? Integer.parseInt(versionParts[2]) : 0;
                    return new Version(major, minor, patch);
                }
            }

            // Original TDDL version parsing logic (used for the regular edition).
            return parseVersionFromProductVersion(productVersion);
        } catch (Exception e) {
            return new Version(0, 0, 0);
        }
    }

    /**
     * Parse version information from the product version string.
     * @param productVersion the product version string
     * @return the version object
     */
    private Version parseVersionFromProductVersion(String productVersion) {
        try {
            String[] segments = productVersion.split("-");
            if (segments.length < 3 || !"TDDL".equalsIgnoreCase(segments[1])) {
                return new Version(0, 0, 0);
            }

            String polarDBXVersion = segments[2];
            String[] numericParts = polarDBXVersion.split("\\.");
            if (numericParts.length != 3) {
                return new Version(0, 0, 0);
            }

            int major = Integer.parseInt(numericParts[0]);
            int minor = Integer.parseInt(numericParts[1]);
            int patch = Integer.parseInt(numericParts[2]);
            return new Version(major, minor, patch);
        } catch (Exception e) {
            return new Version(0, 0, 0);
        }
    }

    /**
     * Get the database product name; the Standard Edition is shown as PolarDB-X (Standard Edition).
     */
    @Override
    public String getDatabaseProductName() {
        String baseName = super.getDatabaseProductName();
        if (dataSource.isPolarDBXStandardEdition()) {
            return baseName + " (Standard Edition)";
        }
        return baseName;
    }
}