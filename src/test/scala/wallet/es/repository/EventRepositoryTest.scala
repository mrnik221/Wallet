package wallet.es.repository

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import wallet.dm.UserId
import wallet.es.service.WalletServiceImpl

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class EventRepositoryTest extends AnyFlatSpec with Matchers {
  behavior of "EventRepository"

  it should "successfully handle commands in single thread mode" in {
    lazy val eventRepository = new EventRepository()
    val walletService        = new WalletServiceImpl(eventRepository)

    val userId = UserId("user-1")

    walletService.change(userId, 10)
    walletService.change(userId, -1)
    walletService.show(userId)
    walletService.get(userId) shouldBe 9
  }

  it should "successfully handle commands in single multi-thread mode" in {
    import scala.concurrent.ExecutionContext.global

    lazy val eventRepository = new EventRepository()
    val walletService        = new WalletServiceImpl(eventRepository)

    val userId = UserId("user-1")

    val thread1 = Future {
      (1 to 1000).foreach(_ => walletService.change(userId, 1))
    }(global)
    val thread2 = Future {
      (1 to 1000).foreach(_ => walletService.change(userId, 1))
    }(global)

    Await.ready(thread1, 5.seconds)
    Await.ready(thread2, 5.seconds)


    walletService.show(userId)
    walletService.get(userId) shouldBe 2000
  }
}
