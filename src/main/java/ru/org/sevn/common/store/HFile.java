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
package ru.org.sevn.common.store;

import ru.org.sevn.common.data.DBProperty;
import ru.org.sevn.common.data.DBTableProperty;

@DBTableProperty(name = "HOME_STORE")
public class HFile {
    
    @DBProperty(name = "ID", dtype = "INTEGER PRIMARY KEY  AUTOINCREMENT   NOT NULL")
    private Long id;
    @DBProperty(name = "ID_FILE", dtype = "INTEGER NOT NULL")
    private long idfile;
    
    @DBProperty(name = "STORE_VOL", dtype = "TEXT NOT NULL")
    private String storeVol;
    @DBProperty(name = "STORE_PATH", dtype = "TEXT NOT NULL")
    private String storePath;
    @DBProperty(name = "VERS", dtype = "INTEGER") //0 is current
    private int version;
    @DBProperty(name = "TDELETE", dtype = "INTEGER")
    private long deleteTime;
    @DBProperty(name = "NAME", dtype = "TEXT NOT NULL")
    private String name;
    // 00000001 1 0
    // 00000010 2 1
    // 00000100 4 2 
    // 00001000 8 3
    // h&t != 0
    @DBProperty(name = "TAGS", dtype = "INTEGER")
    private long tags;
    @DBProperty(name = "UTAGS", dtype = "TEXT")
    private String utags;
    
    private long parentId;
    @DBProperty(name = "TVERSION_END", dtype = "INTEGER")
    private long versionEndTime; //begin lastModifiedTime
    @DBProperty(name = "TTDELETE", dtype = "INTEGER")
    private long toDeleteTime;
    // java.nio.file.attribute.BasicFileAttributes
    @DBProperty(name = "TMODIF", dtype = "INTEGER")
    private long lastModifiedTime;
    @DBProperty(name = "TACCESS", dtype = "INTEGER")
    private long lastAccessTime;
    @DBProperty(name = "TCREATION", dtype = "INTEGER")
    private long creationTime;
    @DBProperty(name = "IS_REGUL", dtype = "INTEGER")
    private boolean isRegularFile;
    @DBProperty(name = "IS_DIR", dtype = "INTEGER")
    private boolean isDirectory;
    @DBProperty(name = "IS_SLINK", dtype = "INTEGER")
    private boolean isSymbolicLink;
    @DBProperty(name = "IS_OTHER", dtype = "INTEGER")
    private boolean isOther;
    @DBProperty(name = "FSIZE", dtype = "INTEGER")
    private long size;

    
}
