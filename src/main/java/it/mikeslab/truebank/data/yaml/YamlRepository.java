package it.mikeslab.truebank.data.yaml;

import it.mikeslab.truebank.data.EntityStyle;
import it.mikeslab.truebank.data.Repository;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.io.File;
import java.util.UUID;

@RequiredArgsConstructor
public class YamlRepository<T extends ConfigurationSerializable> implements Repository<T> {

    private final FileConfiguration configurationFile;
    private final EntityStyle entityStyle;
    private final File configFile;
    private Class<T> type;

    private String repositoryName;

    @Override
    public String save(T obj, Object... args) {

        int dataSize;
        String key = null;

        switch (entityStyle) {

            case UUID:
                UUID userUUID = (UUID) args[0];
                configurationFile.set(repositoryName + "." + userUUID, obj);
                key = userUUID.toString();
                break;

            case INCREMENTAL:

                if(!configurationFile.isConfigurationSection(repositoryName)) {
                    configurationFile.createSection(repositoryName);
                }


                dataSize = configurationFile
                        .getConfigurationSection(repositoryName)
                        .getKeys(false)
                        .size();

                dataSize++; // Increment the size by 1 to get the next available id

                configurationFile.set(repositoryName + "." + dataSize, obj);
                key = String.valueOf(dataSize);
                break;

        }

        this.saveYamlConfiguration();

        // Since we're using ObjectIDs in our actual implementation, we should pad our string in order to be converted to
        // an hex string, which is the format expected by the ObjectId constructor.

        return key;
    }

    @Override
    public void update(String id, T obj) {
        configurationFile.set(repositoryName + "." + id, obj);
        this.saveYamlConfiguration();
    }

    @Override
    public void delete(String id) {
        configurationFile.set(repositoryName + "." + id, null);
        this.saveYamlConfiguration();
    }

    @Override
    public T get(String id) {
        return configurationFile.getSerializable(repositoryName + "." + id, type);
    }

    @Override
    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    @Override
    public String getRepositoryName() {
        return this.repositoryName;
    }

    @Override
    public void setColumns(String[] columns) {

        // Since we are using YAML, we don't need to set columns.

    }

    @Override
    public void setType(Class<T> type) {
        this.type = type;
    }

    private void saveYamlConfiguration() {
        try {
            configurationFile.save(configFile);
        } catch (Exception e) {
            e.printStackTrace(); // TODO: Replace with a more robust logging system
        }
    }

    @Override
    public void close() {
        this.saveYamlConfiguration();
    }

}
