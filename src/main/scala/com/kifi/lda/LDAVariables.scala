package com.kifi.lda

import java.io._
import java.nio.ByteBuffer

/**
 * Variables in the graphical model.
 */

// theta: doc-topic distribution
case class Theta(value: Array[Float])

// beta: topic-word distribution. T x V matrix. Each row is a topic-word distribution.
case class Beta(value: Array[Float], numTopics: Int, vocSize: Int){
  def get(topic: Int, word: Int): Float = value(topic * vocSize + word)
  def set(topic: Int, word: Int, x: Float): Unit = value(topic * vocSize + word) = x
  def setRow(topic: Int, row: Array[Float]): Unit = {
    var i = 0
    while (i < vocSize) { set(topic, i, row(i)); i += 1 }
  }
}

case class WordTopicCounts(value: Array[Int], numTopics: Int, vocSize: Int){
  def get(topic: Int, word: Int): Int = value(topic * vocSize + word)
  def incre(topic: Int, word: Int): Unit = { value(topic * vocSize + word) = value(topic * vocSize + word) + 1 }
  def getRow(topicId: Int): Array[Int] = value.slice( topicId * vocSize, (topicId + 1) * vocSize)
  def clearAll(){
    var i = 0
    while (i < value.size) {value(i) = 0; i += 1 }
  }
}

case class Doc(index: Int, content: Array[Int])
case class WordTopicAssigns(value: Array[(Int, Int)])  // (wordId, topicId)


// some IO utils

object Beta {
  def toBytes(beta: Beta): Array[Byte] = {
    val numBytes = 4 * 2 + 4 * beta.value.size

    val bs = new ByteArrayOutputStream(numBytes)
    val os = new DataOutputStream(bs)
    os.writeInt(beta.numTopics)
    os.writeInt(beta.vocSize)
    beta.value.foreach{os.writeFloat(_)}
    os.close()
    val rv = bs.toByteArray()
    bs.close()
    rv
  }

  def toFile(beta: Beta, path: String) = {
    if (4 * 2.0 + 4.0 * beta.value.size > Int.MaxValue){
      robustToFile(beta, path)  
    } else {
      val os = new FileOutputStream(path)
      val bytes = toBytes(beta)
      os.write(bytes)
      os.close()
    }
  }
  
  private def robustToFile(beta: Beta, path: String) = {
    val os = new FileOutputStream(path)
    val bufsize = beta.vocSize * 4
    val buf = new Array[Byte](bufsize)
    val bbuf = ByteBuffer.wrap(buf, 0, bufsize)
    bbuf.putInt(beta.numTopics)
    bbuf.putInt(beta.vocSize)
    os.write(buf, 0, 8)
    bbuf.clear()
    for( i <- 0 until beta.numTopics){
      var v = 0
      while (v < beta.vocSize){
        bbuf.putFloat(beta.get(i, v))
        v += 1
      }
      os.write(buf, 0, bufsize)
      bbuf.clear()
    }
    os.close()
  }

  def fromFile(path: String) = {
    val is = new DataInputStream(new FileInputStream(new File(path)))
    val numTopics = is.readInt()
    val vocSize = is.readInt()
    val value = new Array[Float](numTopics * vocSize)
    val N = value.size
    var i = 0
    while ( i < N){
      value(i) = is.readFloat()
      i += 1
    }
    Beta(value, numTopics, vocSize)
  }
}
