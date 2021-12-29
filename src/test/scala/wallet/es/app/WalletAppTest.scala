package wallet.es.app

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import wallet.es.event.ChangedEvent
import wallet.es.repository.di.{StateRepositoryComponent, UserEventJournalComponent}
import wallet.fixtures.WalletFixtures

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.DurationInt

class WalletAppTest
    extends AnyFlatSpec
       with Matchers
       with StateRepositoryComponent
       with UserEventJournalComponent
       with WalletFixtures {
  behavior of "WalletApp"

  it should "create 2 wallets with same storage and journal" in {
//    val wallet1 = createWallet()
//    val wallet2 = createWallet()

//    wallet1.change(userId1, 10)
//    wallet2.change(userId2, 20)

//    wallet2.show(userId1).events should contain theSameElementsInOrderAs List(ChangedEvent(userId1, 10))
//    wallet1.show(userId2).events should contain theSameElementsInOrderAs List(ChangedEvent(userId2, 20))

//    wallet1.calculateBalance(userId2).maybeBalance shouldBe Some(20)
//    wallet2.calculateBalance(userId1).maybeBalance shouldBe Some(10)
  }

  it should "create 2 wallets with same storage and journal and use it in different threads" in {
//    val wallet1 = createWallet()
//    val wallet2 = createWallet()

//    val thread1 = Future {
//      (1 to 10).foreach(_ => wallet1.change(userId1, 10))
//    }(global)
//    val thread2 = Future {
//      (1 to 10).foreach(_ => wallet2.change(userId2, 10))
//    }(global)

//    Await.ready(thread1, 5.seconds)
//    Await.ready(thread2, 5.seconds)
//
//    wallet2.show(userId1).events should have length 10
//    wallet1.show(userId2).events should have length 10
//
//    wallet1.calculateBalance(userId2).maybeBalance shouldBe Some(100)
//    wallet2.calculateBalance(userId1).maybeBalance shouldBe Some(100)
  }

}
