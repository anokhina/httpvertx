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

import ru.org.sevn.jvert.wwwgen.RssVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.providers.GoogleAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.FormLoginHandler;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import io.vertx.ext.web.handler.RedirectAuthHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.UserSessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import ru.org.sevn.common.data.SimpleSqliteObjectStore;
import ru.org.sevn.common.solr.SolrIndexer;
import ru.org.sevn.jsecure.PassAuth;
import ru.org.sevn.jvert.auth.ChangePasswordHandler;
import ru.org.sevn.jvert.auth.InviteHandler;
import ru.org.sevn.jvert.wwwgen.FileUploadHandler;
import ru.org.sevn.jvert.wwwgen.FindHandler;
import ru.org.sevn.jvert.wwwgen.HtmlCacheHandler;
import ru.org.sevn.jvert.wwwgen.LogHandler;
import ru.org.sevn.jvert.wwwgen.QRGenHandler;
import ru.org.sevn.jvert.wwwgen.ShareHandler;
import ru.org.sevn.jvert.wwwgen.ShareUrlHandler;
import ru.org.sevn.templ.TemplateEngine;
import ru.org.sevn.jvert.wwwgen.ThumbHandler;
import ru.org.sevn.jvert.wwwgen.ZipHandler;

public class HttpVerticle extends AbstractVerticle {
    private static final int KB = 1024;
    private static final int MB = 1024 * KB;
    private boolean useSsl = true;
    private String hostName = "localhost";
    private String authSys;
    private int port = 1945;
    private JsonObject config;
    private String sslCertPathCer;// = "cert-04.cer";
    private String sslKeyPathPem;// = "key-04.pem";
    private String appName = "SV-www";
    private JsonArray webs;
    private SimpleUserMatcher userMatcher;
    private String saltPrefix = "salt";
    private String schema;
    private String pubDir;
    private long scanPeriod = 5;

    public boolean isUseSsl() {
        return useSsl;
    }

    public String getHostName() {
        return hostName;
    }
    
    public int getPort() {
        return port;
    }
    
    public String getAppName() {
        return appName;
    }
    
    protected Path getConfigJsonPath() {
        return new java.io.File(System.getProperty("user.home") + "/sevn-http-vert.json").toPath();
    }
    
    protected String getSslCertPathCer() {
        return sslCertPathCer;
    }
    protected String getSslKeyPathPem() {
        return sslKeyPathPem;
    }
    
