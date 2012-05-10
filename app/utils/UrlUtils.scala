package utils

import java.net.URLEncoder

object UrlUtils {
  def encodeParam(params:Map[String, Any]):String = {
    def encode(k:String, v:String):String = URLEncoder.encode(k, "utf-8") + "=" + URLEncoder.encode(v, "utf-8")
    params.map { case(key, value) =>
      value match {
        case v:String => encode(key, v)
        case vl:List[_] => vl.map(v => encode(key, v.toString)).mkString("&")
      }
    }.toList.reduceLeft((x, y) => x + "&" + y)
  }
}