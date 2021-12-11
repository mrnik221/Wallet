package wallet
package es
package service

import dm.UserId
import event.{ChangedEvent, Event}
import wallet.es.repository.journal.JournalResponse.{Failure, Success}
import wallet.es.repository.state.StateRepository.BalanceResponse
import wallet.es.repository.journal.Journal
import wallet.es.repository.state.StateRepository
import wallet.es.service.WalletService.{ChangeResponse, ShowResponse, WalletServiceResponse}

trait WalletService {
  def change(userId: UserId, amount: Int): ChangeResponse
  def show(userId: UserId): ShowResponse
  def calculateBalance(userId: UserId): BalanceResponse
}

object WalletService {
  sealed trait WalletServiceResponse

  case class ChangeResponse(msg: String)           extends WalletServiceResponse
  case class ShowResponse(events: List[Event])     extends WalletServiceResponse
  case class BalanceResponse(balance: Option[Int]) extends WalletServiceResponse
}

class WalletServiceImpl(repository: StateRepository, journal: Journal[UserId, Event]) extends WalletService {
  override def change(userId: UserId, amount: Int): ChangeResponse = {
    val currentBalance: Int = repository.getBalance(userId).maybeBalance.getOrElse(0)

    if (currentBalance + amount >= 0) {
      journal.append(userId, ChangedEvent(userId, amount)) match {
        case Success =>
          repository.change(userId, amount) match {
            case StateRepository.ChangeSuccess =>
              ChangeResponse(s"Balance changed from $currentBalance to ${currentBalance + amount}")
          }
        case Failure =>
          ChangeResponse(s"Could not add event ${ChangedEvent(userId, amount)} to journal")
      }
    } else
      ChangeResponse(s"Not enough money to change $amount")
  }

  override def show(userId: UserId): ShowResponse = ShowResponse(journal.getHistory(userId))

  override def calculateBalance(userId: UserId): BalanceResponse = BalanceResponse(
    repository.getBalance(userId).maybeBalance
  )
}
