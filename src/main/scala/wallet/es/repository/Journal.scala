package wallet.es.repository

import java.util.concurrent.atomic.AtomicReference

trait Journal[K, V] {
  def append(key: K, value: V): JournalResponse
  def getHistory(key: K): List[V]
}

sealed trait JournalResponse

case object Success extends JournalResponse
case object Failure extends JournalResponse

object Journal {

  def of[K, V](
    eventStore: AtomicReference[Map[K, AtomicReference[List[V]]]] = new AtomicReference(
      Map.empty[K, AtomicReference[List[V]]]
    )
  ): Journal[K, V] =
    new Journal[K, V] {

      /*      override def append(key: K, value: V): JournalResponse = {
        @tailrec
        def loop(): JournalResponse = {
          val store: AtomicReference[List[V]] = eventStore.get().getOrElse(key, new AtomicReference(List.empty))
          val events: List[V]                 = store.get()
          val updatedEvents: List[V]          = value :: events

          if (store.compareAndSet(events, updatedEvents)) {
            eventStore.updateAndGet(es => es + (key -> store))
            Success
          } else loop()
        }

        loop()
      }*/

      override def append(key: K, value: V): JournalResponse =
        synchronized {
          val store: AtomicReference[List[V]] = eventStore.get().getOrElse(key, new AtomicReference(List.empty))
          store.updateAndGet(e => value :: e)
          eventStore.updateAndGet(es => es + (key -> store))
          Success
        }

      override def getHistory(key: K): List[V] =
        eventStore.get().getOrElse(key, new AtomicReference[List[V]](List.empty)).get()
    }
}
