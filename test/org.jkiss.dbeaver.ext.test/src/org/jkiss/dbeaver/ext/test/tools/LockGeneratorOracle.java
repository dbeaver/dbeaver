package org.jkiss.dbeaver.ext.test.tools;

import java.sql.*;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class LockGeneratorOracle {
	
	public static final int MAX_SESSIONS=89;
	
	public static final int MIN_CHAIN_SIZE = 5;
	public static final int MAX_CHAIN_SIZE = 10;
	
	public static final int MAX_LEVEL_ITEMS = 3;
	
	private static int getPid(Connection conn) throws SQLException {

		try (PreparedStatement stmt = conn.prepareStatement("select sid from v$session where audsid = userenv('sessionid')")) {
			try (ResultSet res = stmt.executeQuery()) {
				res.next();
				return res.getInt(1);
			}
		}
	}

	public static void main(String[] args) {
		  final String url = "jdbc:oracle:thin:@[SERVER]:1521/[SID]";
		  final Properties props = new Properties();
		  props.setProperty("user","user");
		  props.setProperty("password","pwd");
		  Connection conn = null;
		  PreparedStatement stmt = null;
		  ResultSet res = null;

		  
		  try {
		       conn = DriverManager.getConnection(url, props);
	     	   conn.setAutoCommit(false);
	     	   stmt =  conn.prepareStatement("select count(*) c from dba_tables where table_name = 'USR' and owner = 'SCHEMA'");
			   res = stmt.executeQuery();
			   res.next();
			   if (res.getInt(1) != 1){
				   System.out.println("Table not found");
				   stmt =  conn.prepareStatement("create table usr(field NUMBER(11,0),v NUMBER(11,0), s VARCHAR2(1024))");
				   stmt.execute();
				   stmt =  conn.prepareStatement("insert into usr(field,s) select rownum r,DBMS_RANDOM.STRING('U',1024) from dual connect by rownum <= 10000");
				   stmt.execute();
				   stmt =  conn.prepareStatement("alter table usr add primary key (field)");
				   stmt.execute();
	               conn.commit();
	               System.out.println("Table created");
			   }
		   
		   ExecutorService service = Executors.newFixedThreadPool(MAX_SESSIONS);
		   
		   int sessionCount=0;
		   int field = 1;
		   
		   while(sessionCount < MAX_SESSIONS) {
			   
			   final int fieldVal = field;
			
			   service.submit(new Runnable() {
				
				@Override
				public void run() {
					 Connection c = null;
					 PreparedStatement s = null;
					 ResultSet r = null;
				     try {
						c = DriverManager.getConnection(url, props);
				     	c.setAutoCommit(false);
				     	String pid = String.valueOf(getPid(c));
				     	System.out.println("["+pid+"] Submited root session for "+String.valueOf(fieldVal));
				     	s =  c.prepareStatement("/*ROOT "+String.valueOf(fieldVal)+" */ update usr set v = 100500 where field = ?");
				     	s.setInt(1, fieldVal);
				     	s.executeUpdate();	
				     	while(true){
				     		try {
								Thread.sleep(600 * 1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
								break;
							}
				     	}
				     	c.close();

					} catch (SQLException e) {
						e.printStackTrace();
						return;
					}

					
					
				}
			});
			   
			sessionCount++;
			
			if ((MAX_SESSIONS - sessionCount) > MIN_CHAIN_SIZE) { 

				int chainCount = ThreadLocalRandom.current().nextInt(MIN_CHAIN_SIZE ,MAX_CHAIN_SIZE + 1);
				
				if ((MAX_SESSIONS - sessionCount) >= chainCount) {
					
					for(int i =0; i < chainCount;i++){
					
						 final int level = i;
						 
						 int levelCount = ThreadLocalRandom.current().nextInt(1 ,MAX_LEVEL_ITEMS + 1);
						 
						 for(int j = 0; j < levelCount;j++) { 
							 
						   final int levelNo = j;
						   
						   service.submit(new Runnable() {
							
							@Override
							public void run() {
								
								 try {
									Thread.sleep(5000);
								} catch (InterruptedException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								 Connection c = null;
								 PreparedStatement s = null;
								 ResultSet r = null;
								try {								 
									c = DriverManager.getConnection(url, props);
							     	c.setAutoCommit(false);
									int pid = getPid(c);
									String prefix;
									if (levelNo > 0) {
										int sublock = MAX_SESSIONS + (level * MAX_CHAIN_SIZE);
										prefix = String.format("[%d] Sublock %d for %d -> %d (%d) ",pid,sublock,fieldVal,level,levelNo);
										s =  c.prepareStatement("/*"+prefix + "*/ update usr set v = 100500 where field = ?");
										System.out.println("Sublock for "+prefix);
										s.setInt(1, sublock);
										s.executeUpdate();
									} 
									prefix = String.format("[%d] %d->%d (%d) ",pid, fieldVal,level,levelNo);																									     	
									s =  c.prepareStatement("/*"+prefix + "*/ update usr set v = 100500 where field = ?");
									s.setInt(1, fieldVal);
									System.out.println("Wait session for "+prefix);
							     	s.executeUpdate();
							     	c.close();

								} catch (SQLException e) {
									e.printStackTrace();
									return;
								}

								
								
							}
						});
						   
						sessionCount++;   
						
						if (sessionCount >= MAX_SESSIONS) {
							break;
						}
						
					}
						
						 if (sessionCount >= MAX_SESSIONS) {
								break;
							}
					}
					
				}
			 
			}
			
			
			 field++;
		   }
		   
		System.out.println("Sbmited "+sessionCount);
		service.shutdown();
		service.awaitTermination(1, TimeUnit.HOURS);
		   
		} catch (Exception e) {
			e.printStackTrace();
		}
		  

	}

}
