package wallet.es.repository

import wallet.dm.UserId

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import scala.annotation.tailrec

trait StateRepository {
  def getBalance(userId: UserId): Option[Int]
  def change(userId: UserId, amount: Int)
}

class StateRepositoryImpl(
  store: AtomicReference[Map[UserId, AtomicInteger]] = new AtomicReference(Map.empty[UserId, AtomicInteger])
) extends StateRepository {
  override def getBalance(userId: UserId): Option[Int] =
    store
      .get()
      .get(userId)
      .map(_.get())

  override def change(userId: UserId, amount: Int): Unit = synchronized{
    store.updateAndGet(s => s.updated(userId, new AtomicInteger(s.getOrElse(userId, new AtomicInteger(0)).addAndGet(amount))))
  }

//  override def change(userId: UserId, amount: Int): Unit = {
//    @tailrec
//    def loop(): Unit = {
//      val currentBalance = store.get().getOrElse(userId, new AtomicInteger(0)).get()
//      val updatedBalance = currentBalance + amount
//      if (store.get().getOrElse(userId, new AtomicInteger(0)).compareAndSet(currentBalance, updatedBalance)) {} else
//        loop()
//    }
//    loop()
//  }
}
