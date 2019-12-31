package com.gjg.leaderboard;


import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.InternalServerErrorException;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.pi.model.InvalidArgumentException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private BasicAWSCredentials awsCreds;
    private DynamoDB dynamoDB;
    Jedis jedis;
    ObjectMapper objectMapper = new ObjectMapper();
    Table leaderboardTable;
    DynamoDBMapper dynamoDBMapper;

    @Value("${redis.table.key}")
    private String redisTableKey;

    @Value("${dynamodb.primary.key}")
    private String dynamoDbHashKey;

    @Value("${dynamodb.gsi.name}")
    private String dynamoDbGsiName;

    PlayerController(@Value("${spring.redis.host}") String redisHost,
                     @Value("${dynamodb.table.name}") String dynamoDbTableName,
                     @Value("${aws.region}") String awsRegion,
                     @Value("${aws.accessKey}") String awsAccessKey,
                     @Value("${aws.secretKey}") String awsSecretKey) {

        this.awsCreds = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withRegion(awsRegion).
                withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
        this.dynamoDB = new DynamoDB(client);
        this.dynamoDBMapper = new DynamoDBMapper(client);
        this.jedis = new Jedis(redisHost);
        this.leaderboardTable = dynamoDB.getTable(dynamoDbTableName);
    }

    @GetMapping("/leaderboard")
    List<Player> getGlobalLeaderboard(@RequestBody int page) {

        if (page <= 0) throw new InvalidArgumentException("Invalid page number.");

        Set<String> pageUUIDs = jedis.zrevrange(redisTableKey, (page - 1) * 10, page * 10 - 1);
        List<Player> pageList = new ArrayList<>();

        for (String uuid : pageUUIDs) {
            pageList.add(getPlayer(uuid));
        }

        return pageList;

    }

    @GetMapping("/leaderboard/top10")
    List<Player> getLeaderboardTop10() {

        Set<String> top10UUIDs = jedis.zrevrange(redisTableKey, 0, 9);
        List<Player> top10 = new ArrayList<>();

        for (String uuid : top10UUIDs) {
            top10.add(getPlayer(uuid));
        }

        return top10;

    }

    @GetMapping("/leaderboard/{countryCode}")
    List<Player> getCountryLeaderboard(@PathVariable String countryCode) {

        if (countryCode == null || countryCode.length() == 0) {
            throw new IllegalArgumentException("Invalid Country Code");
        }

        ArrayList<Player> players = new ArrayList<>();

        try {
            ItemCollection<QueryOutcome> items = getCountryItemCollectionFromDynamoDb(countryCode);
            players = getCountryPlayerList(items);

            // Handle query errors
        } catch (Exception e) {
            handleQueryErrors(e);
        }

        return players;
    }

    @PostMapping("/score/submit")
    ScoreSubmissionRequestBody submitScore(@RequestBody ScoreSubmissionRequestBody scoreSubmissionRequestBody) throws InvalidArgumentException {

        try {

            double scoreWorth = scoreSubmissionRequestBody.getScoreWorth();
            String uuid = scoreSubmissionRequestBody.getUuid().toString();
            double currentTopScore = jedis.zscore(redisTableKey, scoreSubmissionRequestBody.getUuid().toString());

            if (scoreWorth > currentTopScore) {

                UpdateItemSpec updateItemSpec = getUpdateItemScoreSpec(uuid, scoreWorth);

                leaderboardTable.updateItem(updateItemSpec);
                jedis.zadd(redisTableKey, scoreWorth, uuid);
                scoreSubmissionRequestBody.setTimestamp(System.currentTimeMillis());

                return scoreSubmissionRequestBody;

            } else throw new InvalidArgumentException("Current score is already higher.");

        } catch (Exception e) {
            handleQueryErrors(e);
        }

        return null;

    }

    @GetMapping("/user/profile/{id}")
    Player getPlayer(@PathVariable UUID uuid) {

        if (uuid == null) throw new InvalidArgumentException("Invalid uuid.");

        Player player = null;

        try {
            Item item = leaderboardTable.getItem(dynamoDbHashKey, uuid.toString());
            JSONObject jsonItem = new JSONObject(item.toJSON());
            jsonItem.put("rank", Long.sum(jedis.zrevrank(redisTableKey, uuid.toString()), 1));

            player = objectMapper.readValue(jsonItem.toString(), Player.class);

        } catch (JsonProcessingException | IllegalArgumentException e) {
            // uuid string is wrong
            e.printStackTrace();
        }

        return player;
    }

    @PostMapping("/user/create")
    PlayerItem createPlayer(@RequestBody PlayerCreationRequestBody requestBody) {

        // No need to add newly created user to redis
        // Add user to redis when s/he submits a score

        PlayerItem playerItem = new PlayerItem(requestBody.getDisplayName(), requestBody.getCountry());
        dynamoDBMapper.save(playerItem);

        return dynamoDBMapper.load(PlayerItem.class, playerItem.getUserUuid());
    }

    @PutMapping("/user/profile")
    PlayerItem editPlayer(@RequestBody PlayerItem updatedPlayerItem) {

        PlayerItem playerItem = dynamoDBMapper.load(PlayerItem.class, updatedPlayerItem.getUserUuid());
        playerItem.setDisplayName(updatedPlayerItem.getDisplayName());
        playerItem.setCountry(updatedPlayerItem.getCountry());
        dynamoDBMapper.save(playerItem);

        return dynamoDBMapper.load(PlayerItem.class, playerItem.getUserUuid());
    }

    @DeleteMapping("/user/profile/delete")
    ResponseEntity<?> deletePlayer(@RequestBody PlayerItem playerItem) {

        dynamoDBMapper.delete(playerItem);
        jedis.del(playerItem.getUserUuid());

        return ResponseEntity.noContent().build();
    }

    private Player getPlayer(String uuidString) {

        Player player = null;

        try {
            UUID uuid = UUID.fromString(uuidString);

            Item item = leaderboardTable.getItem(dynamoDbHashKey, uuid.toString());
            JSONObject jsonItem = new JSONObject(item.toJSON());
            jsonItem.put("rank", Long.sum(jedis.zrevrank(redisTableKey, uuid.toString()), 1));

            player = objectMapper.readValue(jsonItem.toString(), Player.class);

        } catch (JsonProcessingException | IllegalArgumentException e) {
            // uuid string is wrong
            e.printStackTrace();
        }

        return player;
    }

    private UpdateItemSpec getUpdateItemScoreSpec(String uuid, double scoreWorth) {

        UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withPrimaryKey(dynamoDbHashKey, uuid)
                .withUpdateExpression("SET points = :newPoints")
                .withValueMap(new ValueMap()
                        .withNumber(":newPoints", scoreWorth));

        return updateItemSpec;
    }

    private PutItemSpec getPutItemSpec(String displayName, String countryCode) {

        return null;
    }

    private ItemCollection<QueryOutcome> getCountryItemCollectionFromDynamoDb(String countryCode) {

        Index index = leaderboardTable.getIndex(dynamoDbGsiName);

        QuerySpec querySpec = new QuerySpec()
                .withMaxPageSize(10)
                .withKeyConditionExpression("country = :countryCode")
                .withScanIndexForward(false)
                .withValueMap(new ValueMap()
                        .withString(":countryCode", countryCode));

        ItemCollection<QueryOutcome> items = index.query(querySpec);

        return items;
    }

    private ArrayList<Player> getCountryPlayerList(ItemCollection<QueryOutcome> items) throws JsonProcessingException {

        ArrayList<Player> players = new ArrayList<>();

        for (Item item : items) {
            JSONObject jsonItem = new JSONObject(item.toJSON());
            jsonItem.put("rank", Long.sum(jedis.zrevrank(redisTableKey, jsonItem.getString(dynamoDbHashKey)), 1));
            players.add(objectMapper.readValue(jsonItem.toString(), Player.class));
        }

        return players;
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
        } catch (InvalidArgumentException e) {
            System.out.println("Invalid Argument Error: " + e.getErrorMessage());
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