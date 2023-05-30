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
package org.jkiss.dbeaver.model.ai.gpt3;

import com.google.gson.Gson;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import okhttp3.ResponseBody;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.ai.AICompletionConstants;
import org.jkiss.dbeaver.model.ai.AIEngineSettings;
import org.jkiss.dbeaver.model.ai.AISettings;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionEngine;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionRequest;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionResponse;
import org.jkiss.dbeaver.model.ai.completion.DAICompletionScope;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.logical.DBSLogicalDataSource;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.struct.rdb.DBSTablePartition;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import retrofit2.HttpException;
import retrofit2.Response;

import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.util.*;

public class GPTCompletionEngine implements DAICompletionEngine {
    private static final Log log = Log.getLog(GPTCompletionEngine.class);

    //How many retries may be done if code 429 happens
    private static final int MAX_REQUEST_ATTEMPTS = 3;

    private static final Map<String, OpenAiService> clientInstances = new HashMap<>();
    private static final int GPT_MODEL_MAX_TOKENS = 2048;
    private static final int MAX_PROMPT_LENGTH = 7500; // 8000 -
    private static final boolean SUPPORTS_ATTRS = true;

    public GPTCompletionEngine() {
    }

    @Override
    public String getEngineName() {
        return "GPT-3";
    }

    @Override
    public String getModelName() {
        return DBWorkbench.getPlatform().getPreferenceStore().getString(GPTConstants.GPT_MODEL);
    }

    @NotNull
    @Override
    public List<DAICompletionResponse> performQueryCompletion(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBSLogicalDataSource dataSource,
        @NotNull DBCExecutionContext executionContext,
        @NotNull DAICompletionRequest completionRequest,
        boolean returnOnlyCompletion,
        int maxResults
    ) throws DBException {
        String result = requestCompletion(completionRequest, monitor, executionContext);
        DAICompletionResponse response = createCompletionResponse(dataSource, executionContext, result);
        return Collections.singletonList(response);
    }

    public boolean isValidConfiguration() {
        return !CommonUtils.isEmpty(acquireToken());
    }

    @NotNull
    protected DAICompletionResponse createCompletionResponse(DBSLogicalDataSource dataSource, DBCExecutionContext executionContext, String result) {
        DAICompletionResponse response = new DAICompletionResponse();
        response.setResultCompletion(result);
        return response;
    }

    /**
     * Initializes OpenAiService instance using token provided by {@link GPTConstants} GTP_TOKEN_PATH
     */
    private static OpenAiService initGPTApiClientInstance() throws DBException {
        String token = acquireToken();
        if (CommonUtils.isEmpty(token)) {
            throw new DBException("Empty API token value");
        }
        return new OpenAiService(token, Duration.ofSeconds(30));
    }

    private static String acquireToken() {
        AIEngineSettings openAiConfig = AISettings.getSettings().getEngineConfiguration(GPTConstants.OPENAI_ENGINE);
        Object token = openAiConfig.getProperties().get(GPTConstants.GPT_API_TOKEN);
        if (token != null) {
            return token.toString();
        }
        return DBWorkbench.getPlatform().getPreferenceStore().getString(GPTConstants.GPT_API_TOKEN);
    }

    /**
     * Request completion from GPT API uses parameters from {@link GPTConstants} for model settings\
     * Adds current schema metadata to starting query
     *
     * @param request          request text
     * @param monitor          execution monitor
     * @return resulting string
     */
    private String requestCompletion(
        @NotNull DAICompletionRequest request,
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext
    ) throws DBException {
        DAICompletionScope scope = request.getScope();
        DBSObjectContainer mainObject = null;
        DBCExecutionContextDefaults<?,?> contextDefaults = executionContext.getContextDefaults();
        if (contextDefaults != null) {
            switch (scope) {
                case CURRENT_SCHEMA:
                    if (contextDefaults.getDefaultSchema() != null) {
                        mainObject = contextDefaults.getDefaultSchema();
                    } else {
                        mainObject = contextDefaults.getDefaultCatalog();
                    }
                    break;
                case CURRENT_DATABASE:
                    mainObject = contextDefaults.getDefaultCatalog();
                    break;
                default:
                    break;
            }
        }
        if (mainObject == null) {
            mainObject = ((DBSObjectContainer) executionContext.getDataSource());
        }

        DBPDataSourceContainer container = executionContext.getDataSource().getContainer();
        OpenAiService service = clientInstances.get(container.getId());
        if (service == null) {
            service = initGPTApiClientInstance();
            clientInstances.put(container.getId(), service);
        }
        String modifiedRequest = addDBMetadataToRequest(monitor, request, executionContext, mainObject);
        if (monitor.isCanceled()) {
            return "";
        }

        Object completionRequest = createCompletionRequest(modifiedRequest);
        monitor.subTask("Request GPT completion");
        try {
            if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(GPTConstants.GPT_LOG_QUERY)) {
                if (completionRequest instanceof ChatCompletionRequest) {
                    log.debug("Chat GPT request:\n" + ((ChatCompletionRequest) completionRequest).getMessages().get(0).getContent());
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
                        if (e instanceof HttpException && ((HttpException) e).code() == 429) {
                            RuntimeUtils.pause(1000);
                            if (i >= MAX_REQUEST_ATTEMPTS - 1) {
                                throw e;
                            } else {
                                log.debug("AI service failed. Retry (" + e.getMessage() + ")");
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
                if (CommonUtils.isEmpty(completionText)) {
                    return null;
                }
                completionText = "SELECT " + completionText.trim() + ";";

                completionText = postProcessGeneratedQuery(monitor, mainObject, executionContext, completionText);
                if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(AICompletionConstants.AI_INCLUDE_SOURCE_TEXT_IN_QUERY_COMMENT)) {
                    String[] lines = request.getPromptText().split("\n");
                    for (String line : lines) {
                        if (!CommonUtils.isEmpty(line)) {
                            completionText = "-- " + line.trim() + "\n" + completionText;
                        }
                    }
                }

                return completionText.trim();
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
                }
                throw exception;
            }

        } finally {
            monitor.done();
        }
    }

