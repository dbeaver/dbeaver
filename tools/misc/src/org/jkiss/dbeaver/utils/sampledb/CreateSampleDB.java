/*
 * Copyright (C) 2010-2014 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.utils.sampledb;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

/**
 * Sample DB creator
 */
public class CreateSampleDB {

    private static class TableInfo {
        String name;
        List<ColumnInfo> columns = new ArrayList<ColumnInfo>();
        List<TableInfo> relations = new ArrayList<TableInfo>();

        public TableInfo(String name)
        {
            this.name = name;
        }
    }

    private static class ColumnInfo {
        String name;
        String type;
        boolean fkColumn;

        public ColumnInfo(String name, String type)
        {
            this.name = name;
            this.type = type;
        }
    }

    private final Connection dbCon;
    private final Map<String ,String> params;
    private int tableCount;
    private int minColumns;
    private int maxColumns;
    private boolean createRelations;

    private Random random;
    private Map<String, TableInfo> tables = new TreeMap<String, TableInfo>();

    public CreateSampleDB(Connection dbCon, Map<String, String> params)
    {
        this.dbCon = dbCon;
        this.params = params;
        tableCount = getIntParam("tables", 100);
        minColumns = getIntParam("min-cols", 2);
        maxColumns = getIntParam("max-cols", 30);
        createRelations = getBoolParam("rels", false);

        random = new Random(System.currentTimeMillis());
    }

    private String getStringParam(String name, String defValue)
    {
        final String value = params.get(name);
        return value == null ? defValue : value;
    }

    private int getIntParam(String name, int defValue)
    {
        final String value = params.get(name);
        return value == null ? defValue : Integer.parseInt(value);
    }

    private boolean getBoolParam(String name, boolean defValue)
    {
        final String value = params.get(name);
        return value == null ? defValue : Boolean.valueOf(value);
    }

    private void run()
        throws Exception
    {
        final NumberFormat tableIdFormat = getNumberFormat(tableCount);
        final NumberFormat columnIdFormat = getNumberFormat(maxColumns);
        final String[] colTypes = new String[] {"INTEGER", "VARCHAR(32)", "DATE"};

        // Generate tables model

        for (int i = 1; i <= tableCount; i++) {
            TableInfo info = new TableInfo("Sample" + tableIdFormat.format(i));
            int colCount = minColumns + random.nextInt(maxColumns - minColumns);
            for (int k = 1; k <= colCount; k++) {
                ColumnInfo column = new ColumnInfo("Column" + columnIdFormat.format(k), colTypes[random.nextInt(colTypes.length)]);
                info.columns.add(column);
            }
            tables.put(info.name, info);
        }
        // Generate relations

        // Create tables
        for (TableInfo table : tables.values()) {
            StringBuilder sql = new StringBuilder("CREATE TABLE ");
            sql.append(table.name).append(" (\n");
            sql.append(table.name).append("_PK INTEGER NOT NULL,\n");
            for (ColumnInfo column : table.columns) {
                sql.append(column.name).append(" ").append(column.type).append(",\n");
            }
            sql.append("PRIMARY KEY (").append(table.name).append("_PK)\n");
            sql.append(" )");

            System.out.print("Create table '" + table.name + "'...");
            final PreparedStatement dbStat = dbCon.prepareStatement(sql.toString());
            dbStat.execute();
            dbStat.close();
            System.out.println("Done.");
        }
    }

    private NumberFormat getNumberFormat(int maxNumber)
    {
        StringBuilder pattern = new StringBuilder(10);
        final int digitCount = (int) Math.log10(maxNumber) + 1;
        for (int i = 0; i < digitCount; i++) {
            pattern.append("0");
        }
        return new DecimalFormat(pattern.toString());
    }

    public static void main(String[] args) throws Exception
    {
        if (args.length < 2) {
            System.out.println("Usage java CreateSampleDB driver-class jdbc-url [-u=<user> -p=<password> -tables=<count> -columns=<count> -relations=<count> -data=<true|false>]");
            return;
        }
        String driverClass = args[0];
        String url = args[1];
        Map<String ,String> params = new HashMap<String, String>();
        for (int i = 2; i < args.length; i ++) {
            String arg = args[i].trim();
            while (arg.startsWith("-")) {
                arg = arg.substring(1);
            }
            int divPos = arg.indexOf('=');
            if (divPos == -1) {
                System.out.println("Bad argument: " + arg);
                continue;
            }
            params.put(arg.substring(0, divPos).toLowerCase(), arg.substring(divPos + 1));
        }

        Class.forName(driverClass);

        Properties connectionProps = new Properties();
        if (params.get("u") != null) connectionProps.put("user", params.get("u"));
        if (params.get("p") != null) connectionProps.put("password", params.get("p"));
        final Connection connection = DriverManager.getConnection(url, connectionProps);
        try {
            System.out.println("Connected to '" + url + "'");
            final DatabaseMetaData metaData = connection.getMetaData();
            System.out.println("Driver: " + metaData.getDriverName() + " " + metaData.getDriverVersion());
            System.out.println("Database: " + metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion());

            CreateSampleDB runner = new CreateSampleDB(connection, params);
            runner.run();
        } finally {
            connection.close();
        }
    }

}
