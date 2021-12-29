package wallet.cluster.node

import wallet.cluster.dm.ShardId
import wallet.dm.UserId
import wallet.es.service.WalletService
import wallet.es.service.WalletService.WalletServiceResponse

final case class NodeRequest[A](shardId: ShardId, requestBody: A)

trait WalletServiceRequest

object WalletServiceRequest {
  final case class ChangeRequest(userId: UserId, amount: Int) extends WalletServiceRequest
  final case class ShowRequest(userId: UserId)                extends WalletServiceRequest
  final case class CalculateBalance(userId: UserId)           extends WalletServiceRequest

  def requestHandler(request: WalletServiceRequest, walletService: WalletService): WalletServiceResponse = request match {
    case ChangeRequest(userId, amount) =>
      walletService.change(userId, amount)
    case ShowRequest(userId)           =>
      walletService.show(userId)
    case CalculateBalance(userId)      =>
      walletService.calculateBalance(userId)
  }
}
