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
package ru.org.sevn.common.solr;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

public class SolrIndexer {
    /*
https://plus.google.com/collection/cP5YV
    
unzip solr.zip
cd solr
bin\solr start -p 8983
bin\solr create -c www

check it with 
http://localhost:8983/solr/admin/cores?action=STATUS

solr-6.6.0\server\solr\www\conf\managed-schema

from solr home
bin\solr stop -p 8983﻿    
    
    solr-6.6.0\server\solr\www\conf\managed-schema 
    */
    public static String DEFAULT_URL = "http://localhost:8983/solr";
    private final SolrClient solrClient;
    public SolrIndexer(String collection) {
        this(DEFAULT_URL, collection);
    }
    public SolrIndexer(String url, String collection) {
        solrClient = new HttpSolrClient.Builder(url + "/" + collection).build();
    }
    public Throwable deleteAll() {
        try {
            solrClient.deleteByQuery("*:*");
        } catch (SolrServerException | IOException ex) {
            Logger.getLogger(SolrIndexer.class.getName()).log(Level.SEVERE, null, ex);
            return ex;
        }
        return null;
    }
    public Throwable addHtml(String wpath, String path, String content) {
        System.out.println("index>>*>"+wpath + ":" + path);
        try {
            solrClient.add(makeHtml(wpath, path, content, null));
        } catch (SolrServerException | IOException ex) {
            Logger.getLogger(SolrIndexer.class.getName()).log(Level.SEVERE, null, ex);
            return ex;
        }
        return null;
    }
    public static SolrInputDocument makeHtml(String wpath, String path, String content, HashMap<String, String> attributes) {
        SolrInputDocument doc = new SolrInputDocument();
        
        doc.addField("path", path);
        doc.addField("wpath", wpath);
        doc.addField("pathidnew", wpath + "/" + path);
        doc.addField("id", wpath + "/" + path);
        doc.addField("_text_", content);
        if (attributes != null) attributes.forEach((k, v) -> { 
            switch(k) {
                case "path":
                case "wpath":
                case "_text_":
                    break;
                    default: doc.addField(k, v);
            }
        });
        return doc;
    }
    public void addDocumentsCommit(Collection<SolrInputDocument> docs) throws SolrServerException, IOException {
        if (docs.size() > 0) {
            solrClient.add(docs);
            solrClient.commit();
        }
    }
    public SolrDocumentList findHtml(String wpath, String str) throws SolrServerException, IOException {
        SolrQuery query = new SolrQuery();
        query.setFields("path");
        query.setQuery(str);
        query.addFilterQuery("wpath:"+wpath);
        query.setStart(0);
        QueryResponse response = solrClient.query(query);
        SolrDocumentList results = response.getResults();
        //results.get(0).get("path");
        return results;
    }

    public SolrClient getSolrClient() {
        return solrClient;
    }
}
