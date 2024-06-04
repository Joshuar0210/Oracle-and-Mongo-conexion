package com.mongodb;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class MongoDBConnection {

    private final MongoClient mongoClient;
    private final MongoDatabase database;

    public MongoDBConnection(String connectionString, String dbName) {
        this.mongoClient = MongoClients.create(connectionString);
        this.database = mongoClient.getDatabase(dbName);
    }

    public void sendObject(Object pObject) {
        try {
            Class<?> objectClass = pObject.getClass();
            String collectionName = objectClass.getSimpleName().toLowerCase();
            MongoCollection<Document> collection = database.getCollection(collectionName);

            Document document = new Document();
            for (Field field : objectClass.getDeclaredFields()) {
                field.setAccessible(true);
                document.append(field.getName(), field.get(pObject));
            }

            collection.insertOne(document);
            System.out.println("Datos insertados en la colección " + collectionName + " correctamente.");
        } catch (IllegalAccessException e) {
            System.err.println("Error al mapear la clase al documento: " + e.getMessage());
        }
    }

    public void updateObject(Object currentObject, Object newObject) {
        try {
            Class<?> objectClass = currentObject.getClass();
            String collectionName = objectClass.getSimpleName().toLowerCase();
            MongoCollection<Document> collection = database.getCollection(collectionName);

            Document currentDoc = new Document();
            Document newDoc = new Document();
            for (Field field : objectClass.getDeclaredFields()) {
                field.setAccessible(true);
                currentDoc.append(field.getName(), field.get(currentObject));
                newDoc.append(field.getName(), field.get(newObject));
            }

            Bson filter = Filters.and(currentDoc.entrySet().stream()
                    .map(entry -> Filters.eq(entry.getKey(), entry.getValue()))
                    .toArray(Bson[]::new));

            collection.replaceOne(filter, newDoc);
            System.out.println("Documento actualizado en la colección " + collectionName + " correctamente.");
        } catch (IllegalAccessException e) {
            System.err.println("Error al actualizar el documento en la colección: " + e.getMessage());
        }
    }

    public <T> List<T> getAllObjects(Class<T> objectClass) {
        List<T> objList = new ArrayList<>();
        String collectionName = objectClass.getSimpleName().toLowerCase();
        MongoCollection<Document> collection = database.getCollection(collectionName);

        for (Document doc : collection.find()) {
            try {
                T object = objectClass.getDeclaredConstructor().newInstance();
                for (Field field : objectClass.getDeclaredFields()) {
                    field.setAccessible(true);
                    field.set(object, doc.get(field.getName()));
                }
                objList.add(object);
            } catch (Exception e) {
                System.err.println("Error al construir el objeto de la colección: " + e.getMessage());
            }
        }
        return objList;
    }

    public <T> T getObject(Class<T> objectClass, String attribute, Object value) {
        String collectionName = objectClass.getSimpleName().toLowerCase();
        MongoCollection<Document> collection = database.getCollection(collectionName);

        Document doc = collection.find(Filters.eq(attribute, value)).first();
        if (doc != null) {
            try {
                T object = objectClass.getDeclaredConstructor().newInstance();
                for (Field field : objectClass.getDeclaredFields()) {
                    field.setAccessible(true);
                    field.set(object, doc.get(field.getName()));
                }
                return object;
            } catch (Exception e) {
                System.err.println("Error al construir el objeto de la colección: " + e.getMessage());
            }
        } else {
            System.out.println("No se encontró un documento con " + attribute + " = " + value);
        }
        return null;
    }
}
