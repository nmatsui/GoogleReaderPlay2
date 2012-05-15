Google Reader Play2!
==============================

[Playframework2](http://www.playframework.org/) + [scala](http://www.scala-lang.org/)で作成した、「Google Readerで自分が購読している記事」を解析し、出現する単語のトレンドを表示するアプリです。

Google Readerのデータを取得するために、[Google oAuth2](https://developers.google.com/accounts/docs/OAuth2)の「Web application API」を用いています。

また記事を形態素解析のために、javaで実装されディクショナリも内蔵された[Kuromoji](http://www.atilika.org/)を利用しました。ありがとうございます！


環境準備
--------

* [Google API Console](https://code.google.com/apis/console/)より、Google Serviceを利用するためのキーを取得してください。
    * Google Readerは公式なサービスAPIが公開されていないため、「URL Shortener API」で代替します。「service」タブから「URL Shortener API」をONにしてください
    * ProjectをCreateし、「Web application」のClientIDを生成してください（この際、RedirectURIは http://localhost:9000/oauth2callback にしてください）
    * app/utils/Constants.scala の clintId, clientSecret, redirectURI を修正してください

License
-------
Copyright(C) 2012 Nobuyuki Matsui (nobuyuki.matsui@gmail.com)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
