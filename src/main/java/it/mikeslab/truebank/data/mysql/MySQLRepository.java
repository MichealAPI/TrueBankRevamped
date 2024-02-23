package it.mikeslab.truebank.data.mysql;

import it.mikeslab.truebank.data.Repository;
import it.mikeslab.truebank.util.LoggerUtil;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.Map;
import java.util.logging.Level;

public class MySQLRepository<T extends ConfigurationSerializable> implements Repository<T> {

    private final MySQLService service;
    private Class<T> type;

    public MySQLRepository(MySQLService service, Class<T> type) {
        this.service = service;

        // Set the type of the repository, strictly necessary and related to the POJO based approach
        this.setType(type);
    }


    @Override
    public String save(T obj, Object... args) {
        try {
            return service.save(obj, args);
        } catch (Exception e) {
            // Log the exception and rethrow it, or handle it in some other way
            throw new RuntimeException("Failed to save object", e);
        }
    }

    @Override
    public T get(String id) {

        // todo, analyze this int string conversion problem in find, be careful. Id should be also alphanumeric occasionally

        Map.Entry<String, Object> entryMap = service.find(new Document("id", new ObjectId(id)));

        // Removed !type.isInstance(entryMap.getValue()) from the if statement
        // Since find method creates a new un-casted instance of a ConfigurationSerializable
        // never instance of T, the check is always false

        if (entryMap == null) {
            LoggerUtil.log(Level.WARNING, LoggerUtil.LogSource.DATABASE, "(GET) MySQL entryMap is null or type is not instance of entryMap.getValue()");
            return null;
        }


        return type.cast(entryMap.getValue());
    }

    @Override
    public Map.Entry<String, Object> find(Document document) {
        return service.find(document);
    }


    @Override
    public void update(String id, T obj) {
        service.update(id, obj);
    }

    @Override
    public void delete(String id) {
        service.delete(id);
    }


    @Override
    public void setRepositoryName(String tableName) {
        service.setTable(tableName);
    }

    @Override
    public String getRepositoryName() {
        return service.getTableName();
    }

    @Override
    public void setColumns(String[] columns) {

        // do we really need to set columns? Shouldn't
        // columns be predefined in a previous statement

    }

    @Override
    public void setType(Class<T> type) {
        this.type = type;
        this.service.setClass(type);
    }

    @Override
    public void close() {
        service.disconnect();
    }
}