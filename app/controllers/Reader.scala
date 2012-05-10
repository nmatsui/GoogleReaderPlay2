package controllers

import scala.io.Source
import scala.xml.NodeSeq
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import play.api.mvc.Action
import play.api.mvc.Controller
import utils.UrlUtils.encodeParam
import utils.NodeSeqHelper
import utils.String2NodeSeq
import utils.JapaneseAnalyzer
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import utils.Constants

object Reader extends Controller {
  implicit def string2NodeSeq(s:String) = new String2NodeSeq(s)
  implicit def nodeSeqHelper(target:NodeSeq) = new NodeSeqHelper(target)
  private val client = new DefaultHttpClient
  
  def tagList = Action { request =>
    println("Reader#tagList")
    request.session.get(Constants.ParamName.accessToken) match {
      case None => Ok(views.html.error("invalid access"))
      case Some(accessToken) => {
        val params = Map(
            "output" -> "xml"
            )
        val url = Constants.Google.Reader.tagListURL + "?" + encodeParam(params)
        val get = new HttpGet(url)
        get.setHeader("Authorization", "Bearer " + accessToken)
        val response = client.execute(get)
        println("statusLine:" + response.getStatusLine)
        response.getStatusLine.getStatusCode match {
          case 200 => {
            val body = Source.fromInputStream(response.getEntity.getContent).getLines.mkString("\n").toNodeSeq
            println("body:" + body)
            val ids = (body \\@ "string[@name=id]").filter(_.text.contains("/label/")).map(_.text)
            println("ids:" + ids)
            Ok(views.html.feeds(ids))
          }
          case _ => {
            Ok(views.html.error("can't get tag list"))
          }
        }
      }
    }
  }
  
  def asyncWordCloud = Action { request =>
    println("Reader#asyncWordCloud")
    (request.queryString.get(Constants.ParamName.tagListId), request.session.get(Constants.ParamName.accessToken)) match {
      case (Some(seqOfId), Some(accessToken)) => {
        println(seqOfId.head)
        val params = Map(
            "s" -> seqOfId.head,
            "n" -> "80",
            "output" -> "xml"
            )
        val url = Constants.Google.Reader.feedIdURL + "?" + encodeParam(params)
        val get = new HttpGet(url)
        get.setHeader("Authorization", "Bearer " + accessToken)
        val response = client.execute(get)
        println("statusLine:" + response.getStatusLine)
        response.getStatusLine.getStatusCode match {
          case 200 => {
            val body = Source.fromInputStream(response.getEntity.getContent).getLines.mkString("\n").toNodeSeq
            println("body:" + body)
            val ids = (body \\@ "number[@name=id]").map(_.text)
            println("ids:" + ids)
            getFeedsContents(ids, accessToken) match {
              case None => Ok("""{"result":"ng","error":"can't get feed content"}""").as("application/json")
              case Some(contents) => {
                val data = JapaneseAnalyzer.tokenize(contents)
                println(data)
                println("""{"result":"ok","data":%s}""".format(data))
                Ok("""{"result":"ok","data":%s}""".format(data)).as("application/json")
              }
            }
          }
          case _ => Ok("""{"result":"ng","error":"can't get feed id list"}""").as("application/json")
        }
      }
      case _ => Ok("""{"result":"ng","error":"can't find id or accessToken"}""").as("application/json")
    }
  }
  private def getFeedsContents(ids:Seq[String], accessToken:String):Option[String] = {
    println("Reader#getFeeds")
    val params = Map(
        "i" -> ids,
        "output" -> "atom"
        )
    val url = Constants.Google.Reader.feedContentURL + "?" + encodeParam(params)
    val get = new HttpGet(url)
    get.setHeader("Authorization", "Bearer " + accessToken)
    val response = client.execute(get)
    println("statusLine:" + response.getStatusLine)
    response.getStatusLine.getStatusCode match {
      case 200 => {
        val body = Source.fromInputStream(response.getEntity.getContent).getLines.mkString("\n").toNodeSeq
        val contents = ((body \\ "content").toList ::: (body \\ "summary").toList)
        				.map(_.text.toNodeSeq.map(_.text).mkString("\n"))
        				.mkString("\n")
        println("contents:" + contents)
        Option(contents)
      }
      case _ => {
        println(Source.fromInputStream(response.getEntity.getContent).getLines.mkString("\n"))
        None
      }
    }
  }
}