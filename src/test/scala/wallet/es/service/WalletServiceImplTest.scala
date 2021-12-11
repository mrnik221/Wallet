package wallet.es.service

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import wallet.dm.UserId
import wallet.es.event.{ChangedEvent, Event}
import wallet.es.fixtures.WalletFixtures
import wallet.es.repository.journal.Journal
import wallet.es.repository.state.{StateRepository, StateRepositoryImpl}

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class WalletServiceImplTest extends AnyFlatSpec with Matchers with WalletFixtures {

  behavior of "WalletServiceImpl"

  private val add10   = (userId1, 10)
  private val substr5 = (userId1, -5)

  it should "run commands in single thread mode" in {
    val repository: StateRepository     =
      new StateRepositoryImpl(new AtomicReference[Map[UserId, AtomicInteger]](Map.empty))
    val journal: Journal[UserId, Event] = Journal.of()

    val walletService = new WalletServiceImpl(repository, journal)

    val add10Resp: WalletService.ChangeResponse      = walletService.change(add10._1, add10._2)
    val withdraw5Resp: WalletService.ChangeResponse  = walletService.change(substr5._1, substr5._2)
    val showRes: WalletService.ShowResponse          = walletService.show(userId1)
    val balanceResp: StateRepository.BalanceResponse = walletService.calculateBalance(userId1)

    val events = List(
      ChangedEvent(userId1, -5),
      ChangedEvent(userId1, 10)
    )

    journal.getHistory(userId1) should contain theSameElementsInOrderAs events
    showRes.events should contain theSameElementsInOrderAs events
    balanceResp.maybeBalance shouldBe Some(5)
    add10Resp.msg should be(s"Balance changed from 0 to 10")
    withdraw5Resp.msg should be(s"Balance changed from 10 to 5")
  }

  it should "run commands in multi thread mode" in {
    val repository: StateRepository     =
      new StateRepositoryImpl(new AtomicReference[Map[UserId, AtomicInteger]](Map(userId1 -> new AtomicInteger(50))))
    val journal: Journal[UserId, Event] = Journal.of()

    val walletService = new WalletServiceImpl(repository, journal)

    val thread1 = Future {
      (1 to 10).foreach(_ => walletService.change(add10._1, add10._2))
    }(global)
    val thread2 = Future {
      (1 to 30).foreach(_ => walletService.change(substr5._1, substr5._2))
    }(global)

    Await.ready(thread1, 5.seconds)
    Await.ready(thread2, 5.seconds)

    journal.getHistory(userId1) should have length 40
    walletService.calculateBalance(userId1).maybeBalance shouldBe Some(0)
  }

  it should "run commands in multi thread mode for 2 users" in {
    val repository: StateRepository     =
      new StateRepositoryImpl(new AtomicReference[Map[UserId, AtomicInteger]](
        Map(
          userId1 -> new AtomicInteger(50),
          userId2 -> new AtomicInteger(150),
        )))
    val journal: Journal[UserId, Event] = Journal.of()

    val walletService = new WalletServiceImpl(repository, journal)

    val thread1 = Future {
      (1 to 10).foreach(_ => walletService.change(add10._1, add10._2))
    }(global)
    val thread2 = Future {
      (1 to 30).foreach(_ => walletService.change(substr5._1, substr5._2))
    }(global)

    val thread3 = Future {
      (1 to 10).foreach(_ => walletService.change(userId2, -10))
    }(global)
    val thread4 = Future {
      (1 to 40).foreach(_ => walletService.change(userId2, 5))
    }(global)

    Await.ready(thread1, 5.seconds)
    Await.ready(thread2, 5.seconds)
    Await.ready(thread3, 5.seconds)
    Await.ready(thread4, 5.seconds)

    journal.getHistory(userId1) should have length 40
    journal.getHistory(userId2) should have length 50
    walletService.calculateBalance(userId1).maybeBalance shouldBe Some(0)
    walletService.calculateBalance(userId2).maybeBalance shouldBe Some(250)
  }
}
