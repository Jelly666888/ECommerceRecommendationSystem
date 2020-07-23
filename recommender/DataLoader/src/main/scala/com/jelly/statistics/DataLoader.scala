package com.jelly.statistics

import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}


//定义样例类
case class Product(productId: Int, name: String, imageUrl: String, categories: String, tags: String)

case class Rating(userId: Int, productId: Int, score: Double, timestamp: Int)

case class MongoConfig(uri: String, db: String)

object DataLoader {
  val PRODUCT_DATA_PATH = "src\\main\\resources\\products.csv"
  val RATING_DATA_PATH = "src\\main\\resources\\ratings.csv"

  val MONGODB_PRODUCT_COLLECTION = "Product"
  val MONGODB_RATING_COLLECTION = "Rating"

  def main(args: Array[String]): Unit = {
    val config = Map("spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://localhost:27017/recommender",
      "mongo.db" -> "recommender"
    )

    val sparkConf = new SparkConf().setAppName("DataLoader").setMaster(config("spark.cores"))
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    //对DataFrame和Dataset进行操作许多操作都需要这个包进行支持
    import spark.implicits._

    val productRDD = spark.sparkContext.textFile(PRODUCT_DATA_PATH)
    val productDF = productRDD.map(row => {
      val datas = row.split("\\^")
      Product(datas(0).trim.toInt, datas(1).trim, datas(4).trim, datas(5).trim, datas(6).trim)
    }).toDF

    val ratingRDD = spark.sparkContext.textFile(RATING_DATA_PATH)
    val ratingDF: DataFrame = ratingRDD.map(row => {
      val datas = row.split(",")
      Rating(datas(0).trim.toInt, datas(1).trim.toInt, datas(2).trim.toDouble, datas(3).trim.toInt)
    }).toDF

    // 声明一个
    implicit val mongoConfig = MongoConfig(config.get("mongo.uri").get, config.get("mongo.db").get)

    // 将数据保存到MongoDB中
    storeDataInMongoDB(productDF, ratingDF)

    spark.stop()

  }

  def storeDataInMongoDB(productDF: DataFrame, ratingDF: DataFrame)(implicit mongoConfig: MongoConfig) = {

    //新建一个到MongoDB的连接
    val mongoClient: MongoClient = MongoClient(MongoClientURI(mongoConfig.uri))
    //定义操作的表 db.product
    val productCollection = mongoClient(mongoConfig.db)(MONGODB_PRODUCT_COLLECTION)
    val ratingCollection = mongoClient(mongoConfig.db)(MONGODB_RATING_COLLECTION)

    //如果MongoDB中有对应的数据库，那么应该删除
    productCollection.dropCollection()
    ratingCollection.dropCollection()

    //将当前数据写入到MongoDB
    productDF.write
      .option("uri",mongoConfig.uri)
      .option("collection",MONGODB_PRODUCT_COLLECTION)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

    ratingDF.write
      .option("uri",mongoConfig.uri)
      .option("collection",MONGODB_RATING_COLLECTION)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()

    productCollection.createIndex(MongoDBObject("productId" -> 1))
    ratingCollection.createIndex(MongoDBObject("userId" -> 1))
    ratingCollection.createIndex(MongoDBObject("productId" -> 1))

    mongoClient.close()
  }

}
