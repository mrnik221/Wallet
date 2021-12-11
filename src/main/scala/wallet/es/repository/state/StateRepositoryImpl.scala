package wallet.es.repository.state

import StateRepository.{BalanceResponse, ChangeSuccess, Response}
import wallet.dm.UserId
import wallet.es.utils.atomic.AtomicReferenceOps.AtomicReferenceOps

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

object StateRepository {
  sealed trait Response
  case class BalanceResponse(maybeBalance: Option[Int]) extends Response
  case object ChangeSuccess                             extends Response
}

trait StateRepository {
  def getBalance(userId: UserId): BalanceResponse
  def change(userId: UserId, amount: Int): Response
}

class StateRepositoryImpl(
  store: AtomicReference[Map[UserId, AtomicInteger]] = new AtomicReference(Map.empty[UserId, AtomicInteger])
) extends StateRepository {
  override def getBalance(userId: UserId): BalanceResponse =
    BalanceResponse(
      store
        .get()
        .get(userId)
        .map(_.get())
    )

  override def change(userId: UserId, amount: Int): Response = {
    val userStore: AtomicInteger = store
      .get()
      .get(userId) match {
      case Some(a) => a
      case None    =>
        val notFound = new AtomicInteger(0)
        store.modify { map =>
          map.get(userId) match {
            case Some(found) => (map, found)
            case None        => (map.updated(userId, notFound), notFound)
          }
        }
    }

    userStore.updateAndGet(_ + amount)
    ChangeSuccess
  }
}
