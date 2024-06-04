package com.mongodb;



import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OracleConnection {

    private Connection connection;

    public OracleConnection(String username, String password, String host, String port, String edition) throws SQLException {
        
        OracleDatabaseConnect newConnection = new  OracleDatabaseConnect();
        this.connection = newConnection.connectWithOracle(username, password, host, port, edition);
    }

    public OracleConnection(Connection pConnection){

        this.connection = pConnection;
    }

    public void sendObject(Object pObject) {
        try {
            Class<?> objectClass = pObject.getClass();
            String tableName = objectClass.getSimpleName().toLowerCase(); 
         try{
                
                createNewTable(tableName, objectClass); 
            }catch(SQLException e){
                System.out.println(e.getMessage());
            }
            
            insertObject(tableName, objectClass, pObject); 
        } catch (SQLException | IllegalAccessException e) {
            System.err.println("Error al mapear la clase a la tabla: " + e.getMessage());
        }
    }

    public void updateObject(Object currentObject, Object newObject) {
        try {
            Class<?> objectClass = currentObject.getClass();

            String tableName = objectClass.getSimpleName().toLowerCase(); 
    
            if (!tableExist(tableName)) {
                System.out.println("La tabla " + tableName + " no existe.");
                return;
            }
    
           
            if (!objectExists(tableName, objectClass, currentObject)) {
                System.out.println("El objeto existente no se encuentra en la tabla.");
                return;
            }
    
           
            StringBuilder query = new StringBuilder("UPDATE ").append(tableName).append(" SET ");     
        
            Field[] currentObjAttributes = objectClass.getDeclaredFields();

            for (Field objAttribute : currentObjAttributes) {

                objAttribute.setAccessible(true);
                String variableName = objAttribute.getName();
                Object updatedVariableValue = objAttribute.get(newObject);
    
                if (objAttribute.getType() == String.class) {
                    query.append(variableName).append(" = '").append(updatedVariableValue).append("', ");
                } else {
                    query.append(variableName).append(" = ").append(updatedVariableValue).append(", ");
                }
            }
    
            query.delete(query.length() - 2, query.length());
    
            query.append(" WHERE ");
            for (Field currentObjAttribute : currentObjAttributes) {
                currentObjAttribute.setAccessible(true);
                String variableName = currentObjAttribute.getName();
                Object existingVariableValue = currentObjAttribute.get(currentObject);
    
                if (currentObjAttribute.getType() == String.class) {
                    query.append(variableName).append(" = '").append(existingVariableValue).append("' AND ");
                } else {
                    query.append(variableName).append(" = ").append(existingVariableValue).append(" AND ");
                }
            }
    
            query.delete(query.length() - 5, query.length());
    
            try (PreparedStatement statement = connection.prepareStatement(query.toString())) {
                statement.executeUpdate();
                System.out.println("Objeto actualizado en la tabla " + tableName + " correctamente.");
            }
        } catch (SQLException | IllegalAccessException e) {
            System.err.println("Error al actualizar el objeto en la tabla: " + e.getMessage());
        }
    }

    public <T> List<T> getAllObjects(Class<T> objectClass) {
        List<T> objList = new ArrayList<>();
        String tableName = objectClass.getSimpleName().toLowerCase(); 
        try {
            String query = "SELECT * FROM " + tableName;
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) { 
                        T object = buildObject(objectClass, resultSet);
                        objList.add(object);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al recuperar objetos de la tabla: " + e.getMessage());
        }
        return objList;
    }

    public <T> Object getObject(Class<T> objectClass, String attribute){
        
        String tableName = objectClass.getSimpleName().toLowerCase();
        String primaryKey = null;
        String query = "SELECT * FROM " + tableName.toUpperCase() + " Where ";

        try{
            String queryPK = "select column_name from user_cons_columns ucc join user_constraints uc on ucc.constraint_name = uc.constraint_name ";
            queryPK += "where uc.constraint_type = 'P' and uc.table_name = '" + tableName.toUpperCase() + "'";

            try(PreparedStatement statement = connection.prepareStatement(queryPK)){

                ResultSet resultSet = statement.executeQuery(queryPK);
                while(resultSet.next()){
                    primaryKey = (String)resultSet.getObject("COLUMN_NAME");
                }
            }

            Object object = null;

            if(primaryKey != null){

                query += primaryKey + " = +" + attribute + "'";
                try(PreparedStatement statement = connection.prepareStatement(query)){

                    ResultSet resultSet2 = statement.executeQuery(query);
                    while(resultSet2.next()){
                        object = buildObject(objectClass, resultSet2);
                        break;
                    }

                    if(object != null){
                        return object;
                    }else{
                        System.out.println("No hay objeto con ese parametro");
                    }
                }
            }
            else{
                System.out.println("No hay llave primaria"); 

                Field[] classAttributes = objectClass.getDeclaredFields();
                String defaultAttribute = classAttributes[0].getName();
                query += defaultAttribute + " = '" + attribute + "'";

                try(PreparedStatement statement = connection.prepareStatement(query)){

                    ResultSet resultSet2 = statement.executeQuery(query);
                    while(resultSet2.next()){
                        object = buildObject(objectClass, resultSet2);
                        break;
                    }

                    if(object != null){
                        return object;
                    }else{
                        System.out.println("No hay objeto con ese parametro");
                    }
                }
            }

        }catch(SQLException e){
            System.out.println(e.getMessage() + "En la tabla " + tableName);  
        }
        return null;
    }





    private void createNewTable(String pTableName, Class<?> pObjectClass) throws SQLException {
        
        if (tableExist(pTableName)) {
            System.out.println("La tabla " + pTableName + " ya existe.");
            return;
        }
        
        StringBuilder query = new StringBuilder("CREATE TABLE ")
                .append(pTableName)
                .append(" (");

        Field[] objAttributes = pObjectClass.getDeclaredFields();
        boolean primaryKey = false;

        for (Field objAttribute : objAttributes) { 

            objAttribute.setAccessible(true); 
            String variableName = objAttribute.getName();
            String variableType = getTypeObjectString(objAttribute.getType());
            query.append(variableName).append(" ").append(variableType);          
        
            if (variableName.equals(pTableName + "_id") && primaryKey == false) {
                query.append(" PRIMARY KEY");
                primaryKey = true;
            }
            query.append(", ");
        }
        
        query.delete(query.length() - 2, query.length());
        query.append(")");

        if (!primaryKey) {
            System.out.println("La clase " + pTableName + " no tiene un campo definido para clave primaria.");
        }
        System.out.println(query.toString()); 
        try (PreparedStatement statement = connection.prepareStatement(query.toString())) {
            statement.executeUpdate();
            System.out.println("Tabla " + pTableName + " creada correctamente.");
        }
    }

    private void insertObject(String pTableName, Class<?> pObjectClass, Object pObject) throws SQLException, IllegalAccessException {

        if(!objectExists(pTableName, pObjectClass, pObject)){

            StringBuilder query = new StringBuilder("INSERT INTO ").append(pTableName).append(" (");  
            StringBuilder values = new StringBuilder("VALUES (");

            Field[] objAttributes = pObjectClass.getDeclaredFields();

            for (Field objAttribute : objAttributes) {
                objAttribute.setAccessible(true);
                String variableName = objAttribute.getName();
                Object variableType = objAttribute.get(pObject);

                query.append(variableName).append(", ");
                values.append("'").append(variableType).append("', ");
            }
        
            query.delete(query.length() - 2, query.length());
            values.delete(values.length() - 2, values.length());

            query.append(") ");
            values.append(")");

            String insertQuery = query.toString() + values.toString();

            try (PreparedStatement statement = connection.prepareStatement(insertQuery)) {
                statement.executeUpdate();
                System.out.println("Datos insertados en la tabla " + pTableName + " correctamente.");
            }
        }else{
            System.out.println("El dato ingresado ya existe en su base de datos");
        }
    }

    private boolean tableExist(String pTableName) throws SQLException {
        String query = "SELECT count(*) FROM user_tables WHERE table_name = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, pTableName.toUpperCase());

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {

                    int count = resultSet.getInt(1);
                    return count > 0;
                }
            }
        }
        return false;
    }

    private String getTypeObjectString(Class<?> pAttributeType) {
        if (pAttributeType == String.class) {
            return "VARCHAR(255)";
        } else if (pAttributeType == int.class || pAttributeType == Integer.class) {
            return "INT";
        } else if (pAttributeType == double.class || pAttributeType == Double.class) {
            return "DOUBLE";
        } else if (pAttributeType == float.class || pAttributeType == Float.class) {
            return "FLOAT";
        } else if (pAttributeType == boolean.class || pAttributeType == Boolean.class) {
            return "BOOLEAN";
        } else {
            return "VARCHAR(255)";
        }
    }

    private boolean objectExists(String pTableName, Class<?> objectClass,  Object pObjeto) throws IllegalArgumentException, IllegalAccessException, SQLException{

        String query = "SELECT count(*) FROM " + pTableName.toUpperCase() + " WHERE ";

        Field[] objAttributes = objectClass.getDeclaredFields();

        for(Field objAttribute : objAttributes){

            objAttribute.setAccessible(true);
            Object valorCampo = objAttribute.get(pObjeto);

            if(objAttribute.getType() == String.class){
                
                query += objAttribute.getName().toUpperCase() + " = " + "'" + valorCampo +  "' AND ";

            }else{

                query += objAttribute.getName().toUpperCase() + " = " + valorCampo + " AND ";

            }
        }

        query = query.substring(0, query.length() - 5);

        try (PreparedStatement statement = connection.prepareStatement(query)) {

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {

                int count = resultSet.getInt(1);
                return count > 0;
            }
            
        }

        return false;
    }

    private <T> T buildObject(Class<T> objectClass, ResultSet resultSet) throws SQLException {
        try {
            Constructor<T> constructor = objectClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            T instance = constructor.newInstance();

            Field[] objAttributes = objectClass.getDeclaredFields();

            for (Field objAttribute : objAttributes) {
                
                objAttribute.setAccessible(true);
                String variableName = objAttribute.getName();
                Object newObject = resultSet.getObject(variableName);

                if (newObject != null) {
                    if (objAttribute.getType() == int.class || objAttribute.getType() == Integer.class) {
                        newObject = ((Number) newObject).intValue();
                    } else if (objAttribute.getType() == double.class || objAttribute.getType() == Double.class) {
                        newObject = ((Number) newObject).doubleValue();
                    } else if (objAttribute.getType() == float.class || objAttribute.getType() == Float.class) {
                        newObject = ((Number) newObject).floatValue();
                    } else if (objAttribute.getType() == long.class || objAttribute.getType() == Long.class) {
                        newObject = ((Number) newObject).longValue();
                    } else if (objAttribute.getType() == boolean.class || objAttribute.getType() == Boolean.class) {
                        newObject = (newObject instanceof Number) ? ((Number) newObject).intValue() != 0 : Boolean.parseBoolean(newObject.toString());
                    }

                    objAttribute.set(instance, newObject);
                }
            }

            return instance;
        } catch (NoSuchMethodException e) {
            throw new SQLException("No se pudo encontrar el constructor predeterminado para la clase " + objectClass.getSimpleName() + ": " + e.getMessage(), e);
        } catch (InstantiationException | IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
            throw new SQLException("Error al instanciar la clase " + objectClass.getSimpleName() + ": " + e.getMessage(), e);
        }
    }
}