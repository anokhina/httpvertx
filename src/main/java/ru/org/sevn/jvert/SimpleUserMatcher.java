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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimpleUserMatcher implements UserMatcher {
    private LinkedHashMap<String, JsonObject> userGroups = new LinkedHashMap<>();

    //config = new JsonObject(new String(Files.readAllBytes(getConfigJsonPath()), "UTF-8"))
    private File file;
    public SimpleUserMatcher(String filePath) throws IOException {
        this.file = new File(filePath);
        JsonArray arr = new JsonArray(new String(Files.readAllBytes(file.toPath()), "UTF-8"));
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
        String id = getId(u);
        if (id != null) {
            return getUserInfo(id);
        }
        return null;
    }

    @Override
    public JsonObject getUserInfo(String uid) {
        if (uid != null) {
            return userGroups.get(uid);
        }
        return null;
    }
    
    private String getId(User u) {
        if (u instanceof ExtraUser) {
            ExtraUser user = (ExtraUser) u;
            return user.getId();
        }
        return null;
    }

    @Override
    public synchronized boolean updateUser(User u, String id, String token) {
        JsonObject jobjEx = getUserInfo(id);
        if (jobjEx == null) {
            String uid = getId(u);
            JsonObject jobj = getUserInfo(uid);
            if (jobj != null) {
                try {
                    if ("person".equals(jobj.getString("invite", "multiple"))) {
                        userGroups.remove(uid);
                    } else {
                        jobj = jobj.copy();
                    }
                    if (token != null) {
                        jobj.put("token", token);
                    }
                    jobj.put("id", id);
                    userGroups.put(id, jobj);
                    
                    JsonArray jarr = new JsonArray(new ArrayList(userGroups.values()));
                    Files.write(file.toPath(), jarr.encodePrettily().getBytes("UTF-8"));
                    ExtraUser.updateUserInfo(jobj, (ExtraUser)u);
                    return true;
                } catch (Exception ex) {
                    Logger.getLogger(SimpleUserMatcher.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return false;
    }
    
}
