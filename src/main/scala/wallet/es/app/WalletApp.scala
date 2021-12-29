package wallet.es.app

import wallet.cluster.node.{NodeRequest, WalletServiceRequest}
import wallet.cluster.{Cluster, ShardingSingleton, ShardingSingletonImpl}
import wallet.dm
import wallet.es.repository.di.{StateRepositoryComponent, UserEventJournalComponent}
import wallet.es.service.WalletService
import wallet.es.service.WalletService.WalletServiceResponse

object WalletApp extends StateRepositoryComponent with UserEventJournalComponent {
  def main(args: Array[String]): Unit = {

    def walletServiceBuilder(): WalletService = WalletService.apply(stateRepository, journal)

    val shardingCoordinator: ShardingSingleton[dm.UserId, WalletService, WalletServiceRequest, WalletServiceResponse] =
      ShardingSingletonImpl.ofUserIdAndWalletService()

    def requestHandlerBuilder: WalletService => WalletServiceRequest => WalletServiceResponse = (ws: WalletService) =>
      (re: WalletServiceRequest) => WalletServiceRequest.requestHandler(re, ws)

    val cluster: Cluster[WalletService, WalletServiceRequest, WalletServiceResponse] =
      Cluster.apply(3, walletServiceBuilder, requestHandlerBuilder, shardingCoordinator)
  }
}
