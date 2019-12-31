package com.gjg.leaderboard.pojo;


import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.UUID;

@Data
@Entity
public class Player {

    /**
     * The UUID representation for player
     */
    private @Id
    UUID userUuid;
    /**
     * Display name of the player
     */
    private String displayName;
    /**
     * Top score for the player
     */
    private double points;
    /**
     * The rank of the player in the global leaderboard
     */
    private long rank;
    /**
     * Country representation of the player as country iso code
     */
    private String country;

    Player() {
    }

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

    public UUID getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(UUID userUuid) {
        this.userUuid = userUuid;
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
