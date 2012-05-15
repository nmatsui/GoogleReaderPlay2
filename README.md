Google Reader Play2!
==============================

[Playframework2](http://www.playframework.org/) + [scala](http://www.scala-lang.org/)で作成した、「Google Readerで自分が購読している記事」を解析し、出現する単語のトレンドを表示するアプリです。

Google Readerのデータを取得するために、[Google oAuth2](https://developers.google.com/accounts/docs/OAuth2)の「Web application API」を用いています。

また記事を形態素解析のために、javaで実装されディクショナリも内蔵された[Kuromoji](http://www.atilika.org/)を利用しました。ありがとうございます！


環境準備
--------

[Google API Console](https://code.google.com/apis/console/)より、Google Serviceを利用するためのキーを取得してください。

1. Google Readerは公式なサービスAPIが公開されていないため、「URL Shortener API」で代替します。「service」タブから「URL Shortener API」をONにしてください
2. ProjectをCreateし、「Web application」のClientIDを生成してください（この際、RedirectURIは http://localhost:9000/oauth2callback にしてください）
3. app/utils/Constants.scala の clintId, clientSecret, redirectURI を修正してください

必要なライブラリを追加し、ClassPathに定義してください
* HTTPクライアント [Apache HttpClient 4.1.3](http://hc.apache.org/httpcomponents-client-ga/)
    * httpcore-4.1.4.jar
    * httpclient-4.1.3.jar

* HTML(XML)パーサ [Validator.nu](http://about.validator.nu/htmlparser/) 
    * htmlparser-1.3.1-with-transitions.jar

* 形態素解析ライブラリ [Kuromoji](http://www.atilika.org/)
    * kuromoji-0.7.7.jar

[Playframeworkのドキュメント](http://www.playframework.org/documentation/2.0.1/Installing)に従い、[Playframework 2.0.1](http://download.playframework.org/releases/play-2.0.1.zip)をインストールしてください

使い方
------

1. GoogleReaderPlay2ディレクトリで、`play`を実行してください（Proxyの背後から利用する場合、`play -Dhttp.proxyHost=192.168.0.1 -Dhttp.proxyPort=8080 -Dhttp.proxyUser=hoge -Dhttp.proxyPassword=fuga`等、Proxyの設定を引数で指定してください）
1. Playコンソール上から、`clean`
1. Playコンソール上から、`compile`
1. Playコンソール上から、`run`
2. ブラウザから`http://localhost:9000`にアクセスすると、TOP画面が表示されます
2. 「Googleへログイン」からgoogle認証サイトへ遷移し、Googleアカウントでログインした後アプリケーションのアクセスを許可してください
3. Google Readerに定義してあるフォルダのリストが表示されます
4. 各フォルダの「単語クラウド表示」をクリックすると、そのフォルダに分類されたRSSフィードを80件取得し、出現頻度に従ってタグクラウド風に単語（名詞）を表示します

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
