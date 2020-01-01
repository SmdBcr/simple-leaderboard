package com.gjg.leaderboard.request;


import lombok.Data;

@Data
public class PlayerCreationWithScoreRequestBody {

    private String displayName;
    private String country;
    private double points;

    public PlayerCreationWithScoreRequestBody(String displayName, String country, double points) {

        if (displayName == null || displayName.length() == 0 ||
                country == null || country.length() == 0)
            throw new IllegalArgumentException("Invalid name or country.");

        this.displayName = displayName;
        this.country = country;
        this.points = points;
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
}