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
package org.jkiss.dbeaver.model.sql.completion;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.sql.SQLHelpProvider;
import org.jkiss.dbeaver.model.sql.SQLHelpTopic;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

/**
 * SQL Completion proposal
 */
public class SQLCompletionHelper {

    private static final Log log = Log.getLog(SQLCompletionHelper.class);

    public static final int ADDITIONAL_INFO_WAIT_TIMEOUT = 3000;

    public static String readAdditionalProposalInfo(@Nullable DBRProgressMonitor monitor, SQLCompletionContext context, DBPNamedObject object, final String[] keywords, final DBPKeywordType keywordType) {
        if (object != null) {
            if (monitor == null) {
                String[] desc = new String[1];
                RuntimeUtils.runTask(monitor1 ->
                    desc[0] = DBInfoUtils.makeObjectDescription(
                        monitor1,
                        object, true),
                    "Extract object properties info",
                    ADDITIONAL_INFO_WAIT_TIMEOUT);
                return desc[0];
            } else {
                return DBInfoUtils.makeObjectDescription(monitor, object, true);
            }
        } else if (keywordType != null && context.getDataSource() != null && context.isShowServerHelp()) {
            HelpReader helpReader = new HelpReader(context.getDataSource(), keywordType, keywords);
            if (monitor == null) {
                RuntimeUtils.runTask(helpReader, "Read help topic", ADDITIONAL_INFO_WAIT_TIMEOUT);
            } else {
                helpReader.run(monitor);
            }

            return helpReader.info;
        } else {
            return keywords.length == 0 ? null : keywords[0];
        }
    }

    private static String readDataSourceHelp(DBRProgressMonitor monitor, DBPDataSource dataSource, DBPKeywordType keywordType, String keyword) {
        final SQLHelpProvider helpProvider = DBUtils.getAdapter(SQLHelpProvider.class, dataSource);
        if (helpProvider == null) {
            return null;
        }
        final SQLHelpTopic helpTopic = helpProvider.findHelpTopic(monitor, keyword, keywordType);
        if (helpTopic == null) {
            return null;
        }
        if (!CommonUtils.isEmpty(helpTopic.getContents())) {
            return helpTopic.getContents();
        } else if (!CommonUtils.isEmpty(helpTopic.getUrl())) {
            return "<a href=\"" + helpTopic.getUrl() + "\">" + keyword + "</a>";
        } else {
            return null;
        }
    }


    private static class HelpReader implements DBRRunnableWithProgress {
        private final DBPDataSource dataSource;
        private final DBPKeywordType keywordType;
        private final String[] keywords;
        private String info;

        public HelpReader(DBPDataSource dataSource, DBPKeywordType keywordType, String[] keywords) {
            this.dataSource = dataSource;
            this.keywordType = keywordType;
            this.keywords = keywords;
        }

        @Override
        public void run(DBRProgressMonitor monitor) {
            for (String keyword : keywords) {
                info = readDataSourceHelp(monitor, dataSource, keywordType, keyword);
                if (info != null) {
                    break;
                }
            }
            if (CommonUtils.isEmpty(info)) {
                info = "<b>" + keywords[0] + "</b> (" + keywordType.name() + ")";
            }
        }
    }

}
