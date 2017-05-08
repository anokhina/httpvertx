/*
 * Copyright 2017 Veronica Anokhina.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.org.sevn.jvert;

import io.vertx.core.json.JsonObject;
import java.util.HashSet;
import java.util.Set;
import io.vertx.ext.auth.User;

public class UserInfo {
    private User token;
    private JsonObject extraData;
    private JsonObject localExtraData;
    private Set<String> groups = new HashSet<>();

    public UserInfo(User t) {
        setToken(t);
    }
    
    public User getToken() {
        return token;
    }

    public void setToken(User token) {
        this.token = token;
    }

    public JsonObject getExtraData() {
        return extraData;
    }

    public void setExtraData(JsonObject extraData) {
        this.extraData = extraData;
    }

    public JsonObject getLocalExtraData() {
        return localExtraData;
    }

    public void setLocalExtraData(JsonObject localExtraData) {
        this.localExtraData = localExtraData;
    }

    public Set<String> getGroups() {
        return groups;
    }
    
}
