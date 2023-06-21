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
package org.jkiss.dbeaver.model.dpi.client;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Simple REST client
 */
public class RestClient {

    private static final Log log = Log.getLog(RestClient.class);

    public static final Gson gson = new Gson();

    private final URL serverURL;

    public RestClient(URL serverURL) {
        this.serverURL = serverURL;
    }

    public String sendRequest(String endpoint, Map<String, Object> body) throws IOException {
        return sendRequest(endpoint, null, body);
    }

    public String sendRequest(String endpoint, String method, Map<String, Object> body) throws IOException {
        URL url = new URL(serverURL, endpoint);
        HttpURLConnection dpiConnection = (HttpURLConnection) url.openConnection();
        try {
            if (method == null) {
                if (CommonUtils.isEmpty(body)) {
                    method = "GET";
                } else {
                    method = "POST";
                }
            }
            dpiConnection.setRequestMethod(method);
            if (!CommonUtils.isEmpty(body)) {
                dpiConnection.setRequestProperty("content-type", "text/json");
                dpiConnection.setDoOutput(true);
            }
            dpiConnection.connect();
            if (!CommonUtils.isEmpty(body)) {
                gson.toJson(body, Map.class,
                    new JsonWriter(
                        new OutputStreamWriter(
                            dpiConnection.getOutputStream(),
                            StandardCharsets.UTF_8
                    )));
            }
            int responseCode = dpiConnection.getResponseCode();
            String responseString = null;
            InputStream inputStream = dpiConnection.getInputStream();
            if (inputStream != null) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                IOUtils.copyStream(inputStream, buffer);
                responseString = buffer.toString(StandardCharsets.UTF_8);
            }

            if (responseCode != 200) {
                if (CommonUtils.isEmpty(responseString)) {
                    responseString = dpiConnection.getResponseMessage();
                }
                throw new DBException("Error calling REST server: " + responseString);
            }

            return responseString;
        }
        catch (Throwable e) {
            throw new IOException("Error pinging DPI server", e);
        }
        finally {
            dpiConnection.disconnect();
        }
    }

}
