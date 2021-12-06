package wallet.es.command

import wallet.dm.UserId

sealed trait Command {
  def userId: UserId
}

case class Change(userId: UserId, amount: Int) extends Command
case class Show(userId: UserId)                extends Command
case class CalculateBalance(userId: UserId)    extends Command
