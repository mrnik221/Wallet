package wallet.es.repository

import wallet.dm.UserId
import wallet.es.command._
import wallet.es.event._

import java.util.concurrent.atomic.AtomicReference

sealed trait Response {
  def state: Option[Int] = None
}

case object Success         extends Response
case object Failure         extends Response
case object BalanceResponse extends Response {
  def apply(balance: Int): Response = new Response {
    override def state: Option[Int] = Some(balance)
  }
}

class EventRepository(store: AtomicReference[Map[UserId, Seq[Event]]] = new AtomicReference(Map.empty[UserId, Seq[Event]])) {

  def add(command: Command): Response =
    command match {
      case Add(userId, amount)      =>
        store
          .updateAndGet(eventStore => {
            eventStore + (command.userId -> (AddEvent(userId, amount) +: eventStore.getOrElse(command.userId, Seq.empty)))
          })
        Success
      case Subtract(userId, amount) =>
        store
          .updateAndGet(eventStore => {
            eventStore + (command.userId -> (SubtractEvent(userId, amount) +: eventStore.getOrElse(command.userId, Seq.empty)))
          })
        Success
      case Show(userId)             =>
        store
          .updateAndGet(eventStore => {
            eventStore + (command.userId -> (ShowEvent(userId) +: eventStore.getOrElse(command.userId, Seq.empty)))
          })
        println("Current event store: ")
        store.get().foreach(println)
        Success
      case CalculateBalance(userId) =>
        store
          .updateAndGet(eventStore => {
            eventStore + (command.userId -> (CalculateBalanceEvent(userId) +: eventStore.getOrElse(command.userId, Seq.empty)))
          })

        val balance = store
          .get()
          .getOrElse(command.userId, Seq.empty)
          .foldLeft(0)((acc, event) =>
            event match {
              case AddEvent(_, amount)      => acc + amount
              case SubtractEvent(_, amount) => acc - amount
              case _                        => acc
            }
          )

        println(s"Current balance for user ${command.userId} is $balance")
        BalanceResponse(balance)
    }

}
