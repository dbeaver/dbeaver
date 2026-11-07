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
package org.jkiss.dbeaver.model.ai.engine.openai.dto;

import com.google.gson.annotations.SerializedName;
import org.jkiss.dbeaver.DBException;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

public class OAIResponsesResponse extends OAIResponsesBase {
    public List<OAIMessage> output;
    public String status;
    public OAIError error;
    @SerializedName("incomplete_details")
    public Map<String, String> incompleteDetails;

    public void validateCompletion() throws DBException {
        // some compatible endpoints omit status; reject explicit unsuccessful states without breaking those endpoints
        if (status != null && !"completed".equals(status)) {
            String detail = status;
            if (error != null && !CommonUtils.isEmpty(error.message)) {
                detail += ": " + error.message;
            } else if (incompleteDetails != null && !CommonUtils.isEmpty(incompleteDetails.get("reason"))) {
                detail += ": " + incompleteDetails.get("reason");
            }
            throw new DBException("Response did not complete: " + detail);
        }
    }
}
