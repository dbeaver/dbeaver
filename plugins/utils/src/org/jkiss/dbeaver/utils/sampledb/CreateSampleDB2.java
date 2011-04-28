/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.utils.sampledb;

import java.sql.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

/*
select x.start_date,(select max(s3.event_date) from term_status s3 where s3.term_id=1 and s3.TERM_STATUS=1 and s3.event_date<x.end_date ),
count(*) as event_count
from (
	SELECT
		s1.EVENT_DATE as start_date,
		(select min(s2.event_date) from term_status s2 where s2.event_date > s1.event_date and s2.TERM_STATUS=0) as end_date
	FROM test.term_status s1
	where s1.term_id=1 and s1.term_status=1
	order by s1.event_date
) x
where x.end_date is not null
group by x.end_date
having event_count>1
*/

/**
 * Sample DB creator
 */
public class CreateSampleDB2 {

    private final Connection dbCon;

    public CreateSampleDB2(Connection dbCon)
    {
        this.dbCon = dbCon;
    }

    private void run()
        throws Exception
    {
        Random rnd = new Random(System.currentTimeMillis());
        PreparedStatement dbStat = dbCon.prepareStatement("CREATE TABLE TERM_STATUS(" +
            "TERM_ID INTEGER NOT NULL," +
            "TERM_STATUS INTEGER NOT NULL," +
            "EVENT_DATE TIMESTAMP NOT NULL," +
            "PRIMARY KEY (TERM_ID,EVENT_DATE))");
        dbStat.execute();

        for (int termId = 1; termId <= 1; termId++) {
            long startDate = System.currentTimeMillis();
            for (int eventId = 0; eventId < 1000; eventId++) {
                int status = rnd.nextBoolean() ? 1 : 0;
                startDate += rnd.nextInt(2 * 24 * 60 * 60 * 1000);

                dbStat = dbCon.prepareStatement("INSERT INTO TERM_STATUS(" +
                    "TERM_ID,TERM_STATUS,EVENT_DATE) VALUES (?,?,?)");
                dbStat.setInt(1, termId);
                dbStat.setInt(2, status);
                dbStat.setTimestamp(3, new Timestamp(startDate));
                dbStat.execute();
            }
        }

    }

    public static void main(String[] args) throws Exception
    {
        Class.forName("com.mysql.jdbc.Driver");

        Properties connectionProps = new Properties();
        connectionProps.put("user", "root");
        connectionProps.put("password", "1978");
        final String url = "jdbc:mysql://localhost/test";
        final Connection connection = DriverManager.getConnection(url, connectionProps);
        try {
            System.out.println("Connected to '" + url + "'");
            final DatabaseMetaData metaData = connection.getMetaData();
            System.out.println("Driver: " + metaData.getDriverName() + " " + metaData.getDriverVersion());
            System.out.println("Database: " + metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion());

            CreateSampleDB2 runner = new CreateSampleDB2(connection);
            runner.run();
        } finally {
            connection.close();
        }
    }

}
