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
import play.Logger

object Reader extends Controller {
  implicit def string2NodeSeq(s:String) = new String2NodeSeq(s)
  implicit def nodeSeqHelper(target:NodeSeq) = new NodeSeqHelper(target)
  private val client = new DefaultHttpClient
  
  def tagList = Action { request =>
    Logger.info("Reader#tagList")
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
        val statusCode = response.getStatusLine.getStatusCode
        val body = Source.fromInputStream(response.getEntity.getContent).getLines.mkString("\n")
        Logger.debug("""statusCode:%d : body:%s""".format(statusCode, body))
        statusCode match {
          case 200 => {
            val xml = body.toNodeSeq
            val tagListIds = (xml \\@ "string[@name=id]").filter(_.text.contains("/label/")).map(_.text)
            Logger.debug("tagListIds:" + tagListIds)
            Ok(views.html.feeds(tagListIds))
          }
          case _ => {
            Ok(views.html.error("can't get tag list"))
          }
        }
      }
    }
  }
  
  def asyncWordCloud = Action { request =>
    Logger.info("Reader#asyncWordCloud")
    (request.queryString.get(Constants.ParamName.tagListId), request.session.get(Constants.ParamName.accessToken)) match {
      case (Some(seqOfId), Some(accessToken)) => {
        val params = Map(
            "s" -> seqOfId.head,
            "n" -> "80",
            "output" -> "xml"
            )
        val url = Constants.Google.Reader.feedIdURL + "?" + encodeParam(params)
        val get = new HttpGet(url)
        get.setHeader("Authorization", "Bearer " + accessToken)
        val response = client.execute(get)
        val statusCode = response.getStatusLine.getStatusCode
        val body = Source.fromInputStream(response.getEntity.getContent).getLines.mkString("\n")
        Logger.debug("""statusCode:%d : body:%s""".format(statusCode, body))
        statusCode match {
          case 200 => {
            val xml = body.toNodeSeq
            val feedIds = (xml \\@ "number[@name=id]").map(_.text)
            Logger.debug("feedIds:" + feedIds)
            getFeedsContents(feedIds, accessToken) match {
              case None => Ok("""{"result":"ng","error":"can't get feed content"}""").as("application/json")
              case Some(contents) => {
                val data = JapaneseAnalyzer.tokenize(contents)
                Logger.debug("tag cloud:" + data)
                Ok("""{"result":"ok","tagCloud":%s}""".format(data)).as("application/json")
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
    Logger.info("Reader#getFeeds")
    val params = Map(
        "i" -> ids,
        "output" -> "atom"
        )
    val url = Constants.Google.Reader.feedContentURL + "?" + encodeParam(params)
    val get = new HttpGet(url)
    get.setHeader("Authorization", "Bearer " + accessToken)
    val response = client.execute(get)
    val statusCode = response.getStatusLine.getStatusCode
    val body = Source.fromInputStream(response.getEntity.getContent).getLines.mkString("\n")
    Logger.debug("""statusCode:%d : body:%s""".format(statusCode, body))
    statusCode match {
      case 200 => {
        val xml = body.toNodeSeq
        val contents = ((xml \\ "content").toList ::: (xml \\ "summary").toList)
        				.map(_.text.toNodeSeq.map(_.text).mkString("\n"))
        				.mkString("\n")
        Option(contents)
      }
      case _ => None
    }
  }
}