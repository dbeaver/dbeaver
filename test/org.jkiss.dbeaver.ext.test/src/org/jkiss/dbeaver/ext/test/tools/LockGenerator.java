package org.jkiss.dbeaver.ext.test.tools;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class LockGenerator {
	
	public static final int MAX_SESSIONS=79;
	
	public static final int MIN_CHAIN_SIZE = 2;
	public static final int MAX_CHAIN_SIZE = 4;
	
	public static final int MAX_LEVEL_ITEMS = 2;
	
	private static int getPid(Connection conn) throws SQLException {
		
		
		PreparedStatement stmt = null;
		ResultSet res = null;
		
		try {
			
			stmt =  conn.prepareStatement("SELECT pg_backend_pid()");
			res = stmt.executeQuery();
			res.next();
			return res.getInt(1);
			
		} finally {
			
			if (res != null) res.close();
			if (stmt != null) stmt.close();
			
		}

		
	}

	public static void main(String[] args) {
		
		  final String url = "jdbc:postgresql://localhost/postgres";
		  final Properties props = new Properties();
		  props.setProperty("user","");
		  props.setProperty("password","");
		  Connection conn = null;
		  PreparedStatement stmt = null;
		  ResultSet res = null;
		
		  
		  try {
		       conn = DriverManager.getConnection(url, props);
	     	   conn.setAutoCommit(false);
	     	   stmt =  conn.prepareStatement("SELECT EXISTS (SELECT 1 FROM   information_schema.tables  WHERE  table_schema = current_schema AND    table_name = 'usr')");
			   res = stmt.executeQuery();
			   res.next();
			   if (!res.getBoolean(1)){
				   System.out.println("Table not found");
				   stmt =  conn.prepareStatement("create table usr(field INTEGER,v INTEGER, s VARCHAR)");
				   stmt.execute();
				   stmt =  conn.prepareStatement("insert into usr(field,s) SELECT b,(SELECT string_agg(x, '')FROM (SELECT chr(ascii('A') + (random() * 25)::integer) FROM generate_series(1, 1024 + b * 0)) AS y(x)) s FROM generate_series(1,10000) as a(b)");
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
