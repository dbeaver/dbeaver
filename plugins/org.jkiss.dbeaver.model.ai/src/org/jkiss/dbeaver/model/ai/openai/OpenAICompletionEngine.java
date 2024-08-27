/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.ai.openai;

import com.google.gson.Gson;
import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import okhttp3.ResponseBody;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.AIEngineSettings;
import org.jkiss.dbeaver.model.ai.AISettingsRegistry;
import org.jkiss.dbeaver.model.ai.completion.AbstractAICompletionEngine;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionContext;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionMessage;
import org.jkiss.dbeaver.model.ai.format.IAIFormatter;
import org.jkiss.dbeaver.model.ai.metadata.MetadataProcessor;
import org.jkiss.dbeaver.model.ai.openai.service.AdaptedOpenAiService;
import org.jkiss.dbeaver.model.ai.openai.service.GPTCompletionAdapter;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import retrofit2.HttpException;
import retrofit2.Response;

import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class OpenAICompletionEngine extends AbstractAICompletionEngine<GPTCompletionAdapter, Object> {
    private static final Log log = Log.getLog(OpenAICompletionEngine.class);

    //How many retries may be done if code 429 happens
    protected static final int MAX_REQUEST_ATTEMPTS = 3;

    private static final Map<String, GPTCompletionAdapter> clientInstances = new HashMap<>();

    private static final Pattern sizeErrorPattern = Pattern.compile(".+context length is ([0-9]+) tokens.+([0-9]+) tokens.+");

    public OpenAICompletionEngine() {
    }

    private static CompletionRequest buildSingleRequest(
        boolean chatMode,
        @NotNull List<DAICompletionMessage> messages,
        int maxTokens,
        Double temperature,
        String modelId
    ) {
        return CompletionRequest.builder()
            .prompt(buildSingleMessage(truncateMessages(chatMode, messages, maxTokens)))
            .temperature(temperature)
            //.maxTokens(maxTokens)
            .frequencyPenalty(0.0)
            .n(1)
            .presencePenalty(0.0)
            .stop(List.of("#", ";"))
            .model(modelId)
            //.echo(true)
            .build();
    }

    private static ChatCompletionRequest buildChatRequest(
        boolean chatMode,
        @NotNull List<DAICompletionMessage> messages,
        int maxTokens,
        Double temperature,
        String modelId
    ) {
        return ChatCompletionRequest.builder()
            .messages(truncateMessages(chatMode, messages, maxTokens).stream()
                .map(m -> new ChatMessage(m.getRole().getId(), m.getContent()))
                .toList())
            .temperature(temperature)
            //.maxTokens(maxTokens)
            .frequencyPenalty(0.0)
            .presencePenalty(0.0)
            .n(1)
            .model(modelId)
            //.echo(true)
            .build();
    }

    /**
     * Resets GPT client cache
     */
    public static void resetServices() {
        clientInstances.clear();
    }

    @Override
    public String getEngineName() {
        return "OpenAI GPT";
    }

    public String getModelName() {
        return CommonUtils.toString(getSettings().getProperties().get(AIConstants.GPT_MODEL), GPTModel.GPT_TURBO.getName());
    }

    public boolean isValidConfiguration() {
        return !CommonUtils.isEmpty(acquireToken());
    }

    @Override
    public Map<String, GPTCompletionAdapter> getServiceMap() {
        return clientInstances;
    }

    /**
     * Request completion from GPT API uses parameters from {@link AIConstants} for model settings\
     * Adds current schema metadata to starting query
     *
     * @param monitor execution monitor
     * @param context completion context
     * @param messages request messages
     * @return resulting string
     */
    @Nullable
    protected String requestCompletion(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DAICompletionContext context,
        @NotNull List<DAICompletionMessage> messages,
        @NotNull IAIFormatter formatter,
        boolean chatCompletion
    ) throws DBException {
        final DBCExecutionContext executionContext = context.getExecutionContext();
        DBSObjectContainer mainObject = getScopeObject(context, executionContext);

        final DAICompletionMessage metadataMessage = MetadataProcessor.INSTANCE.createMetadataMessage(
            monitor,
            context,
            mainObject,
            formatter,
            getInstructions(chatCompletion),
            getMaxTokens() - AIConstants.MAX_RESPONSE_TOKENS
        );

        final List<DAICompletionMessage> mergedMessages = new ArrayList<>();
        mergedMessages.add(metadataMessage);
        mergedMessages.addAll(messages);

        if (monitor.isCanceled()) {
            return "";
        }

        GPTCompletionAdapter service = getServiceInstance(executionContext);
        Object completionRequest = createCompletionRequest(chatCompletion, mergedMessages);
        String completionText = callCompletion(monitor, chatCompletion, mergedMessages, service, completionRequest);

        return processCompletion(
            mergedMessages,
            monitor,
            executionContext,
            mainObject,
            completionText,
            formatter,
            getModel().isChatAPI()
        );
    }

    protected int getMaxTokens() {
        return GPTModel.getByName(getModelName()).getMaxTokens();
    }

    /**
     * Initializes OpenAiService instance using token provided by {@link AIConstants} GTP_TOKEN_PATH
     */
    protected GPTCompletionAdapter initGPTApiClientInstance() throws DBException {
        String token = acquireToken();
        if (CommonUtils.isEmpty(token)) {
            throw new DBException("Empty API token value");
        }
        return new AdaptedOpenAiService(token, Duration.ofSeconds(30));
    }

    protected String acquireToken() {
        AIEngineSettings config = getSettings();
        Object token = config.getProperties().get(AIConstants.GPT_API_TOKEN);
        if (token != null) {
            return token.toString();
        }
        return DBWorkbench.getPlatform().getPreferenceStore().getString(AIConstants.GPT_API_TOKEN);
    }

    @NotNull
    protected AIEngineSettings getSettings() {
        return AISettingsRegistry.getInstance().getSettings().getEngineConfiguration(AIConstants.OPENAI_ENGINE);
    }

    @Nullable
    protected String callCompletion(
        @NotNull DBRProgressMonitor monitor,
        boolean chatMode,
        @NotNull List<DAICompletionMessage> messages,
        @NotNull GPTCompletionAdapter service,
        @NotNull Object completionRequest
    ) throws DBException {
        monitor.subTask("Request GPT completion");
        try {
            if (CommonUtils.toBoolean(getSettings().getProperties().get(AIConstants.AI_LOG_QUERY))) {
                if (completionRequest instanceof ChatCompletionRequest) {
                    log.debug("Chat GPT request:\n" + ((ChatCompletionRequest) completionRequest).getMessages().stream()
                        .map(message -> "# " + message.getRole() + "\n" + message.getContent())
                        .collect(Collectors.joining("\n")));
                } else {
                    log.debug("GPT request:\n" + ((CompletionRequest) completionRequest).getPrompt());
                }
            }
            if (monitor.isCanceled()) {
                return null;
            }

            try {
                List<?> choices;
                for (int i = 0; ; i++) {
                    try {
                        choices = getCompletionChoices(service, completionRequest);
                        break;
                    } catch (Exception e) {
                        if ((e instanceof HttpException && ((HttpException) e).code() == 429)
                            || (e instanceof OpenAiHttpException && e.getMessage().contains("This model's maximum"))) {
                            if (e instanceof HttpException) {
                                RuntimeUtils.pause(1000);
                            } else {
                                // Extracts resulted prompt size from the error message and resizes max response to
                                // value lower that (maxTokens - prompt size)
                                Matcher matcher = sizeErrorPattern.matcher(e.getMessage());
                                int promptSize;
                                if (matcher.find()) {
                                    String numberStr = matcher.group(1);
                                    promptSize = CommonUtils.toInt(numberStr);
                                } else {
                                    throw e;
                                }
                                if (promptSize >= getMaxTokens()) {
                                    // Already at maximum
                                    throw e;
                                }
                                completionRequest = createCompletionRequest(chatMode, messages, promptSize);
                            }
                            if (i >= MAX_REQUEST_ATTEMPTS - 1) {
                                throw e;
                            } else {
                                if (e instanceof HttpException) {
                                    log.debug("AI service failed. Retry (" + e.getMessage() + ")");
                                }
                                continue;
                            }
                        }
                        throw e;
                    }
                }
                String completionText;
                Object choice = choices.stream().findFirst().orElseThrow();
                if (choice instanceof CompletionChoice) {
                    completionText = ((CompletionChoice) choice).getText();
                } else {
                    completionText = ((ChatCompletionChoice) choice).getMessage().getContent();
                }
                if (CommonUtils.toBoolean(getSettings().getProperties().get(AIConstants.AI_LOG_QUERY))) {
                    log.debug("GPT response:\n" + completionText);
                }
                return completionText;
            } catch (Exception exception) {
                if (exception instanceof HttpException) {
                    Response<?> response = ((HttpException) exception).response();
                    if (response != null) {
                        try {
                            try (ResponseBody responseBody = response.errorBody()) {
                                if (responseBody != null) {
                                    String bodyString = responseBody.string();
                                    if (!CommonUtils.isEmpty(bodyString)) {
                                        try {
                                            Gson gson = new Gson();
                                            Map<String, Object> map = JSONUtils.parseMap(gson, new StringReader(bodyString));
                                            Map<String, Object> error = JSONUtils.deserializeProperties(map, "error");
                                            if (error != null) {
                                                String message = JSONUtils.getString(error, "message");
                                                if (!CommonUtils.isEmpty(message)) {
                                                    bodyString = message;
                                                }
                                            }
                                        } catch (Exception e) {
                                            // ignore json errors
                                        }
                                        throw new DBException("AI service error: " + bodyString);
                                    }
                                }
                            }
                        } catch (IOException e) {
                            log.debug(e);
                        }
                    }
                } else if (exception instanceof RuntimeException &&
                    !(exception instanceof OpenAiHttpException) &&
                    exception.getCause() != null) {
                    throw new DBException("AI service error: " + exception.getCause().getMessage(), exception.getCause());
                }
                throw exception;
            }
        } finally {
            monitor.done();
        }
    }

    protected GPTCompletionAdapter getServiceInstance(@NotNull DBCExecutionContext executionContext) throws DBException {
        DBPDataSourceContainer container = executionContext.getDataSource().getContainer();
        GPTCompletionAdapter service = clientInstances.get(container.getId());
        if (service == null) {
            service = initGPTApiClientInstance();
            clientInstances.put(container.getId(), service);
        }
        return service;
    }

    protected Object createCompletionRequest(boolean chatMode, @NotNull List<DAICompletionMessage> messages) {
        return createCompletionRequest(chatMode, messages, getMaxTokens());
    }

    @NotNull
    @Override
    protected String getInstructions(boolean chatCompletion) {
        if (GPTModel.GPT_TURBO.equals(getModel())) {
            return """
                You are SQL assistant. You must produce SQL code for given prompt.
                You must produce valid SQL statement enclosed with Markdown code block and terminated with semicolon.
                All comments MUST be placed before query outside markdown code block.
                Be polite.
                """;
        }
        return super.getInstructions(chatCompletion);
    }

    protected Object createCompletionRequest(boolean chatMode, @NotNull List<DAICompletionMessage> messages, int maxTokens) {
        Double temperature =
            CommonUtils.toDouble(getSettings().getProperties().get(AIConstants.AI_TEMPERATURE), 0.0);
        final GPTModel model = getModel();
        if (model.isChatAPI()) {
            return buildChatRequest(chatMode, messages, maxTokens, temperature, model.getName());
        } else {
            return buildSingleRequest(chatMode, messages, maxTokens, temperature, model.getName());
        }
    }

    @NotNull
    private GPTModel getModel() {
        final String modelId = CommonUtils.toString(getSettings().getProperties().get(AIConstants.GPT_MODEL), "");
        return CommonUtils.isEmpty(modelId) ? GPTModel.GPT_TURBO : GPTModel.getByName(modelId);
    }

    private List<?> getCompletionChoices(GPTCompletionAdapter service, Object completionRequest) {
        if (completionRequest instanceof CompletionRequest) {
            return service.createCompletion((CompletionRequest) completionRequest).getChoices();
        } else {
            return service.createChatCompletion((ChatCompletionRequest) completionRequest).getChoices();
        }
    }

    @NotNull
    private static String buildSingleMessage(@NotNull List<DAICompletionMessage> messages) {
        final StringJoiner buffer = new StringJoiner("\n");

        for (DAICompletionMessage message : messages) {
            if (message.getRole() == DAICompletionMessage.Role.SYSTEM) {
                buffer.add("###");
                buffer.add(message.getContent()
                    .lines()
                    .map(line -> '#' + line)
                    .collect(Collectors.joining("\n")));
                buffer.add("###");
            } else {
                buffer.add(message.getContent());
            }
        }

        buffer.add("SELECT ");

        return buffer.toString();
    }

}
