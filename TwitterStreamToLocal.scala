
import com.johnsnowlabs.nlp.annotator._
import com.johnsnowlabs.nlp.base._
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.Seconds
import twitter4j.conf.ConfigurationBuilder
import twitter4j.auth.OAuthAuthorization
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.ml.{Pipeline}
import org.apache.spark.sql.{ SaveMode, SparkSession}




object TwitterStreamToLocal {
  def main(args: Array[String]) {
    if (args.length < 4) {
      System.err.println("Usage: <ConsumerKey><ConsumerSecret><accessToken><accessTokenSecret>" +
        "[<filters>]")

      System.exit(1)
    }




    val appName = "TwitterData"
    val conf = new SparkConf()
    conf.setAppName(appName).setMaster("local[2]")
    conf.set("spark.driver.allowMultipleContexts", "true")
    val sc = new SparkContext(conf)

    val spark = new SparkSession.Builder().config(conf).getOrCreate()
    import spark.implicits._
    val ssc = new StreamingContext(spark.sparkContext, Seconds(20))

    val Array(consumerKey, consumerSecret, accessToken, accessTokenSecret) = args.take(4)

    val filters = args.takeRight(args.length - 4)
    val cb = new ConfigurationBuilder

    // can use them to generate OAuth credentials
    System.setProperty("twitter4j.oauth.consumerKey", consumerKey)
    System.setProperty("twitter4j.oauth.consumerSecret", consumerSecret)
    System.setProperty("twitter4j.oauth.accessToken", accessToken)
    System.setProperty("twitter4j.oauth.accessTokenSecret", accessTokenSecret)

    val auth = new OAuthAuthorization(cb.build)
    val tweets = TwitterUtils.createStream(ssc, None, filters)

    val eng_tweets = tweets.filter(_.getLang == "en")
    val statuses = eng_tweets.map(status => status.getText())

    statuses.print()
    statuses.foreachRDD( rdd => {
      val stream = rdd.toDF("Original_Tweet")
      stream.write.mode(SaveMode.Append).json("./jsontweet/tweets/")

    })

    //streamed_sentiment_DF.show()


    ssc.start()
    ssc.awaitTermination()
  }

}


