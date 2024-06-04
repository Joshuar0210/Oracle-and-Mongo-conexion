package com.mongodb;


import java.sql.SQLException;
import java.util.List;

/**
 * Main
 */
public class Main {

    public static void main(String[] args) throws SQLException {
        String connectionString = "mongodb://localhost:27017";
        String dbName = "mydatabase";
        String user="ALVAREZ";
        String password="12345678";
        String host="localhost";
        String port="1521";
        String edition="XE";

        // Crear una conexión a MongoDB
        MongoDBConnection mongoConexion = new MongoDBConnection(connectionString, dbName);
        OracleConnection oracleConexion = new OracleConnection(user,password,host,port,edition);

        // Crear un objeto Car
        Car car1 = new Car("Honda", "Amarillo");
        Car car2 = new Car("Nisan", "rojo");
        Car car3 = new Car("Toyota", "azul");

        // Insertar el objeto en MongoDB
        System.out.println("prueba");
        mongoConexion.sendObject(car1);
        mongoConexion.sendObject(car2);
        mongoConexion.sendObject(car3);
        // Insertar el objeto en Oracle
        oracleConexion.sendObject(car1);
        oracleConexion.sendObject(car2);
        oracleConexion.sendObject(car3);


List<Car> cars = mongoConexion.getAllObjects(Car.class);

for (Car car : cars) {
    System.out.println(car);
}
        // Cerrar la conexión
       // conexion.closeConnection();
    }
}
