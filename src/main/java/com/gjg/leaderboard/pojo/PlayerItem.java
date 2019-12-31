package com.gjg.leaderboard.pojo;


import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import javax.persistence.Id;
import java.util.UUID;

//@Data
//@Entity
@DynamoDBTable(tableName = "leaderboard")
public class PlayerItem {

    private @Id
    UUID userUuid;
    private String displayName;
    private double points;
    private String country;

    public PlayerItem() {
    }

    public PlayerItem(String displayName, String country) {

        if (displayName == null || displayName.length() == 0 ||
                country == null || country.length() == 0)
            throw new IllegalArgumentException("Invalid name or country.");

        this.userUuid = UUID.randomUUID();
        this.displayName = displayName;
        this.country = country;
        this.points = 0;
    }

    public PlayerItem(String displayName, String country, double points) {

        if (displayName == null || displayName.length() == 0 ||
                country == null || country.length() == 0 ||
                points < 0)
            throw new IllegalArgumentException("Invalid name or country.");

        this.userUuid = UUID.randomUUID();
        this.displayName = displayName;
        this.country = country;
        this.points = points;
    }

    @DynamoDBHashKey(attributeName = "userUuid")
    public String getUserUuid() {
        return userUuid.toString();
    }

    public void setUserUuid(UUID userUuid) {
        if (userUuid == null)
            throw new IllegalArgumentException("Invalid uuid.");
        this.userUuid = userUuid;
    }

    public void setUserUuid(String userUuid) {
        if (userUuid == null || userUuid.length() == 0)
            throw new IllegalArgumentException("Invalid uuid.");
        this.userUuid = UUID.fromString(userUuid);
    }

    @DynamoDBAttribute(attributeName = "displayName")
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @DynamoDBAttribute(attributeName = "country")
    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    @DynamoDBAttribute(attributeName = "points")
    public double getPoints() {
        return points;
    }

    public void setPoints(double points) {
        this.points = points;
    }

}