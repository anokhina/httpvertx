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
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import ru.org.sevn.jsecure.PassAuth;
import ru.org.sevn.jvert.HttpVerticle.UserMatcher;

public class FileAuthProvider implements AuthProvider {
    
    private final UserMatcher userMatcher;
    private final PassAuth auth;

    public FileAuthProvider(UserMatcher um, PassAuth auth) {
        this.userMatcher = um;
        this.auth = auth;
    }
    
    @Override
    public void authenticate(JsonObject authInfo, Handler<AsyncResult<User>> resultHandler) {
        String username = authInfo.getString("username");
        if (username == null) {
          resultHandler.handle(Future.failedFuture("authInfo must contain username in 'username' field"));
          return;
        }
        String password = authInfo.getString("password");
        if (password == null) {
          resultHandler.handle(Future.failedFuture("authInfo must contain password in 'password' field"));
          return;
        }
        JsonObject jobj = userMatcher.getUserInfo(username);
        if (jobj != null && auth.authenticate(password, username, jobj.getString("token"))) {
            HttpVerticle.ExtraUser user = new HttpVerticle.ExtraUser(new PlainUser(username));
            HttpVerticle.ExtraUser.upgradeUserInfo(userMatcher, user);
            user.setAuthProvider(this);
            resultHandler.handle(Future.succeededFuture(user));
            return;
        }
        resultHandler.handle(Future.failedFuture("Invalid username/password"));
    }
    
}
