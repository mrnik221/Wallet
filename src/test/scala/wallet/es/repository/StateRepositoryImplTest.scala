package wallet.es.repository

import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import wallet.dm.UserId
import wallet.es.fixtures.WalletFixtures
import wallet.es.repository.state.StateRepositoryImpl

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class StateRepositoryImplTest extends AnyFlatSpec with Matchers with Inside with WalletFixtures {
  behavior of "EventRepository"

  it should "get user's balance" in {
    val stateRepository = new StateRepositoryImpl

    stateRepository.change(userId1, 10)

    stateRepository
      .getBalance(userId1)
      .maybeBalance
      .map(inside(_) { case balance =>
        balance shouldBe 10
      })
  }

  it should "update user's balance in multi thread mode" in {
    val stateRepository =
      new StateRepositoryImpl(new AtomicReference[Map[UserId, AtomicInteger]](Map(userId1 -> new AtomicInteger(10))))

    val thread1         = Future {
      (1 to 10).foreach(_ => stateRepository.change(userId1, -10))
    }(global)
    val thread2         = Future {
      (1 to 10).foreach(_ => stateRepository.change(userId1, -10))
    }(global)

    Await.ready(thread1, 5.seconds)
    Await.ready(thread2, 5.seconds)

    stateRepository
      .getBalance(userId1)
      .maybeBalance
      .map(inside(_) { case balance =>
        balance shouldBe -190
      })
  }

  it should "update users balance in multi thread mode" in {
    val stateRepository = new StateRepositoryImpl(
      new AtomicReference[Map[UserId, AtomicInteger]](
        Map(
          userId1 -> new AtomicInteger(10),
          userId2 -> new AtomicInteger(100)
        )
      )
    )

    val thread1 = Future {
      (1 to 10).foreach(_ => stateRepository.change(userId1, -10))
    }(global)
    val thread2 = Future {
      (1 to 10).foreach(_ => stateRepository.change(userId2, -10))
    }(global)

    Await.ready(thread1, 5.seconds)
    Await.ready(thread2, 5.seconds)

    for {
      balance1 <- stateRepository.getBalance(userId1).maybeBalance
      balance2 <- stateRepository.getBalance(userId2).maybeBalance
    } yield {
      balance1 shouldBe -90
      balance2 shouldBe 0
    }
  }
}
