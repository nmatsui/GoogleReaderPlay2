package utils

import java.io.StringReader

import scala.xml.InputSource
import scala.xml.parsing.NoBindingFactoryAdapter
import scala.xml.NodeSeq

import nu.validator.htmlparser.common.XmlViolationPolicy
import nu.validator.htmlparser.sax.HtmlParser

class String2NodeSeq(s:String) {
  def toNodeSeq:NodeSeq = {
    val hp = new HtmlParser
    hp.setNamePolicy(XmlViolationPolicy.ALLOW)
    val saxer = new NoBindingFactoryAdapter
    hp.setContentHandler(saxer)
    hp.parse(new InputSource(new StringReader(s)))
    saxer.rootElem
  }
}

class NodeSeqHelper(target:NodeSeq) {
  def \@ (nodePath:String):NodeSeq = {
    attrCheck(nodePath, (target \ _ ))
  }
  def \\@ (nodePath:String):NodeSeq = {
    attrCheck(nodePath, (target \\ _ ))
  }
  private def attrCheck(nodePath:String, exec:String=>NodeSeq) = {
    val ex = """^(.+)\[@(.+)=(.+)\]$""".r
    nodePath match {
      case ex(node, attr, value) => {
        exec(node).filter(node => (node \ ("@"+attr)).text == value)
      }
      case _ => throw new RuntimeException("""invalid \@ or \\@ format""")
    }
  }
}