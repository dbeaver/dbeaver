package org.jkiss.dbeaver.ext.test.swtbot;

import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellCloses;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

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
    	// assertNotNull();
		bot.toolbarDropDownButtonWithTooltip("Новое соединение").menuItem("MariaDB").click();
		bot.tabItem("Общее").activate();
		bot.button("Finish").click();
    }
    
}