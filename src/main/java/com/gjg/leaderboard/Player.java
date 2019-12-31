package com.gjg.leaderboard;


import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.UUID;

@Data
@Entity
public class Player {

    private @Id /*@Transient*/ UUID userUuid;
    private String displayName;
    private double points;
    private long rank;
    private String country;
    Player() {}

    Player(String displayName, String country) {
        this.userUuid = UUID.randomUUID();
        this.displayName = displayName;
        this.country = country;
    }

    public Player(UUID userUuid, String displayName, String country) {
        this.userUuid = userUuid;
        this.displayName = displayName;
        this.country = country;
    }

    public Player(UUID userUuid, String displayName, String country, double points) {
        this.userUuid = userUuid;
        this.displayName = displayName;
        this.country = country;
        this.points = points;
    }

    public Player(UUID userUuid, String displayName, String country, double points, long rank) {
        this.userUuid = userUuid;
        this.displayName = displayName;
        this.country = country;
        this.points = points;
        this.rank = rank;
    }


    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public UUID getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(UUID userUuid) {
        this.userUuid = userUuid;
    }

    public double getPoints() {
        return points;
    }

    public void setPoints(double points) {
        this.points = points;
    }

    public long getRank() {
        return rank;
    }

    public void setRank(long rank) {
        this.rank = rank;
    }
}






