package com.gjg.leaderboard;


import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.InternalServerErrorException;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gjg.leaderboard.pojo.Player;
import com.gjg.leaderboard.pojo.PlayerItem;
import com.gjg.leaderboard.request.PlayerCreationRequestBody;
import com.gjg.leaderboard.request.PlayerCreationWithScoreRequestBody;
import com.gjg.leaderboard.request.ScoreSubmissionRequestBody;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
public class PlayerController {

    private BasicAWSCredentials awsCreds;
    private DynamoDB dynamoDB;
    final JedisPoolConfig jedisPoolConfig = buildPoolConfig();
    ObjectMapper objectMapper = new ObjectMapper();
    Table leaderboardTable;
    DynamoDBMapper dynamoDBMapper;
    private JedisPool jedisPool;

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
        this.jedisPool = new JedisPool(jedisPoolConfig, redisHost);
        this.leaderboardTable = dynamoDB.getTable(dynamoDbTableName);
        warpUpJedisPool();
    }

    /**
     * @param pageNum the number of page for pagination
     * @return List of players in the current page
     */
    @GetMapping("/leaderboard/page/{pageNum}")
    List<Player> getGlobalLeaderboard(@PathVariable int pageNum) {

        if (pageNum <= 0) throw new IllegalArgumentException("Invalid page number.");

        Set<String> pageUUIDs;

        try(Jedis jedis = jedisPool.getResource()){
            pageUUIDs = jedis.zrevrange(redisTableKey, (pageNum - 1) * 20, pageNum * 20 - 1);
        }

        List<Player> pageList = new ArrayList<>();

        for (String uuid : pageUUIDs) {
            pageList.add(getPlayer(uuid));
        }

        return pageList;

    }

    /**
     * @param countryCode country-iso-code for country specific leaderboard
     * @return List of players in the current page of country specific leaderboard
     * @see <a href="https://en.wikipedia.org/wiki/List_of_ISO_3166_country_codes">Country iso codes</a>
     */
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

    /**
     * @return The top 10 from global leaderboard
     */
    @GetMapping("/leaderboard/top10")
    List<Player> getLeaderboardTop10() {

        Set<String> top10UUIDs;

        try (Jedis jedis = jedisPool.getResource()) {
            top10UUIDs = jedis.zrevrange(redisTableKey, 0, 9);
        }

        List<Player> top10 = new ArrayList<>();

        for (String uuid : top10UUIDs) {
            top10.add(getPlayer(uuid));
        }

        return top10;

    }

    /**
     * Retrieve Existing Player
     * @param playerItem UUID of the player
     * @return Player
     */
    @GetMapping("/user/profile")
    Player getPlayer(@RequestBody PlayerItem playerItem) {

        UUID uuid = UUID.fromString(playerItem.getUserUuid());

        if (uuid == null) throw new IllegalArgumentException("Invalid uuid.");

        Player player = null;

        try (Jedis jedis = jedisPool.getResource()) {
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

    /**
     * Edit existing player info
     * @param updatedPlayerItem
     * @return PlayerItem
     */
    @PutMapping("/user/profile")
    PlayerItem editPlayer(@RequestBody PlayerItem updatedPlayerItem) {

        PlayerItem playerItem = dynamoDBMapper.load(PlayerItem.class, updatedPlayerItem.getUserUuid());
        playerItem.setDisplayName(updatedPlayerItem.getDisplayName());
        playerItem.setCountry(updatedPlayerItem.getCountry());
        dynamoDBMapper.save(playerItem);

        return dynamoDBMapper.load(PlayerItem.class, playerItem.getUserUuid());
    }

    /**
     * New Player Creation
     * @param playerCreationRequestBody
     * @return PlayerItem
     */
    @PostMapping("/user/create")
    PlayerItem createPlayer(@RequestBody PlayerCreationRequestBody playerCreationRequestBody) {
        PlayerItem playerItem = new PlayerItem(playerCreationRequestBody.getDisplayName(), playerCreationRequestBody.getCountry());
        dynamoDBMapper.save(playerItem);
        try(Jedis jedis = jedisPool.getResource()){
            jedis.zadd(redisTableKey, 0, playerItem.getUserUuid());
        }
        return dynamoDBMapper.load(PlayerItem.class, playerItem.getUserUuid());
    }

    /**
     * New Player Creation with scores
     * @param requestBody
     * @return PlayerItem
     */
    @PostMapping("/user/create/score")
    ResponseEntity<?> createPlayerWithScore(@RequestBody PlayerCreationWithScoreRequestBody requestBody) {
        PlayerItem playerItem = new PlayerItem(requestBody.getDisplayName(), requestBody.getCountry(), requestBody.getPoints());
        dynamoDBMapper.save(playerItem);
        try(Jedis jedis = jedisPool.getResource()){
            jedis.zadd(redisTableKey, playerItem.getPoints(), playerItem.getUserUuid());
        }
        return ResponseEntity.accepted().build();
    }

    /**
     * Score Submission
     * @param scoreSubmissionRequestBody a json object with UUID and double parameter
     */
    @PostMapping("/score/submit")
    ScoreSubmissionRequestBody submitScore(@RequestBody ScoreSubmissionRequestBody scoreSubmissionRequestBody) throws IllegalArgumentException {

        try (Jedis jedis = jedisPool.getResource()) {

            double scoreWorth = scoreSubmissionRequestBody.getScoreWorth();
            String uuid = scoreSubmissionRequestBody.getUuid().toString();

            if (jedis.exists(scoreSubmissionRequestBody.getUuid().toString())) {
                double currentTopScore = jedis.zscore(redisTableKey, scoreSubmissionRequestBody.getUuid().toString());
                if (scoreWorth > currentTopScore) {
                    UpdateItemSpec updateItemSpec = getUpdateItemScoreSpec(uuid, scoreWorth);
                    leaderboardTable.updateItem(updateItemSpec);
                    jedis.zadd(redisTableKey, scoreWorth, uuid);
                    scoreSubmissionRequestBody.setTimestamp(System.currentTimeMillis());

                    return scoreSubmissionRequestBody;

                } else throw new IllegalArgumentException("Current score is already higher.");
            }else {
                UpdateItemSpec updateItemSpec = getUpdateItemScoreSpec(uuid, scoreWorth);
                leaderboardTable.updateItem(updateItemSpec);
                jedis.zadd(redisTableKey, scoreWorth, uuid);
                scoreSubmissionRequestBody.setTimestamp(System.currentTimeMillis());

                return scoreSubmissionRequestBody;
            }

        } catch (Exception e) {
            handleQueryErrors(e);
        }

        return null;

    }

    /**
     * Delete existing player
     * @param playerItem
     * @return NoContent ResponseEntity
     */
    @DeleteMapping("/user/profile")
    ResponseEntity<?> deletePlayer(@RequestBody PlayerItem playerItem) {

        dynamoDBMapper.delete(playerItem);
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(playerItem.getUserUuid());
        }

        return ResponseEntity.noContent().build();
    }

    /**
     * Return player corresponding to uuid
     * @param uuidString String form of UUID of the player
     * @return Player
     */
    private Player getPlayer(String uuidString) {

        Player player = null;

        try (Jedis jedis = jedisPool.getResource()) {
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

    private ArrayList<Player> getCountryPlayerList(ItemCollection<QueryOutcome> items) throws JsonProcessingException {

        ArrayList<Player> players = new ArrayList<>();

        for (Item item : items) {
            JSONObject jsonItem = new JSONObject(item.toJSON());
            try (Jedis jedis = jedisPool.getResource()) {
                jsonItem.put("rank", Long.sum(jedis.zrevrank(redisTableKey, jsonItem.getString(dynamoDbHashKey)), 1));
            }
            players.add(objectMapper.readValue(jsonItem.toString(), Player.class));
        }

        return players;
    }

    private ItemCollection<QueryOutcome> getCountryItemCollectionFromDynamoDb(String countryCode) {

        Index index = leaderboardTable.getIndex(dynamoDbGsiName);

        QuerySpec querySpec = new QuerySpec()
                .withMaxPageSize(100)
                .withKeyConditionExpression("country = :countryCode")
                .withScanIndexForward(false)
                .withValueMap(new ValueMap()
                        .withString(":countryCode", countryCode));

        ItemCollection<QueryOutcome> items = index.query(querySpec);

        return items;
    }

    private static void handleCommonErrors(Exception exception) {
        try {
            throw exception;
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid Argument Error: " + e.getMessage());
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

    private static void handleQueryErrors(Exception exception) {
        try {
            throw exception;
        } catch (Exception e) {
            // There are no API specific errors to handle for Query, common DynamoDB API errors are handled below
            handleCommonErrors(e);
        }
    }

    private JedisPoolConfig buildPoolConfig() {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
        poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        return poolConfig;
    }

    private void warpUpJedisPool(){

        List<Jedis> minIdleJedisList = new ArrayList<Jedis>(jedisPoolConfig.getMinIdle());

        for (int i = 0; i < jedisPoolConfig.getMinIdle(); i++) {
            Jedis jedis = null;
            try {
                jedis = jedisPool.getResource();
                minIdleJedisList.add(jedis);
                jedis.ping();
            } catch (Exception e) {
                //log.error(e.getMessage(), e);
            }
        }

        for (int i = 0; i < jedisPoolConfig.getMinIdle(); i++) {
            Jedis jedis = null;
            try {
                jedis = minIdleJedisList.get(i);
                jedis.close();
            } catch (Exception e) {
                //logger.error(e.getMessage(), e);
            }
        }
    }

}