package com.seb.service

import com.redis.RedisClient
import com.redis.api.StringApi.NX
import play.api.Configuration

import java.net.{URI, URL}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class RedisClientFactory {
  def create(host: String, port: Int): RedisClient = new RedisClient(host, port)
}

class UrlService @Inject()(
  config: Configuration,
  implicit val ec: ExecutionContext,
  keyGen: RandomKeyGen,
  clientFactory: RedisClientFactory
) {

  private val host = config.get[String]("redis.host")
  private val port = config.get[Int]("redis.port")
  private val client = clientFactory.create(host, port)

  def shorten(url: String): Future[String] = {
    Future {
      val r: Try[String] = for {
        canonicalUrl <- canonical(url)
        (isPresent, key) <- retrySet(canonicalUrl, 3)
      } yield {
        if (!isPresent) {
          setLookupMapping(url = canonicalUrl, key = key)
        } else key
      }
      val key = r.get // exception will be caught by Future block
      s"http://localhost:9000/$key"
    }
  }

  // Validate URL and convert to canonical form, to avoid redundant mappings of equivalent URLs.
  // Partial implementation due to lack of time
  //
  // Will return Left[MalformedURLException] on error
  //
  def canonical(urlString: String): Try[String] = {
    val canonical = urlString.trim
      .stripSuffix("/")
      .replace(":80", "")
      .stripSuffix(":")
      .stripSuffix("/")
    Try(new URL(urlString).toURI).map(_ => canonical)
  }

  // Attempt to store an entry of key -> url; in the case of a key collision, retry
  //
  // Give the small size of the random key (8 bytes), there will occasional be key collisions if the
  // service is heavily used.
  //
  def retrySet(url: String, count: Int): Try[(Boolean, String)] = {
    if (count < 1) {
      Failure(new DuplicateKeyError)
    }
    else {
      val key = keyGen.generate
      setIfAbsent(key, url) match {
        case Some(r) if r != url =>
          retrySet(url, count - 1)
        case Some(r: String) =>
          // shortened URL already in data store, extremely unlikely scenario
          Success((true, r))
        case None =>
          Success((false, key))
      }
    }
  }

  // Return None, if the key was successfully set. If the key was present, this method
  // will return the old value, instead of replacing it.
  //
  // The Redis client does not yet support "SET NX GET", which would allow this to be
  // done as a single command
  //
  def setIfAbsent(key: String, value: String): Option[String] = {
    if (client.set(key, value, NX)) {
      None
    } else {
      client.get(key)
    }
  }

  // Set mapping of url -> key, which is needed to handle lookup of the shortened URL.
  // In this step we check to see if the shortened URL is already present
  //
  def setLookupMapping(url: String, key: String): String = {
    setIfAbsent(url, key) match {
      case Some(oldKey) =>
        // URL present, delete the redundant key
        client.del(key)
        oldKey
      case None =>
        key
    }
  }

  def get(key: String): Future[Option[String]] = {
    Future {
      client.get[String](key)
    }
  }

}

class DuplicateKeyError extends RuntimeException("Cannot create key")