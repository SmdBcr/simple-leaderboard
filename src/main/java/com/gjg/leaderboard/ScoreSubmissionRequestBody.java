package com.gjg.leaderboard;

import com.amazonaws.services.pi.model.InvalidArgumentException;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.UUID;


@Entity
public class ScoreSubmissionRequestBody {
    private @Id
    UUID uuid;
    private int scoreWorth;
    private long timestamp;

    ScoreSubmissionRequestBody() {
    }

    public ScoreSubmissionRequestBody(UUID uuid, int scoreWorth) {

        if (uuid == null || scoreWorth <= 0)
            throw new InvalidArgumentException("Invalid score submission");

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






