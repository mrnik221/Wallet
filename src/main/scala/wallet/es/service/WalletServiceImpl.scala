package wallet
package es
package service

import WalletService.{ChangeResponse, ShowResponse}
import dm.UserId
import event.{ChangedEvent, Event}
import repository.journal.Journal
import repository.journal.JournalResponse.{Failure, Success}
import repository.state.StateRepository
import repository.state.StateRepository.BalanceResponse
import wallet.cluster.dm.Node
import wallet.cluster.{Cluster, Sharding}

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

  def apply(repository: StateRepository, journal: Journal[UserId, Event]): WalletService =
    new WalletServiceImpl(repository, journal)

  def walletServiceSharded(
    cluster: Cluster[WalletService],
    sharding: Sharding[UserId, WalletService],
    walletService: WalletService
  ): WalletService = {

    val thisNode = Node[WalletService](walletService.hashCode().toString, walletService)

    cluster.addNode(thisNode)

    new WalletService {
      override def change(userId: UserId, amount: Int): ChangeResponse =
        sharding.nodeForShard(sharding.shardId(userId)) match {
          case nodeForShard if nodeForShard == thisNode => walletService.change(userId, amount)
          case otherNode                                => cluster.callChangeOnNode(otherNode, userId, amount)
        }

      override def show(userId: UserId): ShowResponse =
        sharding.nodeForShard(sharding.shardId(userId)) match {
          case nodeForShard if nodeForShard == thisNode => walletService.show(userId)
          case otherNode                                => cluster.callShowOnNode(otherNode, userId)
        }

      override def calculateBalance(userId: UserId): StateRepository.BalanceResponse =
        sharding.nodeForShard(sharding.shardId(userId)) match {
          case nodeForShard if nodeForShard == thisNode => walletService.calculateBalance(userId)
          case otherNode                                => cluster.callCalculateBalanceOnNode(otherNode, userId)
        }
    }
  }
}

private class WalletServiceImpl(repository: StateRepository, journal: Journal[UserId, Event]) extends WalletService {
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
