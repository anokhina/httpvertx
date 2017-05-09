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

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.FormLoginHandler;
import static io.vertx.ext.web.handler.FormLoginHandler.DEFAULT_PASSWORD_PARAM;
import static io.vertx.ext.web.handler.FormLoginHandler.DEFAULT_RETURN_URL_PARAM;
import static io.vertx.ext.web.handler.FormLoginHandler.DEFAULT_USERNAME_PARAM;

public class MultiFormLoginHandlerImpl implements FormLoginHandler {

    private static final Logger log = LoggerFactory.getLogger(MultiFormLoginHandlerImpl.class);

    private final AuthProvider authProvider;

    private String usernameParam;
    private String passwordParam;
    private String returnURLParam;
    private String directLoggedInOKURL;

    @Override
    public FormLoginHandler setUsernameParam(String usernameParam) {
        this.usernameParam = usernameParam;
        return this;
    }

    @Override
    public FormLoginHandler setPasswordParam(String passwordParam) {
        this.passwordParam = passwordParam;
        return this;
    }

    @Override
    public FormLoginHandler setReturnURLParam(String returnURLParam) {
        this.returnURLParam = returnURLParam;
        return this;
    }

    @Override
    public FormLoginHandler setDirectLoggedInOKURL(String directLoggedInOKURL) {
        this.directLoggedInOKURL = directLoggedInOKURL;
        return this;
    }
    
    public static FormLoginHandler create(AuthProvider authProvider) {
      return new MultiFormLoginHandlerImpl(authProvider, DEFAULT_USERNAME_PARAM, DEFAULT_PASSWORD_PARAM,
        DEFAULT_RETURN_URL_PARAM, null);
    }

    public MultiFormLoginHandlerImpl(AuthProvider authProvider, String usernameParam, String passwordParam,
            String returnURLParam, String directLoggedInOKURL) {
        this.authProvider = authProvider;
        this.usernameParam = usernameParam;
        this.passwordParam = passwordParam;
        this.returnURLParam = returnURLParam;
        this.directLoggedInOKURL = directLoggedInOKURL;
    }

    protected JsonObject makeAuthInfo(RoutingContext context) {
        HttpServerRequest req = context.request();
        MultiMap params = req.formAttributes();
        String username0 = params.get(usernameParam+"0");
        String username = params.get(usernameParam);
        String password = params.get(passwordParam);
        String password2 = params.get(passwordParam+"2");
        JsonObject authInfo = new JsonObject()
                .put("username", username)
                .put("password", password)
                .put("username0", username0)
                .put("password2", password2)
                ;
        return authInfo;
    }

    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest req = context.request();
        if (req.method() != HttpMethod.POST) {
            context.fail(405); // Must be a POST
        } else {
            if (!req.isExpectMultipart()) {
                throw new IllegalStateException("Form body not parsed - do you forget to include a BodyHandler?");
            }
            Session session = context.session();
            JsonObject authInfo = makeAuthInfo(context);
            authProvider.authenticate(authInfo, res -> {
                if (res.succeeded()) {
                    User user = res.result();
                    context.setUser(user);
                    if (session != null) {
                        // the user has upgraded from unauthenticated to authenticated
                        // session should be upgraded as recommended by owasp
                        session.regenerateId();

                        String returnURL = session.remove(returnURLParam);
                        if (returnURL != null) {
                            // Now redirect back to the original url
                            doRedirect(req.response(), returnURL);
                            return;
                        }
                    }
                    // Either no session or no return url
                    if (directLoggedInOKURL != null) {
                        // Redirect to the default logged in OK page - this would occur
                        // if the user logged in directly at this URL without being redirected here first from another
                        // url
                        doRedirect(req.response(), directLoggedInOKURL);
                    } else {
                        // Just show a basic page
                        req.response().end(DEFAULT_DIRECT_LOGGED_IN_OK_PAGE);
                    }
                } else {
                    context.fail(403);  // Failed login
                }
            });

        }
    }

    private void doRedirect(HttpServerResponse response, String url) {
        response.putHeader("location", url).setStatusCode(302).end();
    }

    private static final String DEFAULT_DIRECT_LOGGED_IN_OK_PAGE = ""
            + "<html><body><h1>Login successful</h1></body></html>";

}
