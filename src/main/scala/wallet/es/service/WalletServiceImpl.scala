package wallet.es.service

import wallet.dm.UserId
import wallet.es.command.{CalculateBalance, Change, Command, Show}
import wallet.es.event.{ChangedEvent, Event}
import wallet.es.repository.{Failure, Journal, StateRepository, Success}

trait WalletService {
  def runCommand(command: Command): Unit
}

class WalletServiceImpl(repository: StateRepository, journal: Journal[UserId, Event]) extends WalletService {
  override def runCommand(command: Command): Unit = command match {
    case Change(userId, amount)   =>
      synchronized {
        val currentBalance: Int = repository.getBalance(userId).getOrElse(0)

        if (currentBalance + amount >= 0) {
          journal.append(userId, ChangedEvent(userId, amount)) match {
            case Success =>
              repository.change(userId, amount)
            case Failure =>
              println(s"Could not add event ${ChangedEvent(userId, amount)} to journal") //change(userId, amount)
          }
        } else
          println(s"Not enough money to change $amount")
      }
    case Show(userId)             => println(journal.getHistory(userId))
    case CalculateBalance(userId) => println(s"User's: $userId current balance is :${repository.getBalance(userId)}")
  }
}
