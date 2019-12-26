package org.jkiss.dbeaver.ext.test.swtbot;

import static org.eclipse.swtbot.swt.finder.waits.Conditions.shellCloses;   // not referenced or required

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;   // not referenced or required
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;   // not referenced or required
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;   // not referenced or required

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
    	SWTBotShell dialog = bot.shell();
    	dialog.activate();
		bot.toolbarDropDownButtonWithTooltip("Новое соединение").menuItem("MariaDB").click();
		bot.tabItem("Общее").activate();
		bot.button("Finish").click();
    }
    
}
