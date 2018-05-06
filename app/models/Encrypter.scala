//package models
//
//import akka.actor.ActorRef
//import akka.pattern.ask
//import akka.util.Timeout
//import com.typesafe.scalalogging.LazyLogging
//
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.Future
//import scala.concurrent.duration.DurationInt
//import scala.reflect.ClassTag
//
//trait Encrypter extends LazyLogging {
//
//  val encryptionManager: ActorRef
//  private implicit val defaultTimeout = Timeout(60.seconds)
//
//  def decrCollection[T <: Encryptable: ClassTag](encrSeq: Seq[T]): Future[Seq[T]] = {
//    Future.sequence(
//      encrSeq.map(e => decrEntity(e))
//    )
//  }
//
//  def decrMessages(messages: Seq[Message]): Future[Seq[Message]] = {
//    (encryptionManager ? DecryptMessages(messages)).mapTo[Seq[Message]]
//  }
//
//  def encrMessages(messages: Seq[Message]): Future[Seq[Message]] = {
//    (encryptionManager ? EncryptMessages(messages)).mapTo[Seq[Message]]
//  }
//
//  def decrTexts(texts: Seq[String]): Future[Seq[String]] = {
//    (encryptionManager ? DecryptTexts(texts)).mapTo[Seq[String]]
//  }
//
//  def decrMessagesSpecParts(messages: Seq[Message]): Future[Seq[Message]] = {
//    (encryptionManager ? DecryptMessagesSpecParts(messages)).mapTo[Seq[Message]]
//  }
//
//  def decrEntity[T <: Encryptable: ClassTag](encrObj: T): Future[T] = {
//    (encryptionManager ? DecryptEntity[T](encrObj)).mapTo[T]
//  }
//
//  def encrCollection[T <: Encryptable: ClassTag](encrSeq: Seq[T]): Future[Seq[T]] = {
//    Future.sequence(
//      encrSeq.map(e => encrEntity(e))
//    )
//  }
//
//  def encrEntity[T <: Encryptable: ClassTag](decrObj: T): Future[T] = {
//    (encryptionManager ? EncryptEntity[T](decrObj)).mapTo[T]
//  }
//
//  def encryptIfNonEmpty(str: String): Future[Option[String]] = {
//    if (str.trim.isEmpty) {
//      Future.successful(None)
//    } else {
//      (encryptionManager ? EncryptText(str)).mapTo[String].map(s => Some(s))
//    }
//  }
//
//  def encryptOptionStr(str: Option[String]): Future[Option[String]] = {
//    if (str.isEmpty) {
//      Future.successful(None)
//    } else {
//      (encryptionManager ? EncryptText(str.get)).mapTo[String].map(s => Some(s))
//    }
//  }
//
//  def channelToEncrOptString(channel: String): Future[Option[String]] = {
//    if (channel == "all") {
//      Future.successful(None)
//    } else {
//      (encryptionManager ? EncryptText(channel)).mapTo[String].map(s => Some(s))
//    }
//  }
//
//}
