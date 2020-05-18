package org.jkiss.dbeaver.ext.test;

import java.util.Properties;

public class SQLServerFetchTest {

    public static void main(String[] args) throws Exception {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        Properties props = new Properties();
        props.put("integratedSecurity", "true");
        Connection dbCon = DriverManager.getConnection("jdbc:sqlserver://;serverName=localhost;databaseName=master", props);
        dbCon.setAutoCommit(false);
        try (Statement dbStat = dbCon.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            int rsCount = 0;

            if (dbStat.execute("sp_help TestTable")) {
                dumpResultSet(dbStat, rsCount);
                rsCount++;
            }
            while (dbStat.getMoreResults() || dbStat.getUpdateCount() != -1) {
                dumpResultSet(dbStat, rsCount);
                rsCount++;
            }
            System.out.println("Total result = " + rsCount);
        }
    }


    public static void dumpResultSet(Statement dbStat, int number) throws SQLException {
        System.out.println("================================================ " + number);
        try (ResultSet dbResult = dbStat.getResultSet()) {
            ResultSetMetaData md = dbResult.getMetaData();
            int count = md.getColumnCount();
            dumpResultSetMetaData(dbResult);
            while (dbResult.next()) {
                for (int i = 1; i <= count; i++) {
                    String colValue = dbResult.getString(i);
                    System.out.print(colValue + "\t");
                }
                System.out.println();
            }
            System.out.println();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void dumpResultSetMetaData(ResultSet dbResult)
    {
        try {
            ResultSetMetaData md = dbResult.getMetaData();
            int count = md.getColumnCount();
            for (int i = 1; i <= count; i++) {
                System.out.print(md.getColumnName(i) + " [" + md.getColumnTypeName(i) + "]\t");
            }
            System.out.println();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
