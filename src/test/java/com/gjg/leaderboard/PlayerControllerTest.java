package com.gjg.leaderboard;

import com.gjg.leaderboard.pojo.Player;
import com.gjg.leaderboard.pojo.PlayerItem;
import com.gjg.leaderboard.request.PlayerCreationRequestBody;
import com.gjg.leaderboard.request.ScoreSubmissionRequestBody;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerControllerTest {

    private static PlayerController playerController;

    @BeforeAll
    static void setUp() {
        playerController = new PlayerController("localhost",
                "test",
                "localhost",
                "accessKey",
                "secretKey");
    }

    @Test
    void getGlobalLeaderboardThrowsInvalidArgumentExceptionWithInvalidParam() {
        assertThrows(IllegalArgumentException.class, () -> {
            List<Player> players = playerController.getGlobalLeaderboard(-1);
        });
    }

    @Test
    void getCountryLeaderboardThrowsInvalidArgumentExceptionWithInvalidParam() {
        assertThrows(IllegalArgumentException.class, () -> {
            List<Player> players = playerController.getCountryLeaderboard(null);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            List<Player> players = playerController.getCountryLeaderboard("");
        });
    }

    @Test
    void submitScoreThrowsInvalidArgumentExceptionWithInvalidParam() {
        assertThrows(IllegalArgumentException.class, () -> {
            ScoreSubmissionRequestBody body = playerController.submitScore(new ScoreSubmissionRequestBody(null, 123));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            ScoreSubmissionRequestBody body = playerController.submitScore(new ScoreSubmissionRequestBody(UUID.randomUUID(), -5));
        });
    }

    @Test
    void getPlayerThrowsInvalidArgumentExceptionWithInvalidParam() {
        assertThrows(IllegalArgumentException.class, () -> {
            Player player = playerController.getPlayer(new PlayerItem(null, null));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            Player player = playerController.getPlayer(new PlayerItem("name", null));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            Player player = playerController.getPlayer(new PlayerItem(null, "tr"));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            Player player = playerController.getPlayer(new PlayerItem("", null));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            Player player = playerController.getPlayer(new PlayerItem(null, ""));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            Player player = playerController.getPlayer(new PlayerItem("", ""));
        });
    }

    @Test
    void createPlayerThrowsInvalidArgumentExceptionWithInvalidParam() {
        assertThrows(IllegalArgumentException.class, () -> {
            PlayerItem playerItem = playerController.createPlayer(new PlayerCreationRequestBody(null, null));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            PlayerItem playerItem = playerController.createPlayer(new PlayerCreationRequestBody("name", null));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            PlayerItem playerItem = playerController.createPlayer(new PlayerCreationRequestBody(null, "tr"));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            PlayerItem playerItem = playerController.createPlayer(new PlayerCreationRequestBody("", null));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            PlayerItem playerItem = playerController.createPlayer(new PlayerCreationRequestBody(null, ""));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            PlayerItem playerItem = playerController.createPlayer(new PlayerCreationRequestBody("", ""));
        });
    }

    @Test
    void editPlayerThrowsInvalidArgumentExceptionWithInvalidParam() {
        assertThrows(IllegalArgumentException.class, () -> {
            PlayerItem playerItem = playerController.editPlayer(new PlayerItem(null, null));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            PlayerItem playerItem = playerController.editPlayer(new PlayerItem("name", null));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            PlayerItem playerItem = playerController.editPlayer(new PlayerItem(null, "tr"));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            PlayerItem playerItem = playerController.editPlayer(new PlayerItem("", null));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            PlayerItem playerItem = playerController.editPlayer(new PlayerItem(null, ""));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            PlayerItem playerItem = playerController.editPlayer(new PlayerItem("", ""));
        });
    }

    @Test
    void deletePlayerThrowsInvalidArgumentExceptionWithInvalidParam() {
        assertThrows(IllegalArgumentException.class, () -> {
            PlayerItem playerItem = playerController.editPlayer(new PlayerItem(null, null));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            PlayerItem playerItem = playerController.editPlayer(new PlayerItem("name", null));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            PlayerItem playerItem = playerController.editPlayer(new PlayerItem(null, "tr"));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            PlayerItem playerItem = playerController.editPlayer(new PlayerItem("", null));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            PlayerItem playerItem = playerController.editPlayer(new PlayerItem(null, ""));
        });
        assertThrows(IllegalArgumentException.class, () -> {
            PlayerItem playerItem = playerController.editPlayer(new PlayerItem("", ""));
        });
    }

}