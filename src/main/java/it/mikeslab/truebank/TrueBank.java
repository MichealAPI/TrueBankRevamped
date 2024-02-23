package it.mikeslab.truebank;

import it.mikeslab.truebank.data.Repository;
import it.mikeslab.truebank.pojo.Card;
import it.mikeslab.truebank.util.RepositoryUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public final class TrueBank extends JavaPlugin {



    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();

        ConfigurationSection cardConfig = getConfig().getConfigurationSection("cardDb");

        Repository<Card> testRepo = new RepositoryUtil<>(
                cardConfig,
                Card.class,
                "card-database"
        ).fromConfig();

        testRepo.setRepositoryName("card-database");

        testRepo.save(new Card(122112, "test3t"));


    }

    @Override
    public void onDisable() {

    }
}
