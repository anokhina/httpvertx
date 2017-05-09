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

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ExtraUser implements User {

    private final User user;
    private JsonObject extraData;
    private JsonObject localExtraData;
    private Set<String> groups = new HashSet<>();
    private String authSystem;

    public static void fillFrom(ExtraUser to, ExtraUser from) {
        if (from.extraData != null) { to.extraData = from.extraData.copy(); }
        if (from.localExtraData != null) { to.localExtraData = from.localExtraData.copy(); }
        if (from.groups != null) { to.groups.addAll(from.groups); }
    }
    public static void updateUserInfo(JsonObject jobj, ExtraUser euser) {
        if (jobj != null) {
            jobj = jobj.copy();
            jobj.remove("token");
            jobj.remove("pcomment");
        }
        euser.setLocalExtraData(jobj);
    }
    public static ExtraUser upgradeUserInfo(UserMatcher userMatcher, ExtraUser euser) {
        Collection<String> groups = userMatcher.getGroups(euser);
        if (groups != null) {
            updateUserInfo(userMatcher.getUserInfo(euser), euser);
            euser.getGroups().addAll(groups);
            return euser;
        }
        return null;
    }

    public ExtraUser(String authSystem, User u) {
        this.authSystem = authSystem;
        user = u;
    }

    @Override
    public User isAuthorised(String string, Handler<AsyncResult<Boolean>> hndlr) {
        user.isAuthorised(string, hndlr);
        return this;
    }

    @Override
    public User clearCache() {
        user.clearCache();
        return this;
    }

    @Override
    public JsonObject principal() {
        return user.principal();
    }

    @Override
    public void setAuthProvider(AuthProvider ap) {
        user.setAuthProvider(ap);
    }
    
    public JsonObject fullInfo(boolean full) {
        JsonObject ret = new JsonObject();
        putNotNul(ret, "id", getId());
        putNotNul(ret, "authSystem", authSystem);
        putNotNul(ret, "extraData", extraData);
        putNotNul(ret, "localExtraData", localExtraData);
        if (full) {
            putNotNul(ret, "principal", user.principal());
        }
        // user.principal() returns not public info
        return ret;
    }
    
    private void putNotNul(JsonObject ret, String k, Object v) {
        if (v != null) {
            ret.put(k, v);
        }
    }

    public String getId() {
        String ret = null;
        if (extraData != null) {
            ret = authSystem + ":" + extraData.getString("id", null);
        }
        if (ret == null) {
            if (localExtraData != null) {
                ret = localExtraData.getString("id", null);
            }
        }
        if (ret == null) {
            if (user.principal() != null) {
                ret = authSystem + ":" + user.principal().getString("id", null);
            }
        }
        return ret;
    }

    public JsonObject getExtraData() {
        return extraData;
    }

    public void setExtraData(JsonObject extraData) {
        this.extraData = extraData;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }

    public JsonObject getLocalExtraData() {
        return localExtraData;
    }

    public void setLocalExtraData(JsonObject localExtraData) {
        this.localExtraData = localExtraData;
    }

    protected void setAuthSystem(String name) {
        this.authSystem = name;
    }
}
