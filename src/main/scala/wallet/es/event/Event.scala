package wallet.es.event

import wallet.dm.UserId

sealed trait Event

case class ShowEvent(userId: UserId)                  extends Event
case class CalculateBalanceEvent(userId: UserId)      extends Event
case class ChangedEvent(userId: UserId, amount: Int)               extends Event
