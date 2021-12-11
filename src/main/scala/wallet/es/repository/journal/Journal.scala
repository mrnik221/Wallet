package wallet.es.repository.journal

import wallet.es.utils.atomic.AtomicReferenceOps.AtomicReferenceOps

import java.util.concurrent.atomic.AtomicReference

trait Journal[K, V] {
  def append(key: K, value: V): JournalResponse
  def getHistory(key: K): List[V]
}

sealed trait JournalResponse

object JournalResponse {
  case object Success extends JournalResponse
  case object Failure extends JournalResponse
}

object Journal {
  def of[K, V](
    eventStore: AtomicReference[Map[K, AtomicReference[List[V]]]] = new AtomicReference(
      Map.empty[K, AtomicReference[List[V]]]
    )
  ): Journal[K, V] =
    new Journal[K, V] {
      override def append(key: K, value: V): JournalResponse = {
        val store = eventStore
          .get()
          .get(key) match {
          case Some(a) => a
          case None    =>
            val notFound = new AtomicReference(List.empty[V])
            eventStore.modify { map =>
              map.get(key) match {
                case Some(found) => (map, found)
                case None        => (map.updated(key, notFound), notFound)
              }
            }
        }

        store.update(events => value :: events)
        JournalResponse.Success
      }

      override def getHistory(key: K): List[V] =
        eventStore.get().getOrElse(key, new AtomicReference[List[V]](List.empty)).get()
    }
}
