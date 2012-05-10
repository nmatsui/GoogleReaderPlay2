package utils

object Constants {
  val clientName = "google_reader_play2"
  object Google {
    object oAuth2 {
      val clientId     = "xxxxxx"
      val clientSecret = "xxxxxx"
      val redirectURI  = "xxxxxx"
      val responseType = "code"
      val authURL      = "https://accounts.google.com/o/oauth2/auth"
      val tokenURL     = "https://accounts.google.com/o/oauth2/token"
    }
    object Reader {
      val baseURI        = "https://www.google.com/reader/api/"
      val tagListURL     = baseURI + "0/tag/list"
      val feedIdURL      = baseURI + "0/stream/items/ids"
      val feedContentURL = baseURI + "0/stream/items/contents" 
    }
  }
  object Morphology {
    val speech = "名詞"
  }
  object ParamName {
    val accessToken  = "access_token"
    val tagListId = "tagListId"
  }
}