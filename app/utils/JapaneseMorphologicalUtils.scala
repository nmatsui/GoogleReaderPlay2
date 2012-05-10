package utils

import scala.collection.JavaConverters.asScalaBufferConverter

import org.atilika.kuromoji.Tokenizer

object JapaneseAnalyzer {
  def tokenize(source:String):String = {
    val token = Tokenizer.builder.build.tokenize(source).asScala.toList
    val nouns = token.filter(_.getPartOfSpeech.split(",").head == Constants.Morphology.speech)
    				.map(_.getBaseForm)
    				.filterNot(_ == null)
    				.filterNot(_.isEmpty)
    val result = nouns.distinct.map(noun => (noun, nouns.count(_==noun)))
    				.filter(_._2 > 1)
    				.sortWith(_._2 > _._2)
    "{" + toJsonStr(result) + "}"
  }
  private def toJsonStr(data:List[(String, Int)]):String = {
    data.map{case(noun, count) => """"%s":%d""".format(noun, count)}.mkString(",")
  }
}