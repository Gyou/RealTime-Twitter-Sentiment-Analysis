
import com.johnsnowlabs.nlp.annotator._
import com.johnsnowlabs.nlp.base._
import org.apache.spark.{SparkConf}

import org.apache.spark.ml.{Pipeline}

import org.apache.spark.sql.types.{StructType}
import org.apache.spark.sql.{SparkSession}

import org.apache.spark.sql.functions._

object StreamDFSentimentAnalysisToKafka {

  def main(args: Array[String]) {

    val appName = "TwitterSentimentDF"
    val conf = new SparkConf()
    conf.setAppName(appName).setMaster("local[3]")
    conf.set("spark.driver.allowMultipleContexts", "true")

    val spark = new SparkSession.Builder().config(conf).getOrCreate()
    // Read all the csv files written atomically in a directory
    val userSchema = new StructType().add("Original_Tweet", "string")


    val textDF = spark.readStream.format("json").schema(userSchema).json("./jsontweet/tweets/*.json")

    //textDF.show()


    //train a sentiment analysis model using own data
    val training = spark.read.option("header", "false").option("inferSchema", "true").csv("small_sentiment_data.csv").toDF("train_sentiment", "train_text")


    //build a pipeline
    val document = new DocumentAssembler()
      .setInputCol("train_text")
      .setOutputCol("document")

    val token = new Tokenizer()
      .setInputCols("document")
      .setOutputCol("token")

    val remover = StopWordsCleaner.pretrained("stopwords_en", "en")
      .setInputCols(Array("token"))
      .setOutputCol("cleanTokens")

    val normalizer = new Normalizer()
      .setInputCols("cleanTokens")
      .setOutputCol("normal")

    val vivekn = new ViveknSentimentApproach()
      .setInputCols("document", "normal")
      .setOutputCol("result_sentiment")
      .setSentimentCol("train_sentiment")

    val finisher = new Finisher()
      .setInputCols("result_sentiment")
      .setOutputCols("final_sentiment")

    val pipeline = new Pipeline().setStages(Array(document, token,remover, normalizer, vivekn, finisher))

    val sparkPipeline = pipeline.fit(training)



    document.setInputCol("Original_Tweet")
    //streamed_sentiment_DF = sparkPipeline.transform(stream).filter($"final_sentiment"(0)==="positive" or $"final_sentiment"(0)==="negative")

    // the sentiment data of tweets
    val sentiment = sparkPipeline.transform(textDF)

    //prepare the data to be written into Kafka
    val data_written = sentiment.withColumn("value",to_json(struct("Original_Tweet","final_sentiment")))

    val query1 = data_written.writeStream.format("console").start()

    val query2 = data_written
      .selectExpr(  "CAST(value AS STRING)")
      .writeStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("topic","TweetTopic")
      .option("checkpointLocation","/tmp/vaquarkhan/checkpoint")
      .start()

    query1.awaitTermination()
    query2.awaitTermination()

    //spark.close()

  }

}

