package com.gjg.leaderboard;


import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.InternalServerErrorException;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
class PlayerController {

    private final PlayerRepository repository;
    private BasicAWSCredentials awsCreds;
    private DynamoDB dynamoDB;
    Jedis jedis;
    ObjectMapper objectMapper = new ObjectMapper();
    Table leaderboardTable;

    @Value("${redis.key}")
    private String redisKey;

    PlayerController(PlayerRepository repository) {
        this.repository = repository;
        this.awsCreds = new BasicAWSCredentials("*", "*");
        this.dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.standard().withRegion("eu-central-1").
                withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build());
        this.jedis = new Jedis("global-leaderboard.i2obg1.0001.euc1.cache.amazonaws.com");
        this.leaderboardTable = dynamoDB.getTable("leaderboard");
    }

    // Aggregate root

    @GetMapping("/leaderboard")
    List<Player> globalLeaderboard() {


        return null;
    }

    @GetMapping("/leaderboard/top10")
    List<Player> globalRedisLeaderboard() {

        Set<String> top10UUIDs = jedis.zrevrange(redisKey, 0, 9);
        List<Player> top10 = new ArrayList<>();

        for (String uuid : top10UUIDs) {
            top10.add(getPlayerFromDynamoDb(uuid));
        }

        return top10;

    }

    @GetMapping("/leaderboard/{countryCode}")
    List<Player> countryLeaderboard(@PathVariable String countryCode) {

        try {

            ArrayList<Player> players = new ArrayList<>();

            Index index = leaderboardTable.getIndex("country-points-index");

            QuerySpec querySpec = new QuerySpec()
                    .withMaxPageSize(10)
                    .withKeyConditionExpression("country = :countryCode")
                    .withScanIndexForward(false)
                    .withValueMap(new ValueMap()
                            .withString(":countryCode", countryCode));

            ItemCollection<QueryOutcome> items = index.query(querySpec);

            JSONArray jsonArray = new JSONArray();

            for (Item item : items) {
                JSONObject jsonItem = new JSONObject(item.toJSON());
                jsonItem.put("rank", Long.sum(jedis.zrevrank(redisKey, jsonItem.getString("userUuid")), 1));
                jsonArray.put(jsonItem);
                players.add(objectMapper.readValue(jsonItem.toString(), Player.class));
            }

//            System.out.println("Query successful.");
//            System.out.println(jsonArray.toString(4));
            return players;

            // Handle query errors
        } catch (Exception e) {
            handleQueryErrors(e);
        }

        return null;
    }


    @PostMapping("/score/submit")
    ScoreSubmission newScoreSubmission(@RequestBody ScoreSubmission scoreSubmission) {
        //todo update score if higher than last one

        try {

            double scoreWorth = scoreSubmission.getScoreWorth();
            String uuid = scoreSubmission.getUuid().toString();
            double currentTopScore = jedis.zscore(redisKey, scoreSubmission.getUuid().toString());

            if (scoreWorth > currentTopScore) {

                UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                        .withPrimaryKey("userUuid", uuid)
                        .withUpdateExpression("SET points = :newPoints")
                        .withValueMap(new ValueMap()
                                .withNumber(":newPoints", scoreWorth));

                leaderboardTable.updateItem(updateItemSpec);

                jedis.zadd(redisKey, scoreWorth, uuid);

                scoreSubmission.setTimestamp(System.currentTimeMillis());
                return scoreSubmission;

            }
        } catch (Exception e) {
            handleQueryErrors(e);
        }


        return null;
    }

    // Single item
    @GetMapping("/user/profile/{id}")
    Player getPlayer(@PathVariable UUID uuid) {

        /*QuerySpec querySpec = new QuerySpec()
                .withKeyConditionExpression("userUuid = :uuid")
                .withScanIndexForward(false)
                .withValueMap(new ValueMap()
                        .withString(":uuid", uuid.toString()));*/
        try {


            Item item = leaderboardTable.getItem("userUuid", uuid.toString());
            JSONObject jsonItem = new JSONObject(item.toJSON());
            jsonItem.put("rank", Long.sum(jedis.zrevrank(redisKey, uuid.toString()), 1));

            Player player = objectMapper.readValue(jsonItem.toString(), Player.class);

            return player;

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return null;
    }


    @PutMapping("/user/profile/{id}")
    Player editPlayer(@RequestBody Player player, @PathVariable UUID id) {
        //todo update display name and country ??
        return repository.findById(id)
                .map(player1 -> {
                    player1.setDisplayName(player.getDisplayName());
                    player1.setCountry(player.getCountry());
                    return repository.save(player1);
                })
                .orElseGet(() -> {
                    player.setUserUuid(id);
                    return repository.save(player);
                });
    }

    @DeleteMapping("/user/profile/delete")
    ResponseEntity<?> deletePlayer(@RequestBody Player player) {
        repository.deleteById(player.getUserUuid());
        return ResponseEntity.noContent().build();
    }

    private Player getPlayerFromDynamoDb(UUID uuid) {

        Player player = null;

        try {
            Item item = leaderboardTable.getItem("userUuid", uuid.toString());
            JSONObject jsonItem = new JSONObject(item.toJSON());
            jsonItem.put("rank", Long.sum(jedis.zrevrank(redisKey, uuid.toString()), 1));

            player = objectMapper.readValue(jsonItem.toString(), Player.class);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return player;
    }

    private Player getPlayerFromDynamoDb(String uuidString) {

        Player player = null;

        try {
            UUID uuid = UUID.fromString(uuidString);

            Item item = leaderboardTable.getItem("userUuid", uuid.toString());
            JSONObject jsonItem = new JSONObject(item.toJSON());
            jsonItem.put("rank", Long.sum(jedis.zrevrank(redisKey, uuid.toString()), 1));

            player = objectMapper.readValue(jsonItem.toString(), Player.class);

        } catch (JsonProcessingException | IllegalArgumentException e) {
            // uuid string is wrong
            e.printStackTrace();
        }



        return player;
    }

    private static QuerySpec createQueryRequest(String countryCode) {

        QuerySpec querySpec = new QuerySpec()
                .withMaxPageSize(10)
                .withKeyConditionExpression("country = :countryCode")
                .withScanIndexForward(false)
                .withValueMap(new ValueMap()
                        .withString("country", countryCode));

        return querySpec;

    }


    private static void handleQueryErrors(Exception exception) {
        try {
            throw exception;
        } catch (Exception e) {
            // There are no API specific errors to handle for Query, common DynamoDB API errors are handled below
            handleCommonErrors(e);
        }
    }

    private static void handleCommonErrors(Exception exception) {
        try {
            throw exception;
        } catch (InternalServerErrorException isee) {
            System.out.println("Internal Server Error, generally safe to retry with exponential back-off. Error: " + isee.getErrorMessage());
        } catch (ProvisionedThroughputExceededException ptee) {
            System.out.println("Request rate is too high. If you're using a custom retry strategy make sure to retry with exponential back-off. " +
                    "Otherwise consider reducing frequency of requests or increasing provisioned capacity for your table or secondary index. Error: " +
                    ptee.getErrorMessage());
        } catch (ResourceNotFoundException rnfe) {
            System.out.println("One of the tables was not found, verify table exists before retrying. Error: " + rnfe.getErrorMessage());
        } catch (AmazonServiceException ase) {
            System.out.println("An AmazonServiceException occurred, indicates that the request was correctly transmitted to the DynamoDB " +
                    "service, but for some reason, the service was not able to process it, and returned an error response instead. Investigate and " +
                    "configure retry strategy. Error type: " + ase.getErrorType() + ". Error message: " + ase.getErrorMessage());
        } catch (AmazonClientException ace) {
            System.out.println("An AmazonClientException occurred, indicates that the client was unable to get a response from DynamoDB " +
                    "service, or the client was unable to parse the response from the service. Investigate and configure retry strategy. " +
                    "Error: " + ace.getMessage());
        } catch (Exception e) {
            System.out.println("An exception occurred, investigate and configure retry strategy. Error: " + e.getMessage());
        }
    }

}