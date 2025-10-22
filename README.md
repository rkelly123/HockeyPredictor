# HockeyPredictor

Run the App with mvn spring-boot:run

The application runs on localhost:8080,
and you can view the local DB at http://localhost:8080/h2-console/ using JDBC URL jdbc:h2:file:./data/hockeypredictor-db

Call /api/update-data to fill the database with NHL team stats/analytics so far this season, as well as upcoming matchups that day.
Fetching the team stats takes a long time, so if you only want to update the matchups for the day, instead call /api/update-matchups.

Now, call /api/predict to get all of the predictions of the results of all the upcoming games that day.

This is a toy project to get more familiar with Springboot!