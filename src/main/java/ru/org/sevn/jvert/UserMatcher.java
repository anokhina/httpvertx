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
import io.vertx.ext.auth.User;
import java.util.Collection;

public interface UserMatcher {

    Collection<String> getGroups(User u);

    JsonObject getUserInfo(User u);

    JsonObject getUserInfo(String uid);
    
    default boolean updateUser(User u, String id, String token) {
        JsonObject jobj = new JsonObject();
        if (id != null) {
            jobj.put("id", id);
        }
        if (token != null) {
            jobj.put("token", token);
        }
        return updateUser(u, jobj);
    }
    
    boolean updateUser(User u, JsonObject jobj);
    
    void refreshCheck();
}
