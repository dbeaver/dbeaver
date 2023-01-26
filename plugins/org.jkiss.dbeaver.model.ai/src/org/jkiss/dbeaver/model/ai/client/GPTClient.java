/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.ai.client;

import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import okhttp3.ResponseBody;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.GPTPreferences;
import org.jkiss.dbeaver.model.ai.formatter.GPTRequestFormatter;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;
import retrofit2.HttpException;
import retrofit2.Response;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GPTClient {
    private static final Log log = Log.getLog(GPTClient.class);

    //How many retries may be done if code 429 happens
    private static final int MAX_REQUEST_ATTEMPTS = 2;

    private static final Map<String, OpenAiService> clientInstances = new HashMap<>();

    public static boolean isValidConfiguration() {
        return !CommonUtils.isEmpty(acquireToken());
    }

    /**
     * Initializes OpenAiService instance using token provided by {@link GPTPreferences} GTP_TOKEN_PATH
     */
    private static OpenAiService initGPTApiClientInstance() throws DBException {
        String token = acquireToken();
        if (CommonUtils.isEmpty(token)) {
            throw new DBException("Empty API token value");
        }
        return new OpenAiService(token, Duration.ofSeconds(30));
    }

    private static String acquireToken() {
        return DBWorkbench.getPlatform().getPreferenceStore().getString(GPTPreferences.GPT_API_TOKEN);
    }

    /**
     * Request completion from GPT API uses parameters from {@link GPTPreferences} for model settings\
     * Adds current schema metadata to starting query
     *
     * @param request          request text
     * @param monitor          execution monitor
     * @param context          context object
     * @return resulting string
     */
    public static String requestCompletion(
        @NotNull String request,
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBSObjectContainer context,
        @NotNull DBCExecutionContext executionContext) throws DBException {
        if (context == null || context.getDataSource() == null) {
            throw new DBException("No datasource container");
        }
        DBPDataSourceContainer container = context.getDataSource().getContainer();
        OpenAiService service = clientInstances.get(container.getId());
        if (service == null) {
            service = initGPTApiClientInstance();
            clientInstances.put(container.getId(), service);
        }
        String modifiedRequest = GPTRequestFormatter.addDBMetadataToRequest(monitor, request, executionContext, context);
        if (monitor.isCanceled()) {
            return "";
        }
        CompletionRequest completionRequest = createCompletionRequest(modifiedRequest);
        monitor.subTask("Request GPT completion");
        try {
            if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(GPTPreferences.GPT_LOG_QUERY)) {
                log.debug("GPT request:\n" + completionRequest.getPrompt());
            }
            if (monitor.isCanceled()) {
                return null;
            }
            return tryCreateCompletion(service, completionRequest, 0);
        } finally {
            monitor.done();
        }
    }

    private static String tryCreateCompletion(@NotNull OpenAiService service, @NotNull CompletionRequest completionRequest, int attempt) throws DBException {
        if (attempt == MAX_REQUEST_ATTEMPTS) {
            log.error("Maximum GPT completion attempts reached");
            return null;
        }
        try {
            List<CompletionChoice> choices = service.createCompletion(completionRequest).getChoices();
            Optional<CompletionChoice> choice = choices.stream().findFirst();
            String completionText = choice.orElseThrow().getText();
            if (CommonUtils.isEmpty(completionText)) {
                return null;
            }
            completionText = "SELECT " + completionText.trim() + ";";

            return completionText.trim();
        } catch (Exception exception) {
            if (exception instanceof HttpException && ((HttpException) exception).code() == 429) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    return null;
                }
                return tryCreateCompletion(service, completionRequest, attempt + 1);
            } else {
                if (exception instanceof HttpException) {
                    Response<?> response = ((HttpException) exception).response();
                    if (response != null) {
                        try {
                            try (ResponseBody responseBody = response.errorBody()) {
                                if (responseBody != null) {
                                    String bodyString = responseBody.string();
                                    if (!CommonUtils.isEmpty(bodyString)) {
                                        throw new DBException("GTP completion error:\n" + bodyString);
                                    }
                                }
                            }
                        } catch (IOException e) {
                            log.debug(e);
                        }
                    }
                }
                throw exception;
            }
        }
    }

    private static DBPPreferenceStore getPreferenceStore() {
        return DBWorkbench.getPlatform().getPreferenceStore();
    }

    private static CompletionRequest createCompletionRequest(@NotNull String request) {
        int maxTokens = getPreferenceStore().getInt(GPTPreferences.GPT_MODEL_MAX_TOKENS);
        Double temperature = getPreferenceStore().getDouble(GPTPreferences.GPT_MODEL_TEMPERATURE);
        String model = getPreferenceStore().getString(GPTPreferences.GPT_MODEL);
        return CompletionRequest.builder()
            .prompt(request)
            .temperature(temperature)
            .maxTokens(maxTokens)
            .frequencyPenalty(0.0)
            .presencePenalty(0.0)
            .stop(List.of("#", ";"))
            .model(model)
            //.echo(true)
            .build();
    }

    private GPTClient() {

    }

    /**
     * Resets GPT client cache
     */
    public static void resetServices() {
        clientInstances.clear();
    }
}
