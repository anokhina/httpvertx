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

public class InviteAuthProvider implements AuthProvider {
    
    private final FileAuthProvider fileAuthProvider;

    public InviteAuthProvider(FileAuthProvider fileAuthProvider) {
        this.fileAuthProvider = fileAuthProvider;
    }
    
    public static final String INV_SYSTEM = "inv";
    
    @Override
    public void authenticate(JsonObject authInfo, Handler<AsyncResult<User>> resultHandler) {
        String username0 = authInfo.getString("username0");
        if (username0 == null) {
          resultHandler.handle(Future.failedFuture("authInfo must contain username in 'username0' field"));
          return;
        }
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
        String password2 = authInfo.getString("password2");
        if (password2 == null) {
          resultHandler.handle(Future.failedFuture("authInfo must contain password in 'password2' field"));
          return;
        }
        if (!password.equals(password2)) {
            resultHandler.handle(Future.failedFuture("password mismatch"));
            return;
        }
        JsonObject jobj0 = getUserMatcher().getUserInfo(FileAuthProvider.AUTH_SYSTEM + ":" + username);
        if (jobj0 != null) {
            resultHandler.handle(Future.failedFuture("user exists"));
            return;
        }
        JsonObject jobj = getUserMatcher().getUserInfo(INV_SYSTEM + ":" + username0);
        if (jobj != null) {
            PlainUser puser = new PlainUser(username0);
            puser.setAuthProvider(fileAuthProvider);
            
            ExtraUser user = new ExtraUser(INV_SYSTEM, puser);
            ExtraUser.upgradeUserInfo(getUserMatcher(), user);
            if (getUserMatcher().updateUser(user, FileAuthProvider.AUTH_SYSTEM + ":" + username, getAuth().getHashString(password, username))) {
                user.setAuthSystem(FileAuthProvider.AUTH_SYSTEM);
            
                resultHandler.handle(Future.succeededFuture(user));
                return;
            }
        }
        resultHandler.handle(Future.failedFuture("Invalid username/password"));
    }
    public UserMatcher getUserMatcher() {
        return fileAuthProvider.getUserMatcher();
    }

    public PassAuth getAuth() {
        return fileAuthProvider.getAuth();
    }
    
}
