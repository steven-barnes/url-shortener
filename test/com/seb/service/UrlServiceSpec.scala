package com.seb.service

import com.github.sebruck.EmbeddedRedis
import com.redis.RedisClient
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.scalatest.{BeforeAndAfter, TryValues, fixture}
import org.scalatest.Assertions._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration

import java.net.MalformedURLException
import scala.util.Try

class TestKeyGen extends RandomKeyGen {
  var id = 0

  override def generate: String = {
    val key = id
    id += 1
    key.toString
  }
}

class UrlServiceSpec extends AnyFunSuite with TryValues with ScalaFutures with MockitoSugar {

  val ec = scala.concurrent.ExecutionContext.global
  val config = Configuration.from(Map(
    "redis.host" -> "localhost",
    "redis.port" -> 6379
  ))

  test("Shorten URL successfully") {
    val clientMock = mock[RedisClient]
    when(clientMock.getset(eqTo("0"), any())(any(), any())).thenReturn(None)
    when(clientMock.getset(eqTo("http://x.com"), any())(any(), any())).thenReturn(None)
    val factory = new RedisClientFactory {
      override def create(host: String, port: Int): RedisClient = clientMock
    }
    val service = new UrlService(config, ec, new TestKeyGen(), factory)
    val r = service.shorten("http://x.com")
    assert(r.futureValue == "http://localhost:9000/0")
  }

  test("Shorten URL called twice returns first shortened URL") {
    val clientMock = mock[RedisClient]
    when(clientMock.getset(eqTo("0"), any())(any(), any())).thenReturn(None)
    when(clientMock.getset[String](eqTo("http://x.com"), any())(any(), any())).thenReturn(Some("99"))
    val factory = new RedisClientFactory {
      override def create(host: String, port: Int): RedisClient = clientMock
    }
    val service = new UrlService(config, ec, new TestKeyGen(), factory)
    val r = service.shorten("http://x.com")
    assert(r.futureValue == "http://localhost:9000/99")
  }

}

class UrlParsingSpec extends fixture.FunSuite with TryValues with MockitoSugar {

  type FixtureParam = UrlService

  def withFixture(test: OneArgTest) = {
    val ec = scala.concurrent.ExecutionContext.global
    val config = Configuration.from(Map(
      "redis.host" -> "localhost",
      "redis.port" -> 6379
    ))
    val clientFactory = new RedisClientFactory {
      override def create(host: String, port: Int) = mock[RedisClient]
    }
    test(new UrlService(config, ec, new RandomKeyGen(), clientFactory))
  }


  def valid(service: UrlService, url: String): Unit = {
    assert(service.canonical(url).success.value == url)
  }

  def invalid(service: UrlService, url: String): Unit = {
    assert(service.canonical(url).failure.exception.isInstanceOf[MalformedURLException])
  }

  test("valid URLs should parse") { service =>
    valid(service, "http://x.com")
    valid(service, "https://x.com")
    valid(service, "https://x.com:99")
    valid(service, "http://x.com?thing")
    valid(service, "http://x.com?thing=42")
    valid(service, "http://x.com?thing=42&thing2=99")
    valid(service, "http://x.com:123?thing=42&thing2=99")
  }

  test("valid URLs should be converted to canonical form") { service =>
    assert(service.canonical("http://x.com").success.value == "http://x.com")
    assert(service.canonical(" http://x.com ").success.value == "http://x.com")
    assert(service.canonical("http://x.com/").success.value == "http://x.com")
    assert(service.canonical("http://x.com:80").success.value == "http://x.com")
    assert(service.canonical("http://x.com:99/").success.value == "http://x.com:99")
    assert(service.canonical("http://x.com:").success.value == "http://x.com")
    assert(service.canonical("http://x.com:").success.value == "http://x.com")
    assert(service.canonical("http://x.com/:80").success.value == "http://x.com")
    assert(service.canonical("http://x.com/:").success.value == "http://x.com")
  }

  test("invalid URLs should fail") { service =>
    invalid(service, "bad")
    invalid(service, "x://x.com")
    invalid(service, "//x.com")
    invalid(service, "http://x.com:foo")
  }
}
