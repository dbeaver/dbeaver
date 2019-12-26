package org.jkiss.dbeaver.ext.test.swtbot;


import static org.junit.Assert.assertTrue;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class SWTbotTest {
    private static SWTWorkbenchBot bot;
    @BeforeClass
    public static void initBot() {
        bot = new SWTWorkbenchBot();	
    }
    
    @AfterClass
    public static void afterClass() {
    	bot.resetWorkbench();
    }
 
    @Test
    public void testSampleMenu() {
		bot.toolbarDropDownButtonWithTooltip("Новое соединение").menuItem("MariaDB").click();
		bot.tabItem("Общее").activate();
		bot.button("Finish").click();
		
		assertTrue(true);
    }
    
}
