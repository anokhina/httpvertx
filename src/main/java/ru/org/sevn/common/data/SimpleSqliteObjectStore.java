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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SimpleSqliteObjectStore {

    private ObjectDescriptor getObjectDescriptor(Class objType) {
        return objectDescriptorMap.get(objType);
    }
    
    public interface ObjectMapper<T> {
        Class getType();
        void mapValues(T o, String colName, ResultSet rs) throws SQLException ;
        void setStatement(T o, String colName, int parameterIndex, PreparedStatement pstmt) throws SQLException ;
    }
    private final boolean loaded;
    private final String filePath;
    private final Map<Class, ObjectDescriptor> objectDescriptorMap = new HashMap<>();
    
    public SimpleSqliteObjectStore(String filePath, ObjectMapper ... ompArr) {
        this.filePath = filePath;
        boolean l = false;
        try {
            Class.forName("org.sqlite.JDBC");
            l = true;
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(SimpleSqliteObjectStore.class.getName()).log(Level.SEVERE, null, ex);
        }
        loaded = l;
        for (ObjectMapper omp : ompArr) {
            ObjectDescriptor objectDescriptor = new ObjectDescriptor(omp);
            objectDescriptorMap.put(objectDescriptor.getObjectMapper().getType(), objectDescriptor);
        }
        File dbFile = new File(filePath);
        if (!dbFile.exists()) {
            initDB();
        }
    }

    static class ObjectDescriptor {
        private final ObjectMapper objectMapper;
        private String createSql;
        private String addSql;

        public ObjectMapper getObjectMapper() {
            return objectMapper;
        }
        
        
        public ObjectDescriptor(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            makeCreateSql();
            makeAddSql();
        }
    
        public String getTableName() {
            DBTableProperty[] tableProp = (DBTableProperty[])objectMapper.getType().getAnnotationsByType(DBTableProperty.class);
            if (tableProp.length == 0) return null;
            return tableProp[0].name();
        }    
        private void makeCreateSql() {
            if (createSql == null) {
                String tableName = getTableName();
                if (tableName == null) return;

                StringBuilder sb = new StringBuilder();
                sb.append("CREATE TABLE ").append(tableName).append(" (");
                Field[] flds = objectMapper.getType().getDeclaredFields();
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
        private void makeAddSql() {
            if (addSql == null) {
                String tableName = getTableName();
                if (tableName == null) return;

                StringBuilder sb = new StringBuilder();
                StringBuilder sb1 = new StringBuilder();
                sb.append("INSERT INTO ").append(tableName).append(" (");
                Field[] flds = objectMapper.getType().getDeclaredFields();
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
        public Collection getObjects(Connection c, String sql, String[] colName, Object[] val, String[] orderByFields, String[] orderByOrder) throws Exception {
            ArrayList ret = new ArrayList();

            if (sql == null) {
                StringBuilder sb = new StringBuilder();
                sb.append("SELECT * FROM ").append(getTableName()).append(" WHERE ");
                for (int j = 0; j < colName.length; j++) {
                    if (j > 0) {
                        sb.append(" and ");
                    }
                    if (val[j] instanceof Object[]) {
                        sb.append(colName[j]).append(" between ? and ? ");
                    } else {
                        sb.append(colName[j]).append("=? ");
                    }
                }
                if (orderByFields != null) {
                    sb.append(" ORDER BY ");
                    for (int i = 0; i < orderByFields.length; i++) {
                        if (i > 0) { sb.append(", "); }
                        sb.append(orderByFields[i]);
                        if (orderByOrder != null && orderByOrder.length > i && orderByOrder[i] != null) {
                            sb.append(" ").append(orderByOrder[i]);
                        }
                    }
                }
                sql = sb.toString();
            }

            PreparedStatement pstmt = c.prepareStatement(sql);
            try {
                int j = 0;
                for (int i = 0; i < colName.length; i++) {
                    if (val[i] instanceof Object[]) {
                        Object[] btw = (Object[]) val[i];
                        pstmt.setObject(++j, btw[0]);
                        pstmt.setObject(++j, btw[1]);
                    } else {
                        pstmt.setObject(++j, val[i]);
                    }
                }

                ResultSet rs = pstmt.executeQuery();
                java.sql.ResultSetMetaData rsmd = rs.getMetaData();
                while (rs.next()) {
                    Object o = objectMapper.getType().getConstructor().newInstance();
                    int colNum = rsmd.getColumnCount();
                    for (int i = 1; i <= colNum; i++) {
                        objectMapper.mapValues(o, rsmd.getColumnName(i), rs);
                    }
                    ret.add(o);
                }
            } finally {
                pstmt.close();
            }
            return ret;
        }
        public int addObject(Connection c, Object o) throws SQLException {
            PreparedStatement pstmt = c.prepareStatement(addSql);
            try {
                Field[] flds = objectMapper.getType().getDeclaredFields();
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

                return pstmt.executeUpdate();
            } finally {
                pstmt.close();
            }
        }
        protected void initDB(Connection c) throws SQLException {
            Statement stmt = c.createStatement();
            try {
                stmt.executeUpdate(createSql);
            } finally {
                stmt.close();
            }
        }
    }
    
    protected void initDB() {
        if (loaded) {
            try {
                
                final Connection c = DriverManager.getConnection(getConnectionString());
                try {
                    this.objectDescriptorMap.values().stream().forEach(objectDescriptor -> {
                        try {
                            objectDescriptor.initDB(c);
                        } catch (SQLException ex) {
                            Logger.getLogger(SimpleSqliteObjectStore.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    });
                } finally {
                    c.close();                    
                }
            } catch (SQLException ex) {
                Logger.getLogger(SimpleSqliteObjectStore.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public String getConnectionString() {
        return "jdbc:sqlite:"+filePath;
    }
    
    public Collection getObjects(Class objType, String[] colName, Object[] val) throws Exception {
        return getObjects(objType, null, colName, val, null, null);
    }
    
    public Collection getObjects(Class objType, String sql, String[] colName, Object[] val, String[] orderByFields, String[] orderByOrder) throws Exception {
        Collection ret = new ArrayList();
        ObjectDescriptor objectDescriptor = getObjectDescriptor(objType);
        if (loaded && objectDescriptor != null) {
            try {
                Connection c = DriverManager.getConnection(getConnectionString());
                try {
                    ret = objectDescriptor.getObjects(c, sql, colName, val, orderByFields, orderByOrder);
                } finally {
                    c.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(SimpleSqliteObjectStore.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return ret;
    }
        

    public int addObject(Object o) {
        int ret = 0;
        ObjectDescriptor objectDescriptor = getObjectDescriptor(o.getClass());
        if (loaded) {
            try {
                Connection c = DriverManager.getConnection(getConnectionString());
                try {
                    objectDescriptor.addObject(c, o);
                } finally {
                    c.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(SimpleSqliteObjectStore.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        return ret;
    }
}
