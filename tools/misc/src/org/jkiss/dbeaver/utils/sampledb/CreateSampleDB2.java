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

import java.sql.*;
import java.util.Properties;
import java.util.Random;

/*
select
	min(x.start_date),
	max(x.start_date),
	count(*) as event_count
from (
	SELECT
		s1.EVENT_DATE as start_date,
		(select min(s2.event_date) from term_status s2 where s2.terminal_id=1 and s2.event_date > s1.event_date and s2.status=0) as end_date
	FROM test.term_status s1
	where s1.terminal_id=1 and s1.status=1
	order by s1.event_date
) x
group by x.end_date
having event_count>1


select days.day,TIME(stats.period_start),TIME(stats.period_end) from
(
	SELECT @row := @row + 1 as day
	FROM information_schema.TABLES t, (SELECT @row := 0) r
	where @row < DAY(LAST_DAY(NOW()))
) days
LEFT OUTER JOIN (
	select
		min(x.start_date) as period_start,
		max(x.start_date) as period_end,
		count(*) as event_count
	from (
		SELECT
			s1.EVENT_DATE as start_date,
			(select min(s2.event_date) from term_status s2 where s2.terminal_id=1 and s2.event_date > s1.event_date and s2.status=0) as end_date
		FROM test.term_status s1
		where s1.terminal_id=1 and s1.status=1
		order by s1.event_date
	) x
	group by x.end_date
	having event_count>1
) stats ON DAY(stats.period_start)=days.day
order by days.day,stats.period_start
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
            "TERMINAL_ID INTEGER NOT NULL," +
            "STATUS INTEGER NOT NULL," +
            "EVENT_DATE TIMESTAMP NOT NULL," +
            "PRIMARY KEY (TERMINAL_ID,EVENT_DATE))");
        dbStat.execute();

        for (int termId = 1; termId <= 1; termId++) {
            long startDate = System.currentTimeMillis();
            for (int eventId = 0; eventId < 1000; eventId++) {
                int status = rnd.nextBoolean() ? 1 : 0;
                startDate += rnd.nextInt(2 * 60 * 60 * 1000);

                dbStat = dbCon.prepareStatement("INSERT INTO TERM_STATUS(" +
                    "TERMINAL_ID,STATUS,EVENT_DATE) VALUES (?,?,?)");
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
