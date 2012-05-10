package controllers

import scala.xml.NodeSeq

import play.api.mvc.Action
import play.api.mvc.Controller
import play.Logger
import utils.Constants
import utils.HttpUtils
import utils.JapaneseAnalyzer
import utils.NodeSeqHelper
import utils.String2NodeSeq

object Reader extends Controller {
  implicit def string2NodeSeq(s:String) = new String2NodeSeq(s)
  implicit def nodeSeqHelper(target:NodeSeq) = new NodeSeqHelper(target)
  
  def tagList = Action { request =>
    Logger.info("Reader#tagList")
    request.session.get(Constants.ParamName.accessToken) match {
      case Some(accessToken) => {
        val params = Map(
            "output" -> "xml"
            )
        val url = Constants.Google.Reader.tagListURL
        
        HttpUtils.doGet(url, params, Some(accessToken)) { case(statusCode, body) =>
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
      case None => Ok(views.html.error("invalid access"))
    }
  }
  
  def asyncWordCloud = Action { request =>
    Logger.info("Reader#asyncWordCloud")
    (request.queryString.get(Constants.ParamName.tagListId), 
     request.session.get(Constants.ParamName.accessToken)) match {
      case (Some(seqOfId), Some(accessToken)) => {
        val params = Map(
            "s" -> seqOfId.head,
            "n" -> "80",
            "output" -> "xml"
            )
        val url = Constants.Google.Reader.feedIdURL
        
        HttpUtils.doGet(url, params, Some(accessToken)) { case(statusCode, body) =>
          statusCode match {
            case 200 => {
              val xml = body.toNodeSeq
              val feedIds = (xml \\@ "number[@name=id]").map(_.text)
              Logger.debug("feedIds:" + feedIds)
              getFeedsContents(feedIds, accessToken) match {
                case Some(contents) => {
                  val data = JapaneseAnalyzer.tokenize(contents)
                  Logger.debug("content:" + data)
                  Ok(Constants.ParamName.resultOk(data)).as("application/json")
                }
                case None => Ok(Constants.ParamName.resultNg("can't get feed content")).as("application/json")
              }
            }
            case _ => Ok(Constants.ParamName.resultNg("can't get feed id list")).as("application/json")
          }
        }
      }
      case _ => Ok(Constants.ParamName.resultNg("can't find id or accessToken")).as("application/json")
    }
  }
  
  private def getFeedsContents(ids:Seq[String], accessToken:String):Option[String] = {
    Logger.info("Reader#getFeeds")
    val params = Map(
        "i" -> ids,
        "output" -> "atom"
        )
    val url = Constants.Google.Reader.feedContentURL
    
    HttpUtils.doGet(url, params, Some(accessToken)) { case(statusCode, body) =>
      statusCode match {
        case 200 => {
          val xml = body.toNodeSeq
          val contents = ((xml \\ "content").toList ::: (xml \\ "summary").toList)
                        .map(_.text.toNodeSeq.map(_.text).mkString("\n")).mkString("\n")
          Option(contents)
        }
        case _ => None
      }
    }
  }
}