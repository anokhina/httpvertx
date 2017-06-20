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
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileUploadHandler implements io.vertx.core.Handler<RoutingContext> {
    
    private final JsonObject config;
    private final Updater updater;
    
    public FileUploadHandler(String wpathDelim, String dirpath, Updater updater) {
        this(new JsonObject().put("wpathDelim", wpathDelim).put("dirpath", dirpath), updater);
    }
    
    public FileUploadHandler(JsonObject config, Updater updater) {
        this.config = config;
        this.updater = updater;
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
        String dirname = req.getParam("dirname");
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
                                
                if (dirname != null) {
                    File newDir = new File(dir, dirname);
                    if (newDir.exists()) {
                        if (newDir.isDirectory()) {
                            dir = newDir;
                        } else {
                            ctx.fail(403);
                            return;
                            //TODO
                        }
                    } else {
                        try {
                            newDir.mkdirs();
                            Files.write(new File(newDir, ".title.txt").toPath(), dirname.getBytes("UTF-8"));
                            dir = newDir;
                        } catch (Exception e) {
                            Logger.getLogger(FileUploadHandler.class.getName()).log(Level.SEVERE, null, e);
                            ctx.fail(e);
                            return;
                        }
                    }
                }
                final File dirr = dir; 
                    
                if (req.method() == HttpMethod.POST) {
                    Set<FileUpload> uploads = ctx.fileUploads();
                    StringBuilder sbNot = new StringBuilder();
                    StringBuilder sb = new StringBuilder();
                    sb.append("<p>Uploaded:\n");
                    sbNot.append("<p>Not loaded:\n");
                    for(FileUpload fu : uploads) {
                        JsonObject jo = new JsonObject();
                        jo.put("fileName", fu.fileName());
                        jo.put("name", fu.name());
                        jo.put("uploadedFileName", fu.uploadedFileName());
                        System.out.println(jo.encodePrettily());
                        File f = new File(fu.fileName());
                        File outFile = new File(dirr, f.getName());
                        File loadedFile = new File(fu.uploadedFileName());
                        if (outFile.exists()) {
                            sbNot.append(outFile.getName()).append(",\n");
                            try {
                                loadedFile.delete();
                            } catch (Exception ex) {
                                Logger.getLogger(FileUploadHandler.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        } else {
                            try {
                                loadedFile.renameTo(outFile);
                                sb.append(outFile.getName()).append(",\n");
                            } catch (Exception e) {
                                Logger.getLogger(FileUploadHandler.class.getName()).log(Level.SEVERE, null, e);
                                try {
                                    loadedFile.delete();
                                } catch (Exception ex) {
                                    Logger.getLogger(FileUploadHandler.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                    }
                    ctx.response().putHeader("content-type", "text/html").end(sb.toString()+sbNot.toString());
                    if (updater != null) {
                        updater.forceUpdate();
                    }
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
        return "<!DOCTYPE HTML>\n" +
"<html>\n" +
"<head>\n" +
"	<meta charset=\"utf-8\">\n" +
"    <title></title>\n" +
"</head>\n" +
"<script language=\"JavaScript\" type=\"text/javascript\">\n" +
"function handleBrowseClick(fi) {\n" +
"    var fileinput = document.getElementById(fi);\n" +
"    fileinput.click();\n" +
"}\n" +
"function handleChange(fi, ti) {\n" +
"    var fileinput = document.getElementById(fi);\n" +
"    var textinput = document.getElementById(ti);\n" +
"    textinput.value = fileinput.value;\n" +
"}\n" +
"</script>\n" +
"<body>\n" +
"\n" +
"<form action=\""+actionName+"\" ENCTYPE=\"multipart/form-data\" method=\"POST\" name=\"fileform\">\n" +
"    Part name:<input type=\"text\" name=\"dirname\" style=\"width: 100%;\"/>\n" +
"\n" +
"    Choose a file to upload:<input type=\"file\" name=\"uploadedfiles\" id=\"uploadedfiles\" multiple=\"multiple\" onChange=\"handleChange('uploadedfiles','filenames')\" style=\"width: 100%;\"/><br>\n" +
"    <!--input type=\"button\" value=\"Click to select file\" onclick=\"handleBrowseClick('uploadedfiles');\"/-->\n" +
"    <textarea id=\"filenames\" readonly=\"true\" rows=\"14\" style=\"width: 100%;\" wrap=\"soft\"> </textarea>\n" +
"\n" +
"    <input type=\"submit\"/>\n" +
"    <input type=\"hidden\" name=\"edit\" value=\"2\"/>\n" +
"</form>\n" +
"\n" +
"</body>\n" +
"</html>";
    }
}
/*
<!DOCTYPE HTML>
<html>
<head>
	<meta charset="utf-8">
    <title></title>
</head>
<script language="JavaScript" type="text/javascript">
function handleBrowseClick(fi) {
    var fileinput = document.getElementById(fi);
    fileinput.click();
}
function handleChange(fi, ti) {
    var fileinput = document.getElementById(fi);
    var textinput = document.getElementById(ti);
    textinput.value = fileinput.value;
}
</script>
<body>

<form action="actionName" ENCTYPE="multipart/form-data" method="POST" name="fileform">
    Part name:<input type="text" name="dirname" style="width: 100%;"/>

    Choose a file to upload:<input type="file" name="uploadedfiles" id="uploadedfiles" multiple="multiple" onChange="handleChange('uploadedfiles','filenames')" style="width: 100%;"/><br>
    <!--input type="button" value="Click to select file" onclick="handleBrowseClick('uploadedfiles');"/-->
    <textarea id="filenames" readonly="true" rows="14" style="width: 100%;" wrap="soft"> </textarea>

    <input type="submit"/>
    <input type="hidden" name="edit" value="2"/>
</form>

</body>
</html>
*/