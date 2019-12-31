package com.gjg.leaderboard;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
//@Slf4j
class LoadDatabase {

    Logger logger = LoggerFactory.getLogger(LoadDatabase.class);

    @Bean
    CommandLineRunner initDatabase(PlayerRepository repository) {
        return args -> {
//            logger.info("Preloading " + repository.save(new Player("Bilbo Baggins", "tr")));
//            logger.info("Preloading " + repository.save(new Player("Frodo Baggins", "us")));

        };
    }
}


