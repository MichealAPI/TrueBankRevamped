package it.mikeslab.truebank.util;

import it.mikeslab.truebank.data.EDatabase;
import it.mikeslab.truebank.data.EntityStyle;
import it.mikeslab.truebank.data.Repository;
import it.mikeslab.truebank.data.mongodb.MongoDBImpl;
import it.mikeslab.truebank.data.mongodb.MongoDBRepository;
import it.mikeslab.truebank.data.mongodb.MongoDBService;
import it.mikeslab.truebank.data.mysql.MySQLImpl;
import it.mikeslab.truebank.data.mysql.MySQLRepository;
import it.mikeslab.truebank.data.mysql.MySQLService;
import it.mikeslab.truebank.data.yaml.YamlRepository;
import it.mikeslab.truebank.pojo.database.URIBuilder;
import lombok.AllArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.io.File;
import java.util.logging.Level;

@AllArgsConstructor
public class RepositoryUtil<T extends ConfigurationSerializable> {

    private ConfigurationSection theDbConfigSection;
    private Class<T> thePojoClazz;
    private String theServiceName;


    /**
     * Generates a Repository from the configuration.
     * @return The Repository.
     */
    public Repository<T> fromConfig() {

        String databaseTypeAsString = theDbConfigSection.getString("type", null);

        // Checking if property is correctly set
        if (databaseTypeAsString == null) {
            LoggerUtil.log(Level.SEVERE,
                           LoggerUtil.LogSource.CONFIG,
                    "[" + theServiceName + "] Database type not found in configuration (" + theDbConfigSection.getCurrentPath() + "." + theServiceName + ".type)"
            );
            return null;
        }

        // Parsing the string to the enum
        EDatabase databaseType;


        // Little error handling
        try {
            databaseType = EDatabase.valueOf(databaseTypeAsString);
        } catch (IllegalArgumentException e) {
            LoggerUtil.log(Level.SEVERE, LoggerUtil.LogSource.CONFIG, "[" + theServiceName + "] Invalid database type: " + databaseTypeAsString);
            return null;
        }

        switch (databaseType) {
            case MONGODB:
                return connectMongoDB();
            case MYSQL:
                return connectMySQL();
            case YAML:
                return connectYAML();
            default:
                LoggerUtil.log(Level.SEVERE, LoggerUtil.LogSource.CONFIG, "[" + theServiceName + "] Invalid database type: " + databaseTypeAsString);
                return null;
        }


    }

    /**
     * Generates a URIBuilder for a MongoDB database.
     * @return The URIBuilder.
     */
    URIBuilder generateURIBuilder() {
        return URIBuilder.builder()
                .username(this.theDbConfigSection.getString("username"))
                .password(this.theDbConfigSection.getString("password", null)) // defaults to null since password is optional
                .host(this.theDbConfigSection.getString("host"))
                .port(this.theDbConfigSection.getInt("port")) // default port
                .database(this.theDbConfigSection.getString("database"))
                .build();
    }

    /**
     * Generates a URIBuilder for a YAML file.
     * YAML files do not require a username, password, host, port, or database but
     * they do require a path and entityStyle.
     * @return The URIBuilder.
     */
    URIBuilder generateYamlURIBuilder() {
        return URIBuilder.builder()
                .path(this.theDbConfigSection.getString("path"))
                .style(EntityStyle.valueOf(this.theDbConfigSection.getString("entityStyle")))
                .build();
    }

    /**
     * Connects to a MongoDB database.
     * @return The repository.
     */
    Repository<T> connectMongoDB() {

        URIBuilder uriBuilder = generateURIBuilder();

        MongoDBService mongoDBService = new MongoDBImpl(uriBuilder);

        return new MongoDBRepository<>(mongoDBService, thePojoClazz);
    }

    /**
     * Connects to a MySQL database.
     * @return The repository.
     */
    Repository<T> connectMySQL() {

        URIBuilder uriBuilder = generateURIBuilder();

        MySQLService mySQLService = new MySQLImpl(uriBuilder);

        return new MySQLRepository<>(mySQLService, thePojoClazz);
    }

    /**
     * Connects to a YAML file.
     * @return The repository.
     */
    Repository<T> connectYAML() {

        URIBuilder uriBuilder = generateYamlURIBuilder();

        File file = new File(uriBuilder.getPath());

        FileConfiguration fileConfiguration = YamlConfiguration.loadConfiguration(file);

        return new YamlRepository<>(fileConfiguration,
                                    uriBuilder.getStyle(),
                                    file);
    }



}
