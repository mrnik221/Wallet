package wallet.es.repository

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import wallet.dm.UserId
import wallet.es.event.{ChangedEvent, Event}
import wallet.es.fixtures.WalletFixtures
import wallet.es.repository.journal.Journal
import wallet.es.repository.journal.JournalResponse.Success

import scala.concurrent.ExecutionContext.global
import scala.concurrent.{Await, Future}

class JournalTest extends AnyFlatSpec with Matchers with WalletFixtures {
  behavior of "Journal"

  private val event1 = ChangedEvent(userId1, 1)
  private val event2 = ChangedEvent(userId2, 1)

  it should "correctly add events for user in single thread mode" in {
    val journal = Journal.of[UserId, Event]()

    val events   = Seq(event1, event1, event1)
    val response = events.map(journal.append(userId1, _)).toSet

    response should contain theSameElementsAs Set(Success)
    journal.getHistory(userId1) should contain theSameElementsInOrderAs events
  }

  it should "correctly add events for users in single thread mode" in {
    val journal = Journal.of[UserId, Event]()

    val events1 = Seq(event1, event1, event1)
    val events2 = Seq(event2, event2, event2)

    events1.map(journal.append(userId1, _)).toSet should contain theSameElementsAs Set(Success)
    events2.map(journal.append(userId2, _)).toSet should contain theSameElementsAs Set(Success)

    journal.getHistory(userId1) should contain theSameElementsInOrderAs events1
    journal.getHistory(userId2) should contain theSameElementsInOrderAs events2
  }

  it should "correctly add events for user in multi thread mode" in {
    val journal = Journal.of[UserId, Event]()

    val thread1 = Future {
      (1 to 10).foreach(_ => journal.append(userId1, event1))
    }(global)
    val thread2 = Future {
      (1 to 10).foreach(_ => journal.append(userId1, event1))
    }(global)
    val thread3 = Future {
      (1 to 10).foreach(_ => journal.append(userId1, event1))
    }(global)
    val thread4 = Future {
      (1 to 10).foreach(_ => journal.append(userId1, event1))
    }(global)

    Await.ready(thread1, 5.seconds)
    Await.ready(thread2, 5.seconds)
    Await.ready(thread3, 5.seconds)
    Await.ready(thread4, 5.seconds)

    journal.getHistory(userId1).length should be(40)
  }

  it should "correctly add events for different users in multi thread mode" in {
    val journal = Journal.of[UserId, Event]()

    val thread1 = Future {
      (1 to 10).foreach(_ => journal.append(userId1, event1))
    }(global)
    val thread2 = Future {
      (1 to 10).foreach(_ => journal.append(userId1, event1))
    }(global)
    val thread3 = Future {
      (1 to 10).foreach(_ => journal.append(userId2, event2))
    }(global)
    val thread4 = Future {
      (1 to 10).foreach(_ => journal.append(userId2, event2))
    }(global)

    Await.ready(thread1, 5.seconds)
    Await.ready(thread2, 5.seconds)
    Await.ready(thread3, 5.seconds)
    Await.ready(thread4, 5.seconds)

    journal.getHistory(userId1).length should be(20)
    journal.getHistory(userId2).length should be(20)
  }
}
