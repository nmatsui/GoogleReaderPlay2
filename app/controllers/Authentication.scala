package controllers

import scala.io.Source

import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient

import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.Controller
import play.Logger

import utils.UrlUtils.encodeParam
import utils.Constants

object Authentication extends Controller {
  private val client = new DefaultHttpClient
  
  def index = Action { request =>
    Logger.info("Authentication#index")
    val params = Map(
        "response_type"   -> Constants.Google.oAuth2.responseType,
        "client_id"       -> Constants.Google.oAuth2.clientId,
        "redirect_uri"    -> Constants.Google.oAuth2.redirectURI,
        "scope"           -> Constants.Google.Reader.baseURI,
        "state"           -> Constants.clientName,
        "access_type"     -> "online",
        "approval_prompt" -> "force"
        )
    val url = Constants.Google.oAuth2.authURL + "?" + encodeParam(params)
    Ok(views.html.index(url))
  }
  
  def callback = Action { request =>
    Logger.info("Authentication#callback")
    request.queryString.get(Constants.Google.oAuth2.responseType) match {
      case None => Ok(views.html.error("can't get authCode"))
      case Some(seq) => {
        Redirect(routes.Authentication.initialize).withSession {
          Constants.Google.oAuth2.responseType -> seq.head
        }
      }
    }
  }
  
  def initialize = Action { request =>
    Logger.info("Authentication#redirect")
    Ok(views.html.initialize())
  }
  
  def asyncInit = Action { request =>
    Logger.info("Authentication#asyncInit")
    request.session.get(Constants.Google.oAuth2.responseType) match {
      case None => Ok(views.html.error("invalid access"))
      case Some(authCode) => {
        Logger.debug("auth code => " + authCode)
        
        val params = Map(
            "code"          -> authCode,
            "client_id"     -> Constants.Google.oAuth2.clientId,
            "client_secret" -> Constants.Google.oAuth2.clientSecret,
            "redirect_uri"  -> Constants.Google.oAuth2.redirectURI,
            "grant_type"    -> "authorization_code"
            )
        val url = Constants.Google.oAuth2.tokenURL
        
        val post = new HttpPost(url)
        post.setHeader("Content-Type", "application/x-www-form-urlencoded")
        post.setEntity(new StringEntity(encodeParam(params)))
        val response = client.execute(post)
        val statusCode = response.getStatusLine.getStatusCode
        val body = Source.fromInputStream(response.getEntity.getContent).getLines.mkString("\n")
        Logger.debug("""statusCode:%d : body:%s""".format(statusCode, body))
        val json = Json.parse(body)
        statusCode match {
          case 200 => {
            (json \ "access_token").asOpt[String] match {
              case None => Ok(views.html.error("access_token not found"))
              case Some(accessToken) => {
                Logger.debug(Constants.ParamName.resultOk("{}"))
                Ok(Constants.ParamName.resultOk("{}")).withSession {
                  Constants.ParamName.accessToken -> accessToken
                }.as("application/json")
              }
            }
          }
          case _ => {
            val errorMessage = (json \ "error").as[String]
            Logger.error(errorMessage)
            Ok(Constants.ParamName.resultNg(errorMessage)).as("application/json")
          }
        }
      }
    }
  }
}