package com.honestefforts;

import java.util.concurrent.TimeoutException;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.streaming.StreamingQueryListener;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.spark.sql.functions.*;

public class PaymentSparkAnalytics {
  private static final Logger logger = LoggerFactory.getLogger(PaymentSparkAnalytics.class);
  private static final StructType paymentSchema = new StructType()
      .add("user", DataTypes.StringType)
      .add("paymentAmt", DataTypes.DoubleType);
  private static final String kafkaBootstrapServer = "192.168.5.193:32119";

  public static void main(String[] args) {
    try {
      UserGroupInformation.setLoginUser(UserGroupInformation.createRemoteUser("spark"));

      logger.info("==========================================");
      logger.info("Starting Spark Streaming Job");
      logger.info("==========================================");

      SparkSession spark = SparkSession.builder()
          .appName("PaymentAnalytics")
          .getOrCreate();

      // Log Spark configuration
      logger.debug("Spark configuration: {}", spark.sparkContext().getConf().toDebugString());

      // Read from Kafka
      Dataset<Row> payments = spark.readStream()
          .format("kafka")
          .option("kafka.bootstrap.servers", kafkaBootstrapServer)
          .option("subscribe", "payments")
          .option("startingOffsets", "latest")
          .option("maxOffsetsPerTrigger", "20")  // Limit ingestion per batch
          .load()
          .selectExpr("CAST(value AS STRING) as json")
          .select(from_json(col("json"), paymentSchema).alias("data"))
          .select("data.*");

      logger.info("Successfully connected to Kafka topic 'payments'");

      // Aggregation
      Dataset<Row> aggregated = payments.groupBy(col("user"))
          .agg(
              count("*").alias("transactionCount"),
              sum("paymentAmt").alias("totalAmount")
          );

      // Write to Kafka
      StreamingQuery query = aggregated
          .selectExpr("to_json(struct(*)) AS value")
          .writeStream()
          .format("kafka")
          .option("kafka.bootstrap.servers", kafkaBootstrapServer)
          .option("topic", "payment-stats")
          .option("checkpointLocation", "/tmp/spark-checkpoint")
          .trigger(org.apache.spark.sql.streaming.Trigger.ProcessingTime("5 seconds"))  // Add trigger
          .outputMode("update")
          .start();

      logger.info("Started streaming query with ID: {}", query.id());

      // Add streaming progress listener
      spark.streams().addListener(new StreamingQueryListener() {
        @Override
        public void onQueryStarted(QueryStartedEvent event) {
          logger.info("Query started: {}", event.id());
        }

        @Override
        public void onQueryProgress(QueryProgressEvent event) {
          logger.info("Progress update for query {}:\n{}", event.progress().id(), event.progress().json());
        }

        @Override
        public void onQueryTerminated(QueryTerminatedEvent event) {
          logger.info("Query terminated: {}", event.id());
        }
      });

      query.awaitTermination();

    } catch (TimeoutException | StreamingQueryException e) {
      logger.error("Streaming query failed: {}", e.getMessage(), e);
      throw new RuntimeException(e);
    } finally {
      logger.info("Shutting down Spark session");
      SparkSession activeSession = SparkSession.getActiveSession().getOrElse(null);
      if (activeSession != null) {
        activeSession.stop();
      }
    }
  }
}