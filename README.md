# simple-leaderboard
A simple Spring Application for game leaderboard using Redis and DynomoDB on AWS.

![](https://github.com/SmdBcr/simple-leaderboard/workflows/.github/workflows/maven.yml/badge.svg)


**Notes**

1. Global Leaderboard - GET /leaderboard
* http://gjg-leaderboard.eu-central-1.elasticbeanstalk.com/leaderboard/{page}
* {page} --> {1, 2, 3, 4, 5 ... N} for pagination
  
---

2. Global Leaderboard Top 10 - GET /leaderboard/top10
* http://gjg-leaderboard.eu-central-1.elasticbeanstalk.com/leaderboard/top10
  
---

3. Country Specific Leaderboard - GET /leaderboard/{country_iso_code} 
* http://gjg-leaderboard.eu-central-1.elasticbeanstalk.com/leaderboard/tr
   
---

4. Score Submission - POST /score/submit 
* http://gjg-leaderboard.eu-central-1.elasticbeanstalk.com/leaderboard/score/submit
  * **example request json** 

    {
        "userUuid" : "1e673707-8380-42bd-a798-ccb29bff834c",
        "scoreWorth" : 999
    }

---

5. Create New User - POST /user/create
* http://gjg-leaderboard.eu-central-1.elasticbeanstalk.com/leaderboard/user/create
  * **example request json** 

    {
        "displayName" : "Sam",
        "country" : "uk"
    }

---

6. Retrieve User Profile - GET /user/profile/
* http://gjg-leaderboard.eu-central-1.elasticbeanstalk.com/leaderboard/user/profile
  * **example request json** 

    {
        "userUuid" : "1e673707-8380-42bd-a798-ccb29bff834c"
    }

---

7. Edit User - PUT /user/profile
* http://gjg-leaderboard.eu-central-1.elasticbeanstalk.com/leaderboard/user/profile
  * **example request json** 

    {
        "userUuid" : "1e673707-8380-42bd-a798-ccb29bff834c",
        "displayName" : "Sam",
        "country" : "uk"
    }

---

8. Edit User - DELETE /user/profile
* http://gjg-leaderboard.eu-central-1.elasticbeanstalk.com/leaderboard/user/profile
  * **example request json** 

    {
        "userUuid" : "1e673707-8380-42bd-a798-ccb29bff834c",
    }