    protected void configService() {
        try {
            config = new JsonObject(new String(Files.readAllBytes(getConfigJsonPath()), "UTF-8"));
            if (config.containsKey("auth")) {
                try {
                    JsonObject auth = config.getJsonObject("auth", null);
                    if (auth != null) {
                        saltPrefix = auth.getString("salt", "salt");
                        String invitePath = auth.getString("invitePath", null);
                        userMatcher = new SimpleUserMatcher(invitePath);
                    }
                } catch (Exception e) {
                    Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, e);
                }
            }
            if (config.containsKey("schema")) {
                try {
                    schema = config.getString("schema");
                } catch (Exception e) {
                    Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, e);
                }
            }
            if (config.containsKey("pubDir")) {
                try {
                    pubDir = config.getString("pubDir");
                } catch (Exception e) {
                    Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, e);
                }
            }
            if (config.containsKey("scanPeriod")) {
                try {
                    scanPeriod = config.getLong("scanPeriod");
                } catch (Exception e) {
                    Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, e);
                }
            }
            if (config.containsKey("webs")) {
                try {
                    webs = config.getJsonArray("webs", null);
                } catch (Exception e) {
                    Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, e);
                }
            }
            if (config.containsKey("authsys")) {
                try {
                    authSys = config.getString("authsys");
                } catch (Exception e) {
                    Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, e);
                }
            }
            if (config.containsKey("hostName")) {
                try {
                    hostName = config.getString("hostName");
                } catch (Exception e) {
                    Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, e);
                }
            }
            if (config.containsKey("port")) {
                try {
                    port = config.getInteger("port");
                } catch (Exception e) {
                    Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, e);
                }
            }
            if (config.containsKey("useSsl")) {
                try {
                    useSsl = config.getBoolean("useSsl");
                } catch (Exception e) {
                    Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, e);
                }
            }
            if (config.containsKey("sslCertPathCer")) {
                try {
                    sslCertPathCer = config.getString("sslCertPathCer");
                } catch (Exception e) {
                    Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, e);
                }
            }
            if (config.containsKey("sslKeyPathPem")) {
                try {
                    sslKeyPathPem = config.getString("sslKeyPathPem");
                } catch (Exception e) {
                    Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, e);
                }
            }
            if (config.containsKey("appName")) {
                try {
                    appName = config.getString("appName");
                } catch (Exception e) {
                    Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, e);
                }
            }
        } catch (java.nio.file.NoSuchFileException e1) {
            Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, e1.getClass().getName() + ": " + e1.getMessage());
        } catch (IOException ex) {
            Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    protected String getConfigAuthJsonFilename() {
        if (isUseSsl()) {
            return "googletrue.json";
        } else {
            return "google.json";
        }
    }
    
    protected Path getConfigAuthJsonPath() {
        return new java.io.File(System.getProperty("user.home") + "/" + getConfigAuthJsonFilename()).toPath();
    }
    
    private AuthProvider newAuthProviderGoogle() {
        try {
            String fileContent = new String(Files.readAllBytes(getConfigAuthJsonPath()), "UTF-8");
            JsonObject serviceAccountJson = new JsonObject(fileContent);
            JsonObject web = serviceAccountJson.getJsonObject("web");
            OAuth2Auth authProvider = GoogleAuth.create(vertx, web.getString("client_id"), web.getString("client_secret"));
            return authProvider;
        } catch (IOException ex) {
            Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    protected String getAuthUrl() {
        String hostname = getHostName();
        String host = "http://"+hostname+":";
        if (isUseSsl()) {
            host = "https://"+hostname+":";
        }
        return host + getPort();
    }
    
    protected void setUpRouter(Router router) {
        router.route().handler(CookieHandler.create());
        router.route().handler(BodyHandler.create().setBodyLimit(50 * MB));
        SessionHandler sessionHandler = SessionHandler.create(LocalSessionStore.create(vertx));
        sessionHandler.setCookieHttpOnlyFlag(true);
        if (isUseSsl()) { //TODO
            sessionHandler.setCookieSecureFlag(true);
        }
        router.route().handler(sessionHandler);
    }
    
    
    private void redirect(HttpServerRequest r, String location) {
        r.response().setChunked(false);
        r.response().setStatusCode(302);
        r.response().putHeader("Location", location);
        r.response().end();
    }
    
    @Override
    public void start() throws Exception {
        configService();
        
        GroupUserAuthorizer authorizerAdmin = new GroupUserAuthorizer(new JsonArray().add("admin"));
        
        Router router = Router.router(vertx);
        setUpRouter(router);
        
        OAuth2Auth authProvider = (OAuth2Auth) newAuthProviderGoogle();
        router.route().handler(UserSessionHandler.create(authProvider));
        
        router.route("/*").handler(new LogHandler());
        
        OAuth2AuthHandler oauth2 = new GoogleUserOAuth2AuthHandlerImpl(authProvider, getAuthUrl(), getAppName(), userMatcher);
        oauth2.addAuthority("profile");
        oauth2.setupCallback(router.get("/auth"));

        FileAuthProvider fileAuthProvider = new FileAuthProvider(userMatcher, new PassAuth(saltPrefix));
        io.vertx.ext.web.handler.AuthHandler authHandlerLogin = RedirectAuthHandler.create(fileAuthProvider,"/www/loginpage.html");
        router.route("/www/login").handler(authHandlerLogin);
        router.route("/www/login").handler(new WebpathHandler()); //TODO use the same
        router.route("/www/loginauth").handler(FormLoginHandler.create(fileAuthProvider));
        router.post("/www/loginauth").failureHandler(ctx -> {
            int statusCode = ctx.statusCode();
            if (statusCode == 400 || statusCode == 403) {
                //TODO wrong user name password message
                ctx.reroute("/www/login");
            } else {
                ctx.next();
            }
        });
        router.route("/qrcode/*").handler(new QRGenHandler());

        {
            InviteAuthProvider inviteAuthProvider = new InviteAuthProvider(fileAuthProvider);
            router.route("/www/invite/*").handler(new InviteHandler("/www/invite/"));
            router.route("/www/invite/*").handler(new WebpathHandler());
            router.route("/www/inviteauth").handler(MultiFormLoginHandlerImpl.create(inviteAuthProvider));
        }
        {
            router.route("/www/profile/auth").handler(new ChangePasswordHandler(fileAuthProvider));
            router.route("/www/profile/*").handler(new WebpathHandler());
        }
        
        router.route("/logout").handler(context -> {
            context.clearUser();
            context.response().putHeader("location", "/").setStatusCode(302).end();
        });
        
        
        {
            OAuth2AuthHandler oauth2all = new GoogleUserOAuth2AuthHandlerImpl(authProvider, getAuthUrl(), getAppName(), userMatcher).setOnlyUpgraded(false);
            oauth2all.addAuthority("profile");
            oauth2all.setupCallback(router.get("/auth"));
        
            String servPath = "/www/logingoogle/";
            router.route(servPath+"*").handler(new RedirectUnAuthParamPageHandler(servPath));
            router.route(servPath+"*").handler(oauth2all);
            router.route(servPath+"invite/*").handler(new GoogleInviteHandler(servPath, appName, userMatcher));
            router.route(servPath+"*").handler(new WebpathHandler());
        }
        
        {
            final SimpleSqliteObjectStore ostoreChatDb = 
                    new SimpleSqliteObjectStore("chatDb.db", 
                            new ChatHandler.MessageMapper());

            String servPath = "/chat/";
            router.route(servPath+"*").handler(new RedirectUnAuthParamPageHandler(servPath));
            router.route(servPath+"*").handler(("google".equals(authSys)) ? oauth2 : authHandlerLogin);
            router.route(servPath+"*").handler(new ChatHandler(ostoreChatDb));
        }
        {
            final UrlHandler.UrlSimpleSqliteObjectStore ostoreChatDb = 
                    new UrlHandler.UrlSimpleSqliteObjectStore("ulinkDb.db", 
                            new UrlHandler.UrlTagMapper(),
                            new UrlHandler.UrlLinkMapper(),
                            new UrlHandler.UrlLinkTagMapper()
                    );

            String servPath = "/ulnk/";
            router.route(servPath+"*").handler(new RedirectUnAuthParamPageHandler(servPath));
            router.route(servPath+"*").handler(("google".equals(authSys)) ? oauth2 : authHandlerLogin);
            router.route(servPath+"*").handler(new UrlHandler(ostoreChatDb));
        }
        //TODO refresh settings
        
        final SolrIndexer indexer = new SolrIndexer("www");
        final LinkedHashMap<String, WebDescriptor> websList = new LinkedHashMap<>();
        final SimpleSqliteObjectStore ostoreHashDb = new SimpleSqliteObjectStore("hashDb.db", new ShareHandler.HashMapper());
        if (webs != null) {
            for (Object o : webs) {
                if (o instanceof JsonObject) {
                    final JsonObject jobj = (JsonObject)o;
                    if (jobj.containsKey("webpath") && jobj.containsKey("dirpath") && jobj.containsKey("groups")) {
                        //TODO create separate object
                        try {
                            String authsys = jobj.getString("authsys");
                            String webpath = jobj.getString("webpath");
                            String dirpath = jobj.getString("dirpath");
                            String dirpathGen = jobj.getString("dirpathGen", dirpath);
                            String dirpathThumb = jobj.getString("dirpathThumb");
                            String dirpathThumbBig = jobj.getString("dirpathThumbBig");
                            String dirpathRss = jobj.getString("dirpathRss");
                            String dirpathHtmlCache = jobj.getString("dirpathHtmlCache");
                            JsonArray groups = jobj.getJsonArray("groups");
                            long scanPeriod = jobj.getLong("scanPeriod", this.scanPeriod);
                            GroupUserAuthorizer authorizer = new GroupUserAuthorizer(groups);
                            GroupUserAuthorizer authorizerSu = new GroupUserAuthorizer(
                                    groups.stream().map( (s) -> s.toString() + "_su").collect(Collectors.toList()));
                            String wpath = "/"+webpath;
                            String wpathDelim = "/"+webpath+"/";

                            ru.org.sevn.jvert.wwwgen.WWWGenHandler genHandler = null;
                            final RssVerticle rssVerticle;
                            if (jobj.containsKey("htmlgen")) {
                                genHandler = new ru.org.sevn.jvert.wwwgen.WWWGenHandler(jobj.getJsonObject("htmlgen"), new File(dirpath), new File(dirpathGen), webpath);
                                genHandler.init();
                                genHandler.setIndexer(indexer);
                                rssVerticle = new RssVerticle(new File(dirpathRss), genHandler, scanPeriod);
                                vertx.deployVerticle(rssVerticle);
                                if (schema == null) {
                                    final RssVerticle rv = rssVerticle;
                                    router.route(wpath+"/*").handler(ctx -> {
                                        if (rv.getSchema() == null) {
                                            rv.setSchema(getSchemaUri(ctx.request()));
                                        }
                                        ctx.next();
                                    });
                                } else {
                                    rssVerticle.setSchema(schema);
                                }
                            
                                router.route(wpath+"/rss").handler(new UserAuthorizedHandler(authorizer, rssVerticle.getRssHandler()));
                                router.route(wpath+"/rss/index.html").handler(new UserAuthorizedHandler(authorizer, rssVerticle.getRssHtmlHandler()));
                            } else {
                                rssVerticle = null;
                            }
                            
                            router.route(wpath+"/ref/*").handler(new ShareUrlHandler(wpathDelim, dirpath, dirpathGen, ostoreHashDb));
                            router.route(wpath+"/*").handler(new RedirectUnAuthParamPageHandler(wpath + "/"));
                            router.route(wpath+"/*").handler(("google".equals(authsys)) ? oauth2 : authHandlerLogin);
                            router.route(wpath).handler(ctx -> {
                                redirect(ctx.request(), wpath + "/index.html");
                            });
                            router.route(wpath+"/dynamic/*").handler(
                                    new UserAuthorizedHandler(authorizer, ctx -> {
                                        ctx.response().putHeader("content-type", "text/html").end("Dynamic page for " + webpath);
                                    })
                            );
                            if (rssVerticle != null) {
                                router.route(wpath+"/admin/forceUpdate").handler(
                                        new UserAuthorizedHandler(authorizerSu, ctx -> {
                                            rssVerticle.forceUpdate();
                                            ctx.response().putHeader("content-type", "text/html").end("Force update for " + webpath);
                                        })
                                );
                            }
                            router.route(wpath + "/search").handler(new UserAuthorizedHandler(authorizer, new FindHandler(indexer, webpath)));
                            router.route(wpath+"/logout/*").handler(ctx -> {
                                ctx.clearUser();
                                ctx.response().putHeader("location", "/").setStatusCode(302).end();
                            });
                            router.route(wpath+"/*").handler( new UserAuthorizedHandler(authorizer, new ZipHandler(wpathDelim, dirpath)));
                            router.route(wpath+"/*").handler( new UserAuthorizedHandler(authorizerSu, new FileUploadHandler(wpathDelim, dirpath, rssVerticle)));
                            router.route(wpath+"/*").handler( new UserAuthorizedHandler(authorizer, new ShareHandler(wpathDelim, dirpath, dirpathGen, ostoreHashDb)));
                            router.route(wpath+"/*").handler(new UserAuthorizedHandler(authorizer, 
                                    new ThumbHandler(wpath, wpathDelim, dirpath, dirpathThumb, dirpathThumbBig))
                            );
                            router.route(wpath+"/thumb/*").handler(
                                    new UserAuthorizedHandler(authorizer, 
                                        new NReachableFSStaticHandlerImpl().setAlwaysAsyncFS(true).setCachingEnabled(false).setDefaultContentEncoding("UTF-8").setAllowRootFileSystemAccess(true).setWebRoot(new File(dirpathThumb).getAbsolutePath())
                                    )
                            );
                            router.route(wpath+"/thumbg/*").handler(
                                    new UserAuthorizedHandler(authorizer, 
                                        new NReachableFSStaticHandlerImpl().setAlwaysAsyncFS(true).setCachingEnabled(false).setDefaultContentEncoding("UTF-8").setAllowRootFileSystemAccess(true).setWebRoot(new File(dirpathThumbBig).getAbsolutePath())
                                    )
                            );
                            if (dirpathHtmlCache == null) {
                                Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, "Can't cache html");                                
                            } else {
                                router.route(wpath+"/*").handler(
                                        new UserAuthorizedHandler(authorizer, new HtmlCacheHandler(dirpathHtmlCache, wpathDelim, dirpath))
                                );
                            }
                            
                            if (jobj.containsKey("htmlgen") && jobj.getJsonObject("htmlgen").getBoolean("on", false)) {
                                router.route(wpath+"/*").handler(
                                        new UserAuthorizedHandler(authorizer, 
                                            genHandler
                                        )
                                );
                            }
                            router.route(wpath+"/*").handler(
                                    new UserAuthorizedHandler(authorizer, 
                                        new NReachableFSStaticHandlerImpl().setAlwaysAsyncFS(true).setCachingEnabled(false).setDefaultContentEncoding("UTF-8").setAllowRootFileSystemAccess(true).setWebRoot(new File(dirpath).getAbsolutePath())
                                    )
                            );
                                    
                            Handler<RoutingContext> generatedHandler = new UserAuthorizedHandler(authorizer, 
                                        new NReachableFSStaticHandlerImpl().setAlwaysAsyncFS(true).setCachingEnabled(false).setDefaultContentEncoding("UTF-8").setAllowRootFileSystemAccess(true).setWebRoot(new File(dirpathGen).getAbsolutePath())
                                    );
                            router.get(wpath+"/*").failureHandler(ctx -> {
                                int statusCode = ctx.statusCode();
                                if (statusCode == 404 && ctx.failure() == null && ctx.get("__generatedHandler__") == null) {
                                    ctx.put("__generatedHandler__", true);
                                    generatedHandler.handle(ctx);
                                    return;
                                }
                                if (ctx.get("__generatedHandler__") != null && ctx.response().ended()) {
                                    return;
                                }
                                ctx.next();
                            });
                            
                            websList.put(webpath, new WebDescriptor(jobj, authorizer));
                        } catch (Exception ex) {
                            Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        }
        //TODO add dynamic pages
        
        final TemplateEngine templateEngine = new TemplateEngine(null);
        router.get("/").handler(ctx -> {
            StringBuilder configuredPaths = new StringBuilder();            
            for (String wp : websList.keySet()) {
                WebDescriptor dsc = websList.get(wp);
                if (dsc.getUserAuthorizer().isAllowed(ctx.user(), ctx)) {
                    configuredPaths.append("<a href=\"/").append(wp).append("/index.html\">").append(wp).append("</a>").append("<br>");
                }
            }
            
            User user = ctx.user();
            Template templ = null;
            try {
                templ = templateEngine.getVelocityEngine().getTemplate("servIndex.html");

                VelocityContext veloCtx = new VelocityContext();
                ctx.response().putHeader("content-type", "text/html").setChunked(true);
                VertxOutputStream vos = new VertxOutputStream(ctx.response());

                if (user instanceof ExtraUser) {
                    ExtraUser euser = (ExtraUser)user;
                    veloCtx.put("userId", euser.getId());
                    veloCtx.put("userIdDsc", euser.fullInfo(false).encodePrettily());
                } else if (user != null) {
                    veloCtx.put("userId", user.principal().encodePrettily());
                    veloCtx.put("userIdDsc", user.principal().encodePrettily());
                }
                veloCtx.put("configuredPaths", configuredPaths.toString());
                OutputStreamWriter writer = new OutputStreamWriter(vos, "UTF-8");
                templ.merge(veloCtx, writer);
                writer.flush();
                vos.close();
                return;
            } catch (Exception ex) {
                Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, ex);
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Server is running").append("<br>");
            String shemaPath = getSchemaUri(ctx.request());
            {
                String wp = shemaPath + "/www/logingoogle";
                String wpStr = "Google";
                sb.append("<a href=\"").append(wp).append("\">").append(wpStr).append("</a>").append("<br>");
            }
            {
                String wp = shemaPath + "/www/login";
                String wpStr = "Local";
                sb.append("<a href=\"").append(wp).append("\">").append(wpStr).append("</a>").append("<br>");
            }
            {
                String wp = shemaPath + "/logout";
                String wpStr = "Logout";
                sb.append("<a href=\"").append(wp).append("\">").append(wpStr).append("</a>").append("<br>");
            }
            sb.append(configuredPaths.toString());
            ctx.response().putHeader("content-type", "text/html").end(sb.toString()+loggedUserString(ctx, false));
        });
        
        router.get("/user").handler(ctx -> {
            ctx.response().putHeader("content-type", "application/json").end(loggedUserString(ctx, false));
        });
        
        router.get("/admin").handler(ctx -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Configured paths:").append("<br>");
            for (String wp : websList.keySet()) {
                sb.append("<a href=\"/").append(wp).append("/index.html\">").append(wp).append("</a>").append("<br>");
            }
            ctx.response().putHeader("content-type", "text/html").end(sb.toString()+simpleAnswerString(ctx));
        });
        // logs 
        //TODO zip unzip
        router.route("/admin/log/*").handler(
                new UserAuthorizedHandler(authorizerAdmin, 
                    new NReachableFSStaticHandlerImpl()
                            .setAlwaysAsyncFS(true)
                            .setCachingEnabled(false)
                            .setDefaultContentEncoding("UTF-8")
                            .setAllowRootFileSystemAccess(true)
                            .setWebRoot(new File("log").getAbsolutePath()) //TODO log
                )
        );
        
        //webroot
        router.route("/www/*").handler(StaticHandler.create().setCachingEnabled(false).setDefaultContentEncoding("UTF-8"));

        File pubDirFile = (pubDir == null) ? (new File(new File(System.getProperty("user.home")), "pub")) : (new File(pubDir));
        router.route("/4all/*").handler(new FreeStaticHandlerImpl().setWebRoot(pubDirFile.getAbsolutePath()));
        
        router.route("/*").handler(ctx -> {
            if (!ctx.response().ended()) { ctx.fail(404); }
        });
        final LogHandler alertLoger = new LogHandler("alert");
        io.vertx.core.Handler<RoutingContext> failureHandler = ctx -> {
            //TODO error template
            try {
                alertLoger.handle(ctx);
            } catch (Exception e) {}
            int statusCode = ctx.statusCode();
            if (ctx.failure() != null) ctx.failure().printStackTrace();
            HttpServerResponse response = ctx.response();
            response.putHeader("content-type", "text/plain");
            response.setChunked(false);
            response.setStatusCode(statusCode).end("Error:" + statusCode);
        };
        router.get("/*").failureHandler(failureHandler);
        router.post("/*").failureHandler(failureHandler);
        
        startServer(router);
    }
    
    //TODO
    static class WebDescriptor {
        private JsonObject web;
        private UserAuthorizer userAuthorizer;
        public WebDescriptor(JsonObject web, UserAuthorizer userAuthorizer) {
            this.web = web;
            this.userAuthorizer = userAuthorizer;
        }

        public JsonObject getWeb() {
            return web;
        }

        public void setWeb(JsonObject web) {
            this.web = web;
        }

        public UserAuthorizer getUserAuthorizer() {
            return userAuthorizer;
        }

        public void setUserAuthorizer(UserAuthorizer userAuthorizer) {
            this.userAuthorizer = userAuthorizer;
        }
        
    }
    
    protected void startServer(Router router) {
        HttpServerOptions options = new HttpServerOptions();
        if (isUseSsl()) {
            setSslCerts(options);
        }
        vertx.createHttpServer(options).requestHandler(router::accept)
                .listen(getPort());
        System.out.println("HTTP VERTICLE started");
    }
    
    protected void setSslCerts(HttpServerOptions options) {
        if (getSslKeyPathPem() != null && getSslCertPathCer() != null) {
            PemKeyCertOptions pemOptions = new PemKeyCertOptions();
            pemOptions.setKeyPath(getSslKeyPathPem()).setCertPath(getSslCertPathCer());
            options.setSsl(true);
            options.setPemKeyCertOptions(pemOptions);
        } else {
            SelfSignedCertificate certificate = SelfSignedCertificate.create(); 
            options.setKeyCertOptions(certificate.keyCertOptions());
            options.setTrustOptions(certificate.trustOptions());
            options.setSsl(true);
        }
    }
    public static void secureSet(HttpServerResponse response) {
        response
                // do not allow proxies to cache the data
                .putHeader("Cache-Control", "no-store, no-cache")
                // prevents Internet Explorer from MIME - sniffing a
                // response away from the declared content-type
                .putHeader("X-Content-Type-Options", "nosniff")
                // Strict HTTPS (for about ~6Months)
                .putHeader("Strict-Transport-Security", "max-age=" + 15768000)
                // IE8+ do not allow opening of attachments in the context of this resource
                .putHeader("X-Download-Options", "noopen")
                // enable XSS for IE
                .putHeader("X-XSS-Protection", "1; mode=block")
                // deny frames
                .putHeader("X-FRAME-OPTIONS", "DENY");
    }
    
        

//--------------------------
    private static String loggedUserString(RoutingContext context, boolean full) {
        User user = context.user();
        if (user instanceof ExtraUser) {
            return ((ExtraUser)user).fullInfo(full).encodePrettily();
        } else if (user != null) {
            return user.principal().encodePrettily(); //"some authenticated user";
        }
        return "";
    }

    private static String simpleAnswerString(RoutingContext context) {
        HttpServerRequest r = context.request();

        secureSet(context.response());

        Session session = context.session();

        Integer cnt = session.get("hitcount");
        cnt = (cnt == null ? 0 : cnt) + 1;

        session.put("hitcount", cnt);

        String resp = "<pre>"
                + r.absoluteURI() + "\n"
                + getSchemaPath(r) + "\n"
                + r.uri() + "\n"
                + r.path() + "\n"
                + r.query() + "\n"
                + loggedUserString(context, !true) + "\n"
                + cnt + "\n"
                + "</pre>";
        System.err.println("*********>" + resp);
        return resp;
    }
    
    public static String getSchemaUri(HttpServerRequest r) {
        return r.absoluteURI().substring(0, r.absoluteURI().length() - r.uri().length());
    }
    public static String getSchemaPath(HttpServerRequest r) {
        return getSchemaUri(r) + r.path();
    }
    
  public void stop() throws Exception {
        System.out.println("HTTP VERTICLE stopped");
  }
}
