package wallet.es.repository

import wallet.dm.UserId
import wallet.es.command._
import wallet.es.event._

//trait EventRepository {
//  def add(command: Command): Unit =
//    command match {
//      case Add(userId, amount)      => store += (command.userId -> AddEvent(userId, amount))
//      case Subtract(userId, amount) => store += (command.userId -> SubtractEvent(userId, amount))
//      case Show(userId)             =>
//        store += (command.userId -> ShowEvent(userId))
//        println("Current event store: ")
//        store.foreach(println)
//    }
//
//  var store: Map[UserId, Event]
//}
//
//object EventRepository {
//  def apply(): EventRepository = new EventRepository {
//    override var store: Map[UserId, Event] = Map.empty
//  }
//}

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

class EventRepository {
  var store: Map[UserId, Seq[Event]] = Map.empty

  def add(command: Command): Response =
    command match {
      case Add(userId, amount)      =>
        store += (command.userId -> (AddEvent(userId, amount) +: store.getOrElse(command.userId, Seq.empty)))
        Success
      case Subtract(userId, amount) =>
        store += (command.userId -> (SubtractEvent(userId, amount) +: store.getOrElse(command.userId, Seq.empty)))
        Success
      case Show(userId)             =>
        store += (command.userId -> (ShowEvent(userId) +: store.getOrElse(command.userId, Seq.empty)))
        println("Current event store: ")
        store.foreach(println)
        Success
      case CalculateBalance(userId) =>
        store += (command.userId -> (CalculateBalanceEvent(userId) +: store.getOrElse(command.userId, Seq.empty)))
        val balance = store
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
