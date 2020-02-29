/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.help;

import org.eclipse.help.IContext;
import org.eclipse.help.IHelpResource;
import org.eclipse.ui.help.AbstractHelpUI;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * Lightweight help UI
 */
public class GitHubWikiHelpUI extends AbstractHelpUI {

    private static final Log log = Log.getLog(GitHubWikiHelpUI.class);
    public static final String GITHUB_HELP_ROOT = "https://github.com/dbeaver/dbeaver/wiki/";

    @Override
    public void displayHelp()
    {
        showHelpPage(GITHUB_HELP_ROOT);
    }

    @Override
    public void displayDynamicHelp() {
        displayHelp();
    }

    @Override
    public void displayContext(IContext context, int x, int y)
    {
        try {
            IHelpResource[] relatedTopics = context.getRelatedTopics();
            if (relatedTopics == null || relatedTopics.length == 0) {
                return;
            }
            IHelpResource relatedTopic = relatedTopics[0];
            String topicRef = relatedTopic.getHref();
            //Cut plugin ID from href
            while (topicRef.startsWith("/")) {
                topicRef = topicRef.substring(1);
            }
            int divPos = topicRef.indexOf('/');
            if (divPos != -1) {
                topicRef = topicRef.substring(divPos + 1);
            }
            showHelpPage(GITHUB_HELP_ROOT + topicRef);

        } catch (Exception e) {
            log.error(e);
        }
    }

    private void showHelpPage(String fileURL) {
        UIUtils.launchProgram(fileURL);
    }

    @Override
    public void displayHelpResource(String href)
    {
        showHelpPage(href);
    }

    @Override
    public boolean isContextHelpDisplayed()
    {
        return false;
    }

}
