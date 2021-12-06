package wallet.es.service

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import wallet.dm.UserId
import wallet.es.command.{CalculateBalance, Change, Show}
import wallet.es.event.{ChangedEvent, Event}
import wallet.es.fixtures.WalletFixtures
import wallet.es.repository.{Journal, StateRepository, StateRepositoryImpl}

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class WalletServiceImplTest extends AnyFlatSpec with Matchers with WalletFixtures {

  behavior of "WalletServiceImpl"

  private val add10: Change = Change(userId1, 10)
  private val substr5: Change = Change(userId1, -5)

  it should "run commands in single thread mode" in {
    val repository: StateRepository     =
      new StateRepositoryImpl(new AtomicReference[Map[UserId, AtomicInteger]](Map.empty))
    val journal: Journal[UserId, Event] = Journal.of()

    val walletService = new WalletServiceImpl(repository, journal)

    val commands = Seq(
      add10,
      substr5,
      Show(userId1),
      CalculateBalance(userId1)
    )

    val set = commands.map(walletService.runCommand).toSet

    set should contain theSameElementsAs Set(())
    journal.getHistory(userId1) should contain theSameElementsInOrderAs List(
      ChangedEvent(userId1, -5),
      ChangedEvent(userId1, 10)
    )
  }

  it should "run commands in multi thread mode" in {
    val repository: StateRepository     =
      new StateRepositoryImpl(new AtomicReference[Map[UserId, AtomicInteger]](Map.empty))
    val journal: Journal[UserId, Event] = Journal.of()

    val walletService = new WalletServiceImpl(repository, journal)

    val thread1         = Future {
      (1 to 10).foreach(_ => walletService.runCommand(add10))
    }(global)
    val thread2         = Future {
      (1 to 30).foreach(_ => walletService.runCommand(substr5))
    }(global)

    Await.ready(thread1, 5.seconds)
    Await.ready(thread2, 5.seconds)

    walletService.runCommand(CalculateBalance(userId1))
    journal.getHistory(userId1) should have length 30
  }

}
