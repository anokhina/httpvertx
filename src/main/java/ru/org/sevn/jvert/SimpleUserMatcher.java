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
    private long fileTime;
    public SimpleUserMatcher(String filePath) throws IOException {
        this.file = new File(filePath);
        this.fileTime = file.lastModified();
        refresh();
    }
    
    public synchronized void refreshCheck() {
        if (fileTime < file.lastModified()) {
            refresh();
        }
    }
    public synchronized void refresh() {
        LinkedHashMap<String, JsonObject> ug;
        try {
            ug = makeuserGroups();
            this.fileTime = file.lastModified();
            userGroups = ug;
        } catch (IOException ex) {
            Logger.getLogger(SimpleUserMatcher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private LinkedHashMap<String, JsonObject> makeuserGroups() throws IOException {
        LinkedHashMap<String, JsonObject> userGroups = new LinkedHashMap<>();
        JsonArray arr = new JsonArray(new String(Files.readAllBytes(file.toPath()), "UTF-8"));
        for (Object o : arr) {
            if (o instanceof JsonObject) {
                JsonObject jobj = (JsonObject) o;
                if (jobj.containsKey("id")) {
                    userGroups.put(jobj.getString("id"), jobj);
                }
            }
        }
        return userGroups;
    }

    @Override
    public Collection<String> getGroups(User u) {
        if (u instanceof ExtraUser) {
            refreshCheck();
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
        refreshCheck();
        String id = getId(u);
        if (id != null) {
            return getUserInfo(id);
        }
        return null;
    }

    @Override
    public JsonObject getUserInfo(String uid) {
        if (uid != null) {
            refreshCheck();
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
    public synchronized boolean updateUser(User u, JsonObject jobj2set) {
        refreshCheck();
        String id = jobj2set.getString("id");
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
                    {
                        final JsonObject ljobj = jobj;
                        jobj2set.forEach(el -> {
                            ljobj.put(el.getKey(), el.getValue());
                        });
                    }
                    updateUserData(jobj, id, u);
                    return true;
                } catch (Exception ex) {
                    Logger.getLogger(SimpleUserMatcher.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else {
            try {
                String uid = getId(u);
                JsonObject jobj = getUserInfo(uid);
                if (jobj != null) {
                    jobj.put("token", jobj2set.getString("token"));

                    updateUserData(jobj, id, u);
                    return true;
                }
            } catch (Exception ex) {
                Logger.getLogger(SimpleUserMatcher.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return false;
    }
    
    private void updateUserData(JsonObject jobj, String id, User u) throws IOException {
        userGroups.put(id, jobj);
        updateUserFile();

        ((ExtraUser)u).setLocalExtraData(jobj);
        ExtraUser.upgradeUserInfo(this, (ExtraUser)u);
        
    }
    private void updateUserFile() throws IOException {
        JsonArray jarr = new JsonArray(new ArrayList(userGroups.values()));
        Files.write(file.toPath(), jarr.encodePrettily().getBytes("UTF-8"));
        fileTime = file.lastModified();
    }
    
}
