package com.gjg.leaderboard;


import lombok.Data;

@Data
public class PlayerCreationRequestBody {

    private String displayName;
    private String country;

    PlayerCreationRequestBody(String displayName, String country) {
        this.displayName = displayName;
        this.country = country;
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

}






