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
package ru.org.sevn.jvert.wwwgen;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileUploadHandler implements io.vertx.core.Handler<RoutingContext> {
    
    private final JsonObject config;
    
    public FileUploadHandler(String wpathDelim, String dirpath) {
        this(new JsonObject().put("wpathDelim", wpathDelim).put("dirpath", dirpath));
    }
    
    public FileUploadHandler(JsonObject config) {
        this.config = config;
    }
    
    protected String getWpathDelim() {
        return config.getString("wpathDelim");
    }
    
    protected String getDirpath() {
        return config.getString("dirpath");
    }
    
    @Override
    public void handle(RoutingContext ctx) {
        HttpServerRequest req = ctx.request();
        String edit = req.getParam("edit");
        if (edit != null) {
            String path = req.path();
            path = path.substring(getWpathDelim().length());
            try {
                path = java.net.URLDecoder.decode(path, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(FileUploadHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
            File dir = new File(getDirpath(), path);
            if (!dir.isDirectory()) {
                dir = dir.getParentFile();
            }
            if (dir.exists()) {
                                
                final File dirr = dir; 
                if (req.method() == HttpMethod.POST) {
                    req.setExpectMultipart(true);
                    req.uploadHandler(upload -> {
                        upload.exceptionHandler(cause -> {
                            req.response().setChunked(true).end("Upload failed");
                        });

                        upload.endHandler(v -> {
                            req.response().setChunked(true).end("Successfully uploaded to " + upload.filename());
                        });
                        File outFile = new File(dirr, upload.filename());
                        upload.streamToFileSystem(outFile.getAbsolutePath());
                    });
                } else if (req.method() == HttpMethod.GET) {
                    String actionName = "";
                    ctx.response().putHeader("content-type", "text/html").end(getFormContent(actionName));
                }
            }
        } else {
            ctx.next();
        }
    }    

    public String getFormContent(String actionName) {
        return "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n" +
"        \"http://www.w3.org/TR/html4/loose.dtd\">\n" +
"<html>\n" +
"<head>\n" +
"    <title></title>\n" +
"</head>\n" +
"<body>\n" +
"\n" +
"<form action=\""+actionName+"\" ENCTYPE=\"multipart/form-data\" method=\"POST\" name=\"fileform\">\n" +
"    choose a file to upload:<input type=\"file\" name=\"uploadedfile\"/><br>\n" +
"    <input type=\"submit\"/>\n" +
"    <input type=\"hidden\" name=\"edit\" value=\"2\"/>\n" +
"</form>\n" +
"\n" +
"</body>\n" +
"</html>";
    }
}
/*
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <title></title>
</head>
<body>

<form action="actionName" ENCTYPE="multipart/form-data" method="POST" name="fileform">
    choose a file to upload:<input type="file" name="uploadedfile"/><br>
    <input type="submit"/>
    <input type="hidden" name="edit" value="2"/>
</form>

</body>
</html>
*/