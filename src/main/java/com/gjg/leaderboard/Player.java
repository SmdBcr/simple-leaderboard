package com.gjg.leaderboard;


import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.UUID;


@Entity
public class Player {

    private @Id /*@Transient*/ UUID userUuid = UUID.randomUUID();
    private String displayName;
    private int points;
    private long rank;
    private String country;
    Player() {}

    public Player(String displayName, String country) {
        this.displayName = displayName;
        this.country = country;
    }

    public Player(UUID userUuid, String displayName, String country) {
        this.userUuid = userUuid;
        this.displayName = displayName;
        this.country = country;
    }

    public Player(UUID userUuid, String displayName, String country, int points) {
        this.userUuid = userUuid;
        this.displayName = displayName;
        this.country = country;
        this.points = points;
    }

    public Player(UUID userUuid, String displayName, String country, int points, long rank) {
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

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public long getRank() {
        return rank;
    }

    public void setRank(long rank) {
        this.rank = rank;
    }
}






