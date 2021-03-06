# simple-leaderboard
A simple Spring Application for game leaderboard using Redis and DynomoDB on AWS.

  ***Application is deployed on Elastic Beanstalk***
  
  ***Global Leaderboard is kept on Redis as a sorted set (uuid, points)***

  ***Player attributes such as country and name are kept on DynamoDB***

  ***For country specific leaderbaords, global secondary index is used (pk=country, sk=points)***

![](https://github.com/SmdBcr/simple-leaderboard/workflows/.github/workflows/maven.yml/badge.svg)


**Notes**

1. Global Leaderboard - GET /leaderboard/page/{page}
* http://gjg-leaderboard.eu-central-1.elasticbeanstalk.com/leaderboard/{page}
  * {page} --> {1, 2, 3, 4, 5 ... N} for pagination
  * Each page has 20 players
  
---

2. Global Leaderboard Top 10 - GET /leaderboard/top10
* http://gjg-leaderboard.eu-central-1.elasticbeanstalk.com/leaderboard/top10
  
---

3.  Country Specific Leaderboard - GET /leaderboard/{country_iso_code} 
* http://gjg-leaderboard.eu-central-1.elasticbeanstalk.com/leaderboard/tr
  * Returns top 100 players from the specifiec country leaderboard
---

4.  Country Specific Leaderboard - GET /leaderboard/{country_iso_code}/{size}
* http://gjg-leaderboard.eu-central-1.elasticbeanstalk.com/leaderboard/tr/200
  * Returns top {size} players from the specifiec country leaderboard
---

5. Score Submission - POST /score/submit 
* http://gjg-leaderboard.eu-central-1.elasticbeanstalk.com/leaderboard/score/submit
  * **example request json** 

    {
        "userUuid" : "1e673707-8380-42bd-a798-ccb29bff834c",
        "scoreWorth" : 999
    }

---

6. Create New User - POST /user/create
* http://gjg-leaderboard.eu-central-1.elasticbeanstalk.com/leaderboard/user/create
  * **example request json** 

    {
        "displayName" : "Sam",
        "country" : "uk"
    }

---

7. Create New User with a Score - POST /user/create/score
* http://gjg-leaderboard.eu-central-1.elasticbeanstalk.com/leaderboard/user/create
  * **example request json** 

    {
        "displayName" : "Sam",
        "country" : "uk"
        "points" : 34450
    }

---

8. Retrieve User Profile - GET /user/profile/
* http://gjg-leaderboard.eu-central-1.elasticbeanstalk.com/leaderboard/user/profile
  * **example request json** 

    {
        "userUuid" : "1e673707-8380-42bd-a798-ccb29bff834c"
    }

---

9. Edit User - PUT /user/profile
* http://gjg-leaderboard.eu-central-1.elasticbeanstalk.com/leaderboard/user/profile
  * **example request json** 

    {
        "userUuid" : "1e673707-8380-42bd-a798-ccb29bff834c",
        "displayName" : "Sam",
        "country" : "uk"
    }

---

10. Delete User - DELETE /user/profile
* http://gjg-leaderboard.eu-central-1.elasticbeanstalk.com/leaderboard/user/profile
  * **example request json** 

    {
        "userUuid" : "1e673707-8380-42bd-a798-ccb29bff834c",
    }


## Some Test Results from Apache JMeter


| Samples    | AVG | Median | Min | Max | Throughput (/sec)|  Leaderboard  |
|------------|-----|--------|-----|-----|------------------|---------------|
|  50 * 20   | 287 |  296   | 151 | 467 |       60         | Global page 1 |
|  100 * 20  | 433 |  454   | 155 | 837 |       103        | Global page 1 |
|------------|-----|--------|-----|-----|------------------|---------------|
|  50 * 20   | 128 |  118   | 93  | 231 |       75         | Top 10        |
|  100 * 20  | 132 |  128   | 92  | 308 |       110        | Top 10        |
|------------|-----|--------|-----|-----|------------------|---------------|
|  50 * 20   | 119 |  110   | 98  | 218 |       76         | TR 20 players |
|  100 * 20  | 120 |  112   | 99  | 224 |       153        | TR 20 players |
|------------|-----|--------|-----|-----|------------------|---------------|
|  50 * 20   | 350 |  349   | 491 | 556 |       56         | TR 100 players|
|  100 * 20  | 685 |  704   | 500 | 1417|       91         | TR 100 players|

- 50*20 -->  50 Threads, each sends 20 requests
- 100*20 --> 100 Threads, each sends 20 requests
