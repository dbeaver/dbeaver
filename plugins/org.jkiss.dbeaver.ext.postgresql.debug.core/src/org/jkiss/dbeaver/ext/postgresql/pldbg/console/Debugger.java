package org.jkiss.dbeaver.ext.postgresql.pldbg.console;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;

import org.jkiss.dbeaver.ext.postgresql.pldbg.DebugException;
import org.jkiss.dbeaver.ext.postgresql.pldbg.impl.DebugManagerPostgres;
import org.jkiss.dbeaver.ext.postgresql.pldbg.impl.DebugObjectPostgres;
import org.jkiss.dbeaver.ext.postgresql.pldbg.impl.SessionInfoPostgres;

@SuppressWarnings("nls")
public class Debugger {
	
    
    /*
     * * проверить стек вызова (pldbg_get_stack),
* перемещаться по функциям в стеке (pldbg_select_frame),
* смотреть значения параметров и локальных переменных текущей функции в стеке
  (pldbg_get_variables),
* изменить значения переменных (pldbg_deposit_value),
* установить/удалить точки останова
  (pldbg_set_breakpoint/pldbg_drop_breakpoint),
* продолжить выполнение до следующей точки (pldbg_continue),
* выполнять пошаговую отладку с заходом внутрь функций (pldbg_step_into),
* или выполняя строку целиком (pldbg_step_over),
* в конечном итоге, прекратить сеанс отладки (pldbg_abort_target).
     */
	
	public static final String PROMPT = ">";	
	public static final String COMMAND_STACK = "S";
	public static final String COMMAND_FRAME = "F";
	public static final String COMMAND_VARIABLES = "V";
	public static final String COMMAND_VARIABLE_SET = "=";
	public static final String COMMAND_BREAKPOINT = "B";
	public static final String COMMAND_BREAKPOINT_LIST = "L";
	public static final String COMMAND_CONTINUE = "C";
	public static final String COMMAND_INTO = "I";
	public static final String COMMAND_OVER = "O";
	public static final String COMMAND_TERMINATE = "E";
	public static final String COMMAND_SESSIONS = "W";
	public static final String COMMAND_OBJ = "D";
	public static final String COMMAND_HELP = "?";
	
	public static final String ANY_ARG = "*";

	public static void main(String[] args) throws DebugException {
		
		String url = "jdbc:postgresql://10.0.3.36/fsv_dev?user=fsv&password=fsv&ssl=false"; //"jdbc:postgresql://localhost/postgres?user=postgres&password=postgres&ssl=false";
		
		Connection conn;
		DebugManagerPostgres pgDbgManager; 
		try {
			
			conn = DriverManager.getConnection(url);
			
		} catch (SQLException e) {
			e.printStackTrace();
			return;
		}
		
  		 pgDbgManager = new DebugManagerPostgres(conn);

		 Scanner sc = new Scanner(System.in);
		 Scanner scArg;
		 String command;
		 while(true) {
		 System.out.print(PROMPT);
		    command = sc.next();
		    switch (command.toUpperCase()) {
		    
		    case COMMAND_HELP:
		    	System.out.println("W Show sessions");
		    	System.out.println("D Show debug objects");
				System.out.println("S Stack");
				System.out.println("F Frame");
				System.out.println("V Variables");
				System.out.println("= Set Variables");
				System.out.println("L List breakpoint(s)");
				System.out.println("B Set breakpoint");
				System.out.println("C Continue execution");
				System.out.println("I Step into");
				System.out.println("O Step over");
				System.out.println("E Exit debugger");
				System.out.println("? This help");
				break;
		    
			case COMMAND_STACK:
				System.out.println("STACK!!!");
				break;

			case COMMAND_FRAME:
				System.out.println("FRAME!!!");
				break;

			case COMMAND_VARIABLES:
				System.out.println("VARIABLES!!!");
				break;

			case COMMAND_VARIABLE_SET:
				System.out.println("VARIABLE_SET!!!");
				break;

			case COMMAND_BREAKPOINT:
				System.out.println("BREAKPOINT!!!");
				break;
				
			case COMMAND_BREAKPOINT_LIST:
				System.out.println("BREAKPOINT LIST!!!");
				break;	

			case COMMAND_CONTINUE:
				System.out.println("CONTINUE!!!");
				break;

			case COMMAND_INTO:
				System.out.println("STEP INTO !!!");
				break;

			case COMMAND_OVER:
				System.out.println("STEP OVER!!!");
				break;
				
			case COMMAND_SESSIONS:
				System.out.println("SESSIONS!!!");
				 for (SessionInfoPostgres s : pgDbgManager.getSessions()) {
					 System.out.println(s);
				 }
				break;	
				
			case COMMAND_OBJ:
				String proc = ANY_ARG;
				String owner= ANY_ARG;
				
				String arg =sc.nextLine();
				
				if (arg.length() > 0) {
					
					scArg = new Scanner(arg);
					
					if (scArg.hasNext()) {
					
						proc = scArg.next();
						
						if (scArg.hasNext()) {
							
							arg = scArg.nextLine();
							
							if (arg.length() > 0) {
								owner = arg;	
							}
							
						}
						
					}
					scArg.close();
					
				}
				
				
				for (DebugObjectPostgres o : pgDbgManager.getObjects(owner.equals(ANY_ARG) ? "_" : owner, proc.equals(ANY_ARG) ? "_" : proc)) {
					 System.out.println(o);
				 }
				
				break;	

			case COMMAND_TERMINATE:
				//System.out.println("EXIT !!!");
				//break;
				System.out.println("EXIT.....");
				return;

			default:
				System.out.println(String.format("Unnown command '%s' for command list type ?", command));
				break;
			}
			 /*while(sc.hasNext()) {
				 
			 }*/
		 }

	}

}
