package com.gjg.leaderboard.util;

import com.github.javafaker.Faker;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.Random;

public class RandomPlayerCreationRequests {

    public static void main(String[] args) {

        Faker faker = new Faker();
        Random random = new Random();

        try {
            URL url = new URL("http://gjg-leaderboard.eu-central-1.elasticbeanstalk.com/user/create/score");

            for (int i = 0; i < 1000; i++) {

                String requestBody = randomPlayerJsonBody(random, faker);

                int responseCode = sendRequest(url, requestBody);

                System.out.println("# " + i + " Response Code: " + responseCode);

            }


        } catch (
                IOException e) {
            e.printStackTrace();
        }

    }

    private static int sendRequest(URL url, String requestBody) throws IOException {

        CloseableHttpClient hc = null;
        try {
            hc = new DefaultHttpClient();
            HttpPost con = new HttpPost(url.toString());

            con.setEntity(new StringEntity(requestBody));
            con.setHeader("Content-type", "application/json");
            HttpResponse resp = null;

            resp = hc.execute(con);
            return resp.getStatusLine().getStatusCode();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            hc.close();
        }

        return 0;
    }

    private static String randomPlayerJsonBody(Random random, Faker faker) {
        JSONObject player = new JSONObject();

        int randomPoints = random.nextInt(100000) + 1;
        String country;

        if (randomPoints % 3 == 1) {
            country = "tr";
        } else if (randomPoints % 3 == 2) {
            country = "uk";
        } else {
            country = "us";
        }

        player.put("displayName", faker.firstName());
        player.put("country", country);
        player.put("points", randomPoints);
        System.out.println(player.toString(4));

        return player.toString();
    }

}
