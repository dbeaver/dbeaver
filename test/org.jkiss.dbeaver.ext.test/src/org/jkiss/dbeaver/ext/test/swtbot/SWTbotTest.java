/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.test.swtbot;


import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

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
 
    //For the script to work correctly, only the "DBeaver Sample Database (SQLite)" database must be connected in dbeaver before it is executed"
    @Test
    public void testSampleMenu() throws Exception {
    	assertTrue(true);
    	
		bot.toolbarDropDownButtonWithTooltip("Новое соединение").menuItem("PostgreSQL").click();
		bot.tabItem("Общее").activate();
		bot.button("Finish").click();
		bot.toolbarDropDownButtonWithTooltip("Новое соединение").menuItem("MariaDB").click();
		bot.tabItem("Общее").activate();
		bot.button("Finish").click();
		bot.tree().getTreeItem("MariaDB - localhost").select();
		bot.tree().contextMenu("Удалить").click();
		bot.button("Yes").click();
		bot.tree().getTreeItem("DBeaver Sample Database (SQLite)").expand();
		Thread.currentThread().sleep(3000);
		bot.tree().getTreeItem("DBeaver Sample Database (SQLite)").getNode("Таблицы").select();
		bot.tree().getTreeItem("DBeaver Sample Database (SQLite)").getNode("Таблицы").doubleClick();
		bot.tree().getTreeItem("DBeaver Sample Database (SQLite)").getNode("Таблицы").expand();
		Thread.currentThread().sleep(3000);
		bot.tree().getTreeItem("DBeaver Sample Database (SQLite)").getNode("Таблицы").getNode("Album").select();
		Thread.currentThread().sleep(3000);
		bot.tree().getTreeItem("DBeaver Sample Database (SQLite)").getNode("Таблицы").getNode("Album").expand();
		Thread.currentThread().sleep(3000);
		bot.tree().getTreeItem("DBeaver Sample Database (SQLite)").getNode("Таблицы").getNode("Album").getNode("Колонки").expand();
		Thread.currentThread().sleep(3000);
		bot.tree().getTreeItem("DBeaver Sample Database (SQLite)").getNode("Таблицы").getNode("Album").getNode("Колонки").getNode("Column1 (BLOB)").select();
		bot.tree().getTreeItem("DBeaver Sample Database (SQLite)").getNode("Таблицы").getNode("Album").getNode("Колонки").getNode("Column1 (BLOB)").doubleClick();
		Thread.currentThread().sleep(3000);
		bot.editorByTitle("Column1").show();

    }
    
}
