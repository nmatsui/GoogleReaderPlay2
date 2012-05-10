package utils

import java.net.URLEncoder

import scala.io.Source

import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.HttpResponse

import play.Logger

object HttpUtils {
  private val client = new DefaultHttpClient
  
  def encodeParam(params:Map[String, Any]):String = {
    def encode(k:String, v:String):String = URLEncoder.encode(k, "utf-8") + "=" + URLEncoder.encode(v, "utf-8")
    params.map { case(key, value) =>
      value match {
        case v:String => encode(key, v)
        case vl:List[_] => vl.map(v => encode(key, v.toString)).mkString("&")
      }
    }.toList.reduceLeft((x, y) => x + "&" + y)
  }
  
  def doGet[A](url:String, 
               params:Map[String, Any], 
               accessToken:Option[String] = None)(func:(Int, String) => A):A = {
    val get = new HttpGet(url + "?" + encodeParam(params))
    if (accessToken.isDefined) get.setHeader("Authorization", "Bearer " + accessToken.get)
    val response = client.execute(get)
    exec(response, func)
  }
  
  def doPost[A](url:String, 
                params:Map[String, Any], 
                accessToken:Option[String] = None)(func:(Int, String) => A):A = {
    val post = new HttpPost(url)
    if (accessToken.isDefined) post.setHeader("Authorization", "Bearer " + accessToken.get)
    post.setHeader("Content-Type", "application/x-www-form-urlencoded")
    post.setEntity(new StringEntity(encodeParam(params)))
    val response = client.execute(post)
    exec(response, func)
  }
  
  private def exec[A](response:HttpResponse, func: (Int, String) => A): A = {
    val statusCode = response.getStatusLine.getStatusCode
    val source = Source.fromInputStream(response.getEntity.getContent)
    try {
      val body = source.getLines.mkString("\n")
      Logger.debug("""statusCode:%d : body:%s""".format(statusCode, body))
      func(statusCode, body)
    }
    finally {
      source.close
    }
  }
}