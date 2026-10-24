/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.completion.usage;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.LocalCacheProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLModelPreferences;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Usage models of connections (see {@link SQLCompletionUsageModel}), kept in the workspace metadata
 * (never in the project folder, which may be shared). Learns from queries executed in the SQL editor
 * and from accepted proposals.
 */
public final class SQLCompletionUsageManager {
    private static final Log log = Log.getLog(SQLCompletionUsageManager.class);

    private static final String USAGE_FOLDER = "completion-usage";
    private static final long HALF_LIFE_MS = 7L * 24 * 60 * 60 * 1000;
    private static final long SAVE_DELAY_MS = 5000;
    // A query written and then executed (or executed again soon) is learned once
    private static final long SAME_QUERY_WINDOW_MS = 10 * 60 * 1000;
    // Executed queries are learned after the result viewer has read the metadata of their tables
    private static final long LEARN_DELAY_MS = 2000;
    /**
     * Number of recently used tables proposed in table completions.
     */
    public static final int RECENT_TABLES = 5;

    private static final SQLCompletionUsageManager INSTANCE = new SQLCompletionUsageManager();

    private final Map<String, Entry> models = new HashMap<>();
    // Paths of used tables being read in background
    private final Set<List<String>> readingTables = new HashSet<>();

    private static final class Entry {
        final SQLCompletionUsageModel model = new SQLCompletionUsageModel(HALF_LIFE_MS);
        // Null for connections outside a project: the model is kept in memory only
        @Nullable
        final Path file;
        // Normalized text of queries learned recently -> time
        final Map<String, Long> recentQueries = new LinkedHashMap<>();
        SaveJob saveJob;

        Entry(@Nullable Path file) {
            this.file = file;
        }
    }

    private SQLCompletionUsageManager() {
    }

    @NotNull
    public static SQLCompletionUsageManager getInstance() {
        return INSTANCE;
    }

    /**
     * Whether proposals of the connection are ranked by usage.
     */
    public static boolean isEnabled(@Nullable DBPDataSourceContainer container) {
        return container != null && container.getPreferenceStore() != null &&
            container.getPreferenceStore().getBoolean(SQLModelPreferences.SQL_EDITOR_PROPOSAL_USAGE_RANKING);
    }

    /**
     * Learns from an executed query (also a failed one: names which do not resolve are skipped), in background.
     */
    public void learnExecutedQuery(@NotNull DBCExecutionContext executionContext, @NotNull String queryText) {
        DBPDataSource dataSource = executionContext.getDataSource();
        if (!isEnabled(dataSource.getContainer()) || CommonUtils.isEmptyTrimmed(queryText)) {
            return;
        }
        new LearnJob(executionContext, queryText, System.currentTimeMillis()).schedule(LEARN_DELAY_MS);
    }

    /**
     * Learns from a query written in the editor (when the user leaves it), recognized by the editor already.
     */
    public void learnWrittenQuery(@NotNull DBPDataSourceContainer container, @NotNull SQLQueryModel model, @NotNull String queryText) {
        if (!isEnabled(container) || CommonUtils.isEmptyTrimmed(queryText)) {
            return;
        }
        learn(container, SQLUsageExtractor.extract(model, queryText, queryText.length()), queryText, System.currentTimeMillis());
    }

    private void learn(@NotNull DBPDataSourceContainer container, @NotNull SQLUsageQuery usage, @NotNull String queryText, long time) {
        if (usage.getObjects().isEmpty()) {
            return;
        }
        String normalized = queryText.trim().replaceAll("\\s+", " ");
        Entry entry = getEntry(container);
        synchronized (entry) {
            Long learned = entry.recentQueries.get(normalized);
            if (learned != null && time - learned < SAME_QUERY_WINDOW_MS) {
                return;
            }
            entry.recentQueries.put(normalized, time);
            entry.recentQueries.values().removeIf(t -> time - t >= SAME_QUERY_WINDOW_MS);
            entry.model.learnQuery(usage, time);
        }
        scheduleSave(entry);
    }

    /**
     * Paths (container names and table name) of the tables used most on the connection, most used first.
     */
    @NotNull
    public List<List<String>> getTopTables(@NotNull DBPDataSourceContainer container, int limit) {
        if (!isEnabled(container)) {
            return List.of();
        }
        Entry entry = getEntry(container);
        synchronized (entry) {
            return entry.model.getTopTables(limit, System.currentTimeMillis());
        }
    }

    /**
     * Usage scores of proposals (by object key), or null if usage ranking is disabled.
     */
    @Nullable
    public Map<String, Double> score(
        @NotNull DBPDataSourceContainer container,
        @NotNull SQLUsageContext context,
        @NotNull List<String> keys
    ) {
        if (!isEnabled(container)) {
            return null;
        }
        Entry entry = getEntry(container);
        long time = System.currentTimeMillis();
        Map<String, Double> scores = new HashMap<>();
        synchronized (entry) {
            for (int i = 0; i < keys.size(); i++) {
                double score = entry.model.score(context, keys.get(i), i, keys.size(), time);
                if (score > 0) {
                    scores.put(keys.get(i), score);
                }
            }
        }
        return scores;
    }

    /**
     * Learns that the user chose a proposal among the shown ones.
     */
    public void learnChoice(
        @NotNull DBPDataSourceContainer container,
        @NotNull SQLUsageContext context,
        @NotNull List<String> shown,
        @NotNull String chosen
    ) {
        if (!isEnabled(container)) {
            return;
        }
        Entry entry = getEntry(container);
        long time = System.currentTimeMillis();
        List<String> used = new ArrayList<>();
        used.add(chosen);
        String table = SQLUsageQuery.tableOf(chosen);
        if (table != null && chosen.equals(table)) {
            // A chosen table also uses its schema
            int pos = table.indexOf('.');
            if (pos > 0) {
                used.add(SQLUsageQuery.schemaKey(table.substring(2, pos)));
            }
        }
        synchronized (entry) {
            entry.model.learnChoice(context, shown, chosen, time);
            entry.model.learnUse(used, time);
        }
        scheduleSave(entry);
    }

    /**
     * Reads the recently used tables of a connected data source in background, so the first completion
     * after connecting already proposes them.
     */
    public void preloadTopTables(@NotNull DBPDataSourceContainer container) {
        if (!isEnabled(container) || !(container.getDataSource() instanceof DBSObjectContainer root)) {
            return;
        }
        for (List<String> path : getTopTables(container, RECENT_TABLES)) {
            scheduleRead(root, path);
        }
    }

    private void scheduleRead(@NotNull DBSObjectContainer root, @NotNull List<String> path) {
        synchronized (readingTables) {
            if (readingTables.add(path)) {
                new ReadTableJob(root, path).schedule();
            }
        }
    }

    /**
     * Finds an object by its path (see {@link #getTopTables}) in cached metadata. Objects which are not cached
     * are read in background, so they are found next time.
     */
    @Nullable
    public DBSObject findCachedObject(@NotNull DBRProgressMonitor monitor, @NotNull DBSObjectContainer root, @NotNull List<String> path) {
        try {
            DBSObject object = findObject(new LocalCacheProgressMonitor(monitor), root, path);
            if (object == null) {
                scheduleRead(root, path);
            }
            return object;
        } catch (DBException e) {
            log.debug("Error finding used table " + path + ": " + e.getMessage());
            return null;
        }
    }

    @Nullable
    private static DBSObject findObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObjectContainer root,
        @NotNull List<String> path
    ) throws DBException {
        DBSObject object = root;
        for (String name : path) {
            if (!(object instanceof DBSObjectContainer container)) {
                return null;
            }
            object = container.getChild(monitor, name);
            if (object == null) {
                return null;
            }
        }
        return object;
    }

    @NotNull
    private Entry getEntry(@NotNull DBPDataSourceContainer container) {
        synchronized (models) {
            String id = String.valueOf(container.getId());
            Entry entry = models.get(id);
            if (entry == null) {
                entry = new Entry(getUsageFile(container));
                read(entry);
                models.put(id, entry);
            }
            return entry;
        }
    }

    @Nullable
    private static Path getUsageFile(@NotNull DBPDataSourceContainer container) {
        if (container.getProject() == null || container.getId() == null) {
            return null;
        }
        return DBWorkbench.getPlatform().getWorkspace().getMetadataFolder()
            .resolve(USAGE_FOLDER)
            .resolve(CommonUtils.escapeFileName(container.getProject().getName()))
            .resolve(CommonUtils.escapeFileName(container.getId()) + ".json.gz");
    }

    private static void read(@NotNull Entry entry) {
        if (entry.file == null || !Files.exists(entry.file)) {
            return;
        }
        try (InputStreamReader reader = new InputStreamReader(
            new GZIPInputStream(new BufferedInputStream(Files.newInputStream(entry.file))), StandardCharsets.UTF_8)) {
            entry.model.read(reader);
        } catch (Exception e) {
            log.warn("Error reading completion usage " + entry.file + ", ignoring it", e);
        }
    }

    private static void write(@NotNull Entry entry) {
        if (entry.file == null) {
            return;
        }
        Path tmpFile = entry.file.resolveSibling(entry.file.getFileName() + ".tmp");
        try {
            Files.createDirectories(entry.file.getParent());
            try (OutputStreamWriter writer = new OutputStreamWriter(
                new GZIPOutputStream(new BufferedOutputStream(Files.newOutputStream(tmpFile))), StandardCharsets.UTF_8)) {
                synchronized (entry) {
                    entry.model.write(writer, System.currentTimeMillis());
                }
            }
            try {
                Files.move(tmpFile, entry.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmpFile, entry.file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.warn("Error saving completion usage " + entry.file, e);
        }
    }

    private static void scheduleSave(@NotNull Entry entry) {
        synchronized (entry) {
            if (entry.saveJob == null) {
                entry.saveJob = new SaveJob(entry);
            }
            // Rescheduling postpones the save: a burst of changes is written once
            entry.saveJob.schedule(SAVE_DELAY_MS);
        }
    }

    /**
     * Recognizes an executed query and learns its usage.
     */
    private final class LearnJob extends AbstractJob {
        private final DBCExecutionContext executionContext;
        private final String queryText;
        private final long time;

        LearnJob(@NotNull DBCExecutionContext executionContext, @NotNull String queryText, long time) {
            super("Learn query usage");
            this.executionContext = executionContext;
            this.queryText = queryText;
            this.time = time;
            setSystem(true);
            setUser(false);
        }

        @NotNull
        @Override
        protected IStatus run(@NotNull DBRProgressMonitor monitor) {
            try {
                DBPDataSource dataSource = executionContext.getDataSource();
                SQLDialect dialect = dataSource.getSQLDialect();
                SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
                syntaxManager.init(dialect, dataSource.getContainer().getPreferenceStore());
                // Tables of the query are read by name if they are not cached yet (once per session)
                SQLQueryRecognitionContext recognitionContext = new SQLQueryRecognitionContext(
                    monitor, executionContext, true, false, syntaxManager, dialect);
                SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(recognitionContext, queryText);
                if (model != null) {
                    learn(dataSource.getContainer(), SQLUsageExtractor.extract(model, queryText, queryText.length()), queryText, time);
                }
            } catch (Exception e) {
                log.debug("Error learning query usage: " + e.getMessage());
            }
            return Status.OK_STATUS;
        }
    }

    /**
     * Reads a used table which is not cached yet, so it is proposed next time.
     */
    private final class ReadTableJob extends AbstractJob {
        private final DBSObjectContainer root;
        private final List<String> path;

        ReadTableJob(@NotNull DBSObjectContainer root, @NotNull List<String> path) {
            super("Read used table");
            this.root = root;
            this.path = path;
            setSystem(true);
            setUser(false);
        }

        @NotNull
        @Override
        protected IStatus run(@NotNull DBRProgressMonitor monitor) {
            try {
                findObject(monitor, root, path);
            } catch (Exception e) {
                log.debug("Error reading used table " + path + ": " + e.getMessage());
            } finally {
                synchronized (readingTables) {
                    readingTables.remove(path);
                }
            }
            return Status.OK_STATUS;
        }
    }

    private static final class SaveJob extends AbstractJob {
        private final Entry entry;

        SaveJob(@NotNull Entry entry) {
            super("Save completion usage");
            this.entry = entry;
            setSystem(true);
            setUser(false);
        }

        @NotNull
        @Override
        protected IStatus run(@NotNull DBRProgressMonitor monitor) {
            write(entry);
            return Status.OK_STATUS;
        }
    }
}
