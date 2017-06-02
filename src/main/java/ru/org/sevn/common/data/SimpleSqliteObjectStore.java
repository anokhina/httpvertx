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
package ru.org.sevn.common.data;

import java.io.File;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import ru.org.sevn.jvert.ChatHandler;

public class SimpleSqliteObjectStore {
    
    public interface ObjectMapper<T> {
        void mapValues(T o, String colName, ResultSet rs) throws SQLException ;
        void setStatement(T o, String colName, int parameterIndex, PreparedStatement pstmt) throws SQLException ;
    }
    private final boolean loaded;
    private final String filePath;
    private final Class objectClass;
    private final ObjectMapper objectMapper;
    
    public SimpleSqliteObjectStore(String filePath, Class ocl, ObjectMapper omp) {
        this.filePath = filePath;
        this.objectClass = ocl;
        this.objectMapper = omp;
        boolean l = false;
        try {
            Class.forName("org.sqlite.JDBC");
            l = true;
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ChatHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        loaded = l;
        makeCreateSql();
        makeAddSql();
        File dbFile = new File(filePath);
        if (!dbFile.exists()) {
            initDB();
        }
    }
    
    public String getTableName() {
        DBTableProperty[] tableProp = (DBTableProperty[])objectClass.getAnnotationsByType(DBTableProperty.class);
        if (tableProp.length == 0) return null;
        return tableProp[0].name();
    }
    
    private String createSql;
    private String addSql;
    
    private void makeCreateSql() {
        if (createSql == null) {
            String tableName = getTableName();
            if (tableName == null) return;

            StringBuilder sb = new StringBuilder();
            sb.append("CREATE TABLE ").append(tableName).append(" (");
            Field[] flds = objectClass.getDeclaredFields();
            int j = 0;
            for (int i = 0; i < flds.length; i++) {
                DBProperty[] fldProp = (DBProperty[])flds[i].getAnnotationsByType(DBProperty.class);
                if (fldProp.length > 0) {
                    if (j > 0) {
                        sb.append(", ");
                    }
                    sb.append(fldProp[0].name()).append(" ").append(fldProp[0].dtype());
                    j++;
                }
            }
            sb.append(")");
            createSql = sb.toString();
        }
    }
    
    protected void initDB() {
        if (loaded) {
            try {
                
                Connection c = DriverManager.getConnection(getConnectionString());
                try {
                    Statement stmt = c.createStatement();
                    
                    stmt.executeUpdate(createSql);
                    stmt.close();
                } finally {
                    c.close();                    
                }
            } catch (SQLException ex) {
                Logger.getLogger(ChatHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    protected String getConnectionString() {
        return "jdbc:sqlite:"+filePath;
    }
    
    public Collection getObjects(String[] colName, Object[] val) throws Exception {
        ArrayList ret = new ArrayList();
        if (loaded) {
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT * FROM ").append(getTableName()).append(" WHERE ");
            int j = 0;
            for (String cn : colName) {
                if (j > 0) {
                    sb.append(" and ");
                }
                sb.append(cn).append("=? ");
                j++;
            }
            
            try {
                Connection c = DriverManager.getConnection(getConnectionString());
                try {
                    PreparedStatement pstmt = c.prepareStatement(sb.toString());
                    for (int i = 0; i < colName.length; i++) {
                        pstmt.setObject(i + 1, val[i]);
                    }

                    ResultSet rs = pstmt.executeQuery();
                    java.sql.ResultSetMetaData rsmd = rs.getMetaData();
                    while (rs.next()) {
                        Object o = objectClass.getConstructor().newInstance();
                        int colNum = rsmd.getColumnCount();
                        for (int i = 1; i <= colNum; i++) {
                            objectMapper.mapValues(o, rsmd.getColumnName(i), rs);
                        }
                        ret.add(o);
                    }
                    pstmt.close();
                } finally {
                    c.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(ChatHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return ret;
    }
        
    private void makeAddSql() {
        if (addSql == null) {
            String tableName = getTableName();
            if (tableName == null) return;

            StringBuilder sb = new StringBuilder();
            StringBuilder sb1 = new StringBuilder();
            sb.append("INSERT INTO ").append(tableName).append(" (");
            Field[] flds = objectClass.getDeclaredFields();
            int j = 0;
            for (int i = 0; i < flds.length; i++) {
                DBProperty[] fldProp = (DBProperty[])flds[i].getAnnotationsByType(DBProperty.class);
                if (fldProp.length > 0) {
                    if (j > 0) {
                        sb.append(", ");
                        sb1.append(", ");
                    }
                    if (!"ID".equals(fldProp[0].name())) {
                        sb.append(fldProp[0].name()).append(" ");
                        sb1.append("?");
                        j++;
                    }
                }
            }
            sb.append(")");
            sb.append(" VALUES (").append(sb1).append(")");
            addSql = sb.toString();
        }
    }
    public int addObject(Object o) {
        int ret = 0;
        if (loaded) {
            try {
                Connection c = DriverManager.getConnection(getConnectionString());
                try {

                    PreparedStatement pstmt = c.prepareStatement(addSql);

                    Field[] flds = objectClass.getDeclaredFields();
                    int j = 0;
                    for (int i = 0; i < flds.length; i++) {
                        DBProperty[] fldProp = (DBProperty[])flds[i].getAnnotationsByType(DBProperty.class);
                        if (fldProp.length > 0) {
                            if (!"ID".equals(fldProp[0].name())) {
                                j++;
                                objectMapper.setStatement(o, fldProp[0].name(), j, pstmt);
                            }
                        }
                    }
                    
                    ret = pstmt.executeUpdate();
                    pstmt.close();
                } finally {
                    c.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(ChatHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        return ret;
    }
}
