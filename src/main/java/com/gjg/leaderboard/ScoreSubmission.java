package com.gjg.leaderboard;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.UUID;


@Data
@Entity
public class ScoreSubmission {
    private @Id UUID uuid ;
    private int scoreWorth;
    private long timestamp;
    ScoreSubmission() {}

    public ScoreSubmission(UUID uuid, int scoreWorth) {
        this.uuid = uuid;
        this.scoreWorth = scoreWorth;
        this.timestamp = System.currentTimeMillis();
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public int getScoreWorth() {
        return scoreWorth;
    }

    public void setScoreWorth(int scoreWorth) {
        this.scoreWorth = scoreWorth;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}






