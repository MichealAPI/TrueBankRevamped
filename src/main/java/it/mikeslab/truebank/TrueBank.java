package it.mikeslab.truebank;

import it.mikeslab.truebank.data.EntityStyle;
import it.mikeslab.truebank.data.Repository;
import it.mikeslab.truebank.data.mongodb.MongoDBImpl;
import it.mikeslab.truebank.data.mongodb.MongoDBRepository;
import it.mikeslab.truebank.data.mongodb.MongoDBService;
import it.mikeslab.truebank.data.yaml.YamlRepository;
import it.mikeslab.truebank.pojo.Card;
import it.mikeslab.truebank.pojo.database.URIBuilder;
import org.bson.types.ObjectId;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class TrueBank extends JavaPlugin {

    private Repository<Card> cardRepository;

    private Repository<Card> cardTestRepository;

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();

        URIBuilder uriBuilder = URIBuilder.builder()
                .username("mikeslab")
                .password("mikeslab")
                .host("cluster0.mqkev3k.mongodb.net")
                .port(27017)
                .database("blog")
                .build();

        MongoDBService mongoDBService = new MongoDBImpl(uriBuilder);

        this.cardRepository = new MongoDBRepository<>(mongoDBService, Card.class);
        cardRepository.setRepositoryName("posts");


        Card card = Card.builder()
                .test(1)
                .build();

        String id = cardRepository.save(card);

        Card card1 = cardRepository.get(id.toString());

        System.out.println(card1.getTest());


        cardRepository.setRepositoryName("comments");

        Card card2 = Card.builder()
                .test(2)
                .build();

        String id2 = cardRepository.save(card2);

        Card card3 = cardRepository.get(id2.toString());

        System.out.println(card3.getTest());

        System.out.println("Done, next step...");

        this.cardTestRepository = new YamlRepository<>(this.getConfig(),
                                                       EntityStyle.INCREMENTAL,
                                                       new File(this.getDataFolder(), "config.yml"));

        this.cardTestRepository.setRepositoryName("cards");
        this.cardTestRepository.setType(Card.class);

        Card card4 = Card.builder()
                .test(3)
                .build();


        String id3 = this.cardTestRepository.save(card4);

        System.out.println(id3.toString() + " IDDDD from YamlRepository");
        Card card5 = this.cardTestRepository.get(id3.toString());

        System.out.println(card5.getTest() + " from YamlRepository");

    }

    @Override
    public void onDisable() {

        cardRepository.close(); // Close the repository, disconnect from the database for example

    }
}
