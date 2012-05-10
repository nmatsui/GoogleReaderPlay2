package controllers

import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.Controller
import play.Logger
import utils.HttpUtils.encodeParam
import utils.Constants
import utils.HttpUtils

object Authentication extends Controller {
  
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
      case Some(seq) => {
        Redirect(routes.Authentication.initialize).withSession {
          Constants.Google.oAuth2.responseType -> seq.head
        }
      }
      case None => Ok(views.html.error("can't get authCode"))
    }
  }
  
  def initialize = Action { request =>
    Logger.info("Authentication#redirect")
    Ok(views.html.initialize())
  }
  
  def asyncInit = Action { request =>
    Logger.info("Authentication#asyncInit")
    request.session.get(Constants.Google.oAuth2.responseType) match {
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
        
        HttpUtils.doPost(url, params) { case(statusCode, body) =>
          statusCode match {
            case 200 => {
              val json = Json.parse(body)
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
              val json = Json.parse(body)
              val errorMessage = (json \ "error").as[String]
              Logger.error(errorMessage)
              Ok(Constants.ParamName.resultNg(errorMessage)).as("application/json")
            }
          }
        }
      }
      case None => Ok(views.html.error("invalid access"))
    }
  }
}