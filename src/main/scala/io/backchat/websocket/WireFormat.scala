package io.backchat.websocket

import net.liftweb.json._
import akka.util.duration._

trait WireFormat {

  def parseInMessage(message: String): WebSocketInMessage

  def parseOutMessage(message: String): WebSocketOutMessage

  def render(message: WebSocketOutMessage): String

}

class SimpleJsonWireFormat(implicit formats: Formats) extends WireFormat {
  private def parseMessage(message: String) = {
    if (message.trim.startsWith("{") || message.trim.startsWith("["))
      parseOpt(message) map (JsonMessage(_)) getOrElse TextMessage(message)
    else TextMessage(message)
  }

  def parseOutMessage(message: String): WebSocketOutMessage = parseMessage(message)

  def parseInMessage(message: String): WebSocketInMessage = parseMessage(message)

  def render(message: WebSocketOutMessage) = message match {
    case TextMessage(text) => text
    case JsonMessage(json) => compact(JsonAST.render(json))
    case _ => ""
  }
}

object JsonProtocolWireFormat {

  object ParseToWebSocketInMessage {

    def apply(message: String)(implicit format: Formats) = inferMessageTypeFromContent(message)

    private def inferMessageTypeFromContent(content: String)(implicit format: Formats): WebSocketInMessage = {
      val possiblyJson = content.trim.startsWith("{") || content.trim.startsWith("[")
      if (!possiblyJson) TextMessage(content)
      else parseOpt(content) map inferJsonMessageFromContent getOrElse TextMessage(content)
    }

    private def inferJsonMessageFromContent(content: JValue)(implicit format: Formats) = {
      val contentType = (content \ "type").extractOpt[String].map(_.toLowerCase) getOrElse "none"
      (contentType) match {
        case "ack_request" ⇒ AckRequest(inferContentMessage((content \ "content")), (content \ "id").extract[Long])
        case "ack" ⇒ Ack((content \ "id").extract[Long])
        case "text" ⇒ TextMessage((content \ "content").extract[String])
        case "json" ⇒ JsonMessage((content \ "content"))
        case _ ⇒ JsonMessage(content)
      }
    }

    private def inferContentMessage(content: JValue)(implicit format: Formats): Ackable = {
      content match {
        case JString(text) ⇒ TextMessage(text)
        case _ =>
          val contentType = (content \ "type").extractOrElse("none")
          (contentType) match {
            case "text" ⇒ TextMessage((content \ "content").extract[String])
            case "json" ⇒ JsonMessage((content \ "content"))
            case "none" ⇒ content match {
              case JString(text) =>
                val possiblyJson = text.trim.startsWith("{") || text.trim.startsWith("[")
                if (!possiblyJson) TextMessage(text)
                else parseOpt(text) map inferContentMessage getOrElse TextMessage(text)
              case jv => JsonMessage(content)
            }
          }
      }
    }
  }

  object ParseToWebSocketOutMessage {
    def apply(message: String)(implicit format: Formats): WebSocketOutMessage = inferMessageTypeFromContent(message)

    private def inferMessageTypeFromContent(content: String)(implicit format: Formats): WebSocketOutMessage = {
      val possiblyJson = content.trim.startsWith("{") || content.trim.startsWith("[")
      if (!possiblyJson) TextMessage(content)
      else parseOpt(content) map inferJsonMessageFromContent getOrElse TextMessage(content)
    }

    private def inferJsonMessageFromContent(content: JValue)(implicit format: Formats): WebSocketOutMessage = {
      val contentType = (content \ "type").extractOpt[String].map(_.toLowerCase) getOrElse "none"
      (contentType) match {
        case "ack" => Ack((content \ "id").extract[Long])
        case "needs_ack" ⇒ NeedsAck(inferContentMessage(content \ "content"), (content \ "timeout").extract[Long].millis)
        case "text" ⇒ TextMessage((content \ "content").extract[String])
        case "json" ⇒ JsonMessage((content \ "content"))
        case _ ⇒ JsonMessage(content)
      }
    }

    private def inferContentMessage(content: JValue)(implicit format: Formats): Ackable = content match {
      case JString(text) ⇒ TextMessage(text)
      case _ =>
        val contentType = (content \ "type").extractOrElse("none")
        (contentType) match {
          case "text" ⇒ TextMessage((content \ "content").extract[String])
          case "json" ⇒ JsonMessage((content \ "content"))
          case "none" ⇒ content match {
            case JString(text) =>
              val possiblyJson = text.trim.startsWith("{") || text.trim.startsWith("[")
              if (!possiblyJson) TextMessage(text)
              else parseOpt(text) map inferContentMessage getOrElse TextMessage(text)
            case jv => JsonMessage(content)
          }
        }
    }
  }

  object RenderOutMessage {

    import JsonDSL._

    def apply(message: WebSocketOutMessage): String = {
      message match {
        case Ack(id) ⇒ compact(render(("type" -> "ack") ~ ("id" -> id)))
        case m: TextMessage ⇒ compact(render(contentFrom(m)))
        case m: JsonMessage ⇒ compact(render(contentFrom(m)))
        case NeedsAck(msg, timeout) ⇒
          compact(render(("type" -> "needs_ack") ~ ("timeout" -> timeout.toMillis) ~ ("content" -> contentFrom(msg))))
        case x ⇒ sys.error(x.getClass.getName + " is an unsupported message type")
      }
    }

    private[this] def contentFrom(message: Ackable): JValue = message match {
      case TextMessage(text) ⇒ ("type" -> "text") ~ ("content" -> text)
      case JsonMessage(json) ⇒ ("type" -> "json") ~ ("content" -> json)
    }
  }

}

class JsonProtocolWireFormat(implicit formats: Formats) extends WireFormat {
  import JsonProtocolWireFormat._
  def parseInMessage(message: String): WebSocketInMessage = ParseToWebSocketInMessage(message)
  def parseOutMessage(message: String): WebSocketOutMessage = ParseToWebSocketOutMessage(message)
  def render(message: WebSocketOutMessage) = RenderOutMessage(message)
}