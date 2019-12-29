package com.gjg.leaderboard;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.UUID;


@Data
@Entity
public class Player {
    //private @Id @GeneratedValue Long id;
    private String name;
    private String country;
    private @Id /*@Transient*/ UUID uuid = UUID.randomUUID();
    Player() {}

    public Player(String name, String country) {
        this.name = name;
        this.country = country;
    }

    public Player(UUID uuid, String name, String country) {
        this.uuid = uuid;
        this.name = name;
        this.country = country;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

}






