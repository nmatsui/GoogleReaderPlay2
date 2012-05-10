package controllers

import scala.io.Source

import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient

import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.Controller
import utils.UrlUtils.encodeParam
import utils.Constants

object Authentication extends Controller {
  private val client = new DefaultHttpClient
  
  def index = Action { request =>
    println("Authentication#index")
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
    println(url)
    Ok(views.html.index(url))
  }
  
  def callback = Action { request =>
    println("Authentication#callback")
    request.queryString.get(Constants.Google.oAuth2.responseType) match {
      case None => Ok(views.html.error("can't get authCode"))
      case Some(seq) => {
        Redirect(routes.Authentication.initialize).withSession(
          Constants.Google.oAuth2.responseType -> seq.head
        )
      }
    }
  }
  
  def initialize = Action { request =>
    println("Authentication#redirect")
    Ok(views.html.initialize())
  }
  
  def asyncInit = Action { request =>
    println("Authentication#asyncInit")
    request.session.get(Constants.Google.oAuth2.responseType) match {
      case None => Ok(views.html.error("invalid access"))
      case Some(authCode) => {
        println("auth code => " + authCode)
        
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
        val body = Json.parse(Source.fromInputStream(response.getEntity.getContent).getLines.mkString("\n"))
        println(body)
        response.getStatusLine.getStatusCode match {
          case 200 => {
            (body \ "access_token").asOpt[String] match {
              case None => Ok(views.html.error("access_token not found"))
              case Some(accessToken) => {
                println("access_token => " + accessToken)
                Ok("""{"result":"ok"}""").withSession(
                  Constants.ParamName.accessToken -> accessToken
                ).as("application/json")
              }
            }
          }
          case _ => {
            val errorMessage = (body \ "error").as[String]
            println("error => " + errorMessage)
            Ok("""{"result":"ng","error":"%s"}""".format(errorMessage)).as("application/json")
          }
        }
      }
    }
  }
}