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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimpleUserMatcher implements UserMatcher {
    private HashMap<String, JsonObject> userGroups = new HashMap<>();

    public SimpleUserMatcher(JsonArray arr) {
        for (Object o : arr) {
            if (o instanceof JsonObject) {
                JsonObject jobj = (JsonObject) o;
                if (jobj.containsKey("id")) {
                    userGroups.put(jobj.getString("id"), jobj);
                }
            }
        }
    }

    @Override
    public Collection<String> getGroups(User u) {
        if (u instanceof ExtraUser) {
            ExtraUser user = (ExtraUser) u;
            JsonObject jobj = userGroups.get(user.getId());
            if (jobj != null) {
                HashSet<String> grps = new HashSet();
                try {
                    if (jobj.containsKey("groups")) {
                        for (Object g : jobj.getJsonArray("groups")) {
                            grps.add(g.toString());
                        }
                    }
                    return grps;
                } catch (Exception ex) {
                    Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return null;
    }

    @Override
    public JsonObject getUserInfo(User u) {
        if (u instanceof ExtraUser) {
            ExtraUser user = (ExtraUser) u;
            return userGroups.get(user.getId());
        }
        return null;
    }

    @Override
    public JsonObject getUserInfo(String uid) {
        return userGroups.get(uid);
    }
    
}
