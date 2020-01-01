package com.gjg.leaderboard.util;

import java.util.UUID;

public class PlayerNotFoundException extends RuntimeException {
    public PlayerNotFoundException(UUID id) {
        super("There is no registered player with id: " + id);
    }
}
