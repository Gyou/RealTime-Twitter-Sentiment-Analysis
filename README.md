# RealTime-Twitter-Sentiment-Analysis

This is a joint work with Xingyu Chen.

This question asks us to streaming the tweets of a specific topic from Twitter, and then do sentiment analysis.
Please follow the steps here:
1. First we need to run TwitterStreamToLocal.scala , this class will stream the tweets of a specific topic to local file folder (./josntweet/tweets/). Tweets will be stored as .json files. we have already show some example in that folder. You can have a look.
To run  TwitterStreamToLocal.scala, you should specify the running parameters in this order:

ConsumerKey
ConsumerSecret
accessToken
accessTokenSecret
TopicKeyWords

In this project, we use the TopicKeyWords "Trump" to analyze tweets related to Trump.

2. Let the TwitterStreamToLocal.scala run, now lets go to StreamDFSentimentAnalysisToKafka.scala .
This requires you to first start your local kafka service, because we are going to analyze the locally stored tweets. One can easily combine StreamDFSentimentAnalysisToKafka.scala with TwitterStreamToLocal.scala to let these things done in one class. Here we split the functionalities to make things clear.
After starting your local kafka service, please make sure small_sentiment_data.csv is at the root directory.
At the end of StreamDFSentimentAnalysisToKafka.scala , you can choose which topic of kafka you would like to store the sentiment results in.
Then just run this code. It will continuously output the sentiment analysis data to a kafka topic, say "TweetTopic". Each line in  "TweetTopic" will be json format with two fields: Original_Tweet, final_sentiment.

3. Now we need to visualized the data stored in kafka "TweetTopic". First start Eleastic, then Kibana.
Before we start Logstash, we need to create a configuration file logstashHW3.conf under <Logshtash root directory>/config/ folder .
the contents of the logstashHW3.conf will be:
```
input { kafka { bootstrap_servers => "localhost:9092"
 topics => ["TweetTopic"]
codec => json {charset => "UTF-8"}
} }
output { elasticsearch { hosts => ["localhost:9200"] index => "cs6350hw3index" } }
```
Then go into <Logshtash root directory>/bin/ and run
```
logstash -f  config/logstashHW3.conf
```
or
```
./logstash -f  config/logstashHW3.conf 
```
for linux like systems.

Then you can go to http://localhost:5601 to start whatever visualizations you like.