    private List<?> getCompletionChoices(OpenAiService service, Object completionRequest) {
        if (completionRequest instanceof CompletionRequest) {
            return service.createCompletion((CompletionRequest) completionRequest).getChoices();
        } else {
            return service.createChatCompletion((ChatCompletionRequest) completionRequest).getChoices();
        }
    }

    private static DBPPreferenceStore getPreferenceStore() {
        return DBWorkbench.getPlatform().getPreferenceStore();
    }

    private static Object createCompletionRequest(@NotNull String request) throws DBException {
        int maxTokens = GPT_MODEL_MAX_TOKENS;
        Double temperature = getPreferenceStore().getDouble(GPTConstants.GPT_MODEL_TEMPERATURE);
        String modelId = getPreferenceStore().getString(GPTConstants.GPT_MODEL);
        GPTModel model = CommonUtils.isEmpty(modelId) ? null : GPTModel.getByName(modelId);
        if (model == null) {
            model = GPTModel.GPT_TURBO;
        }
        if (model.isChatAPI()) {
            return buildChatRequest(request, maxTokens, temperature, modelId);
        } else {
            return buildLegacyAPIRequest(request, maxTokens, temperature, modelId);
        }
    }

    private static CompletionRequest buildLegacyAPIRequest(
        @NotNull String request,
        int maxTokens,
        Double temperature,
        String modelId
    ) {
        CompletionRequest.CompletionRequestBuilder builder =
            CompletionRequest.builder().prompt(request);
        return builder
            .temperature(temperature)
            .maxTokens(maxTokens)
            .frequencyPenalty(0.0)
            .presencePenalty(0.0)
            .stop(List.of("#", ";"))
            .model(modelId)
            //.echo(true)
            .build();
    }

    private static ChatCompletionRequest buildChatRequest(
        @NotNull String request,
        int maxTokens,
        Double temperature,
        String modelId
    ) {
        ChatMessage message = new ChatMessage("user", request);
        ChatCompletionRequest.ChatCompletionRequestBuilder builder =
            ChatCompletionRequest.builder().messages(Collections.singletonList(message));

        return builder
            .temperature(temperature)
            .maxTokens(maxTokens)
            .frequencyPenalty(0.0)
            .presencePenalty(0.0)
            .stop(List.of("#", ";"))
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

    /**
     * Add completion metadata to request
     */
    protected String addDBMetadataToRequest(
        DBRProgressMonitor monitor,
        DAICompletionRequest request,
        DBCExecutionContext executionContext,
        DBSObjectContainer mainObject
    ) throws DBException {
        if (mainObject == null || mainObject.getDataSource() == null || CommonUtils.isEmptyTrimmed(request.getPromptText())) {
            throw new DBException("Invalid completion request");
        }

        StringBuilder additionalMetadata = new StringBuilder();
        additionalMetadata.append("### ")
            .append(mainObject.getDataSource().getSQLDialect().getDialectName())
            .append(" SQL tables, with their properties:\n#\n");
        String tail = "";
        if (executionContext != null && executionContext.getContextDefaults() != null) {
            DBSSchema defaultSchema = executionContext.getContextDefaults().getDefaultSchema();
            if (defaultSchema != null) {
                tail += "#\n# Current schema is " + defaultSchema.getName() + "\n";
            }
        }
        int maxRequestLength = MAX_PROMPT_LENGTH - additionalMetadata.length() - tail.length() - 20;

        if (request.getScope() != DAICompletionScope.CUSTOM) {
            additionalMetadata.append(generateObjectDescription(
                monitor,
                request,
                mainObject,
                executionContext,
                maxRequestLength,
                false
            ));
        } else {
            for (DBSEntity entity : request.getCustomEntities()) {
                additionalMetadata.append(generateObjectDescription(
                    monitor,
                    request,
                    entity,
                    executionContext,
                    maxRequestLength,
                    isRequiresFullyQualifiedName(entity, executionContext)
                ));
            }
        }

        String promptText = request.getPromptText().trim();
        promptText = postProcessPrompt(monitor, mainObject, executionContext, promptText);
        additionalMetadata.append(tail).append("#\n###").append(promptText).append("\nSELECT");
        return additionalMetadata.toString();
    }

    private boolean isRequiresFullyQualifiedName(@NotNull DBSObject object, @Nullable DBCExecutionContext context) {
        if (context == null || context.getContextDefaults() == null) {
            return false;
        }
        DBSObject parent = object.getParentObject();
        DBCExecutionContextDefaults contextDefaults = context.getContextDefaults();
        return parent != null && !(parent.equals(contextDefaults.getDefaultCatalog())
            || parent.equals(contextDefaults.getDefaultSchema()));
    }

    private String generateObjectDescription(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DAICompletionRequest request,
        @NotNull DBSObject object,
        @Nullable DBCExecutionContext context,
        int maxRequestLength,
        boolean useFullyQualifiedName
    ) throws DBException {
        if (DBNUtils.getNodeByObject(monitor, object, false) == null) {
            // Skip hidden objects
            return "";
        }
        StringBuilder description = new StringBuilder();
        if (object instanceof DBSEntity) {
            String name = useFullyQualifiedName && context != null ? DBUtils.getObjectFullName(
                context.getDataSource(),
                object,
                DBPEvaluationContext.DDL
            ) : DBUtils.getQuotedIdentifier(object);
            description.append("# ").append(name);
            description.append("(");
            boolean firstAttr = addPromptAttributes(monitor, (DBSEntity) object, description, true);
            addPromptExtra(monitor, (DBSEntity) object, description, firstAttr);

            description.append(");\n");
        } else if (object instanceof DBSObjectContainer) {
            monitor.subTask("Load cache of " + object.getName());
            ((DBSObjectContainer) object).cacheStructure(
                monitor,
                DBSObjectContainer.STRUCT_ENTITIES | DBSObjectContainer.STRUCT_ATTRIBUTES);
            int totalChildren = 0;
            for (DBSObject child : ((DBSObjectContainer) object).getChildren(monitor)) {
                if (DBUtils.isSystemObject(child) || DBUtils.isHiddenObject(child) || child instanceof DBSTablePartition) {
                    continue;
                }
                String childText = generateObjectDescription(
                    monitor,
                    request,
                    child,
                    context,
                    maxRequestLength,
                    isRequiresFullyQualifiedName(child, context)
                );
                if (description.length() + childText.length() > maxRequestLength) {
                    log.debug("Trim GPT metadata prompt  at table '" + child.getName() + "' - too long request");
                    break;
                }
                description.append(childText);
                totalChildren++;
            }
        }
        return description.toString();
    }

    protected boolean addPromptAttributes(
        DBRProgressMonitor monitor,
        DBSEntity entity,
        StringBuilder prompt,
        boolean firstAttr
    ) throws DBException {
        if (SUPPORTS_ATTRS) {
            List<? extends DBSEntityAttribute> attributes = entity.getAttributes(monitor);
            if (attributes != null) {
                for (DBSEntityAttribute attribute : attributes) {
                    if (DBUtils.isHiddenObject(attribute)) {
                        continue;
                    }
                    if (!firstAttr) prompt.append(",");
                    firstAttr = false;
                    prompt.append(attribute.getName());
                }
            }
        }
        return firstAttr;
    }

    protected void addPromptExtra(
        DBRProgressMonitor monitor,
        DBSEntity object,
        StringBuilder description,
        boolean firstAttr
    ) throws DBException {

    }

    protected String postProcessPrompt(
        DBRProgressMonitor monitor,
        DBSObjectContainer mainObject,
        DBCExecutionContext executionContext,
        String promptText
    ) {

        return promptText;
    }

    protected String postProcessGeneratedQuery(
        DBRProgressMonitor monitor,
        DBSObjectContainer mainObject,
        DBCExecutionContext executionContext,
        String completionText
    ) {

        return completionText;
    }

}
