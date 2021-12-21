package wallet.es.app

import wallet.cluster.dm.Node
import wallet.cluster.{Cluster, ShardCoordinator, Sharding}
import wallet.dm
import wallet.es.repository.di.{StateRepositoryComponent, UserEventJournalComponent}
import wallet.es.service.WalletService

object WalletApp extends StateRepositoryComponent with UserEventJournalComponent {
  def main(args: Array[String]): Unit = {

    val ws1 = WalletService.apply(stateRepository, journal)

    val node1                                        = Node(ws1.hashCode().toString, ws1)
    val cluster: Cluster[WalletService]              = Cluster.apply(node1)
    val sharding: Sharding[dm.UserId, WalletService] = ShardCoordinator.apply(node1)

    val walletService = WalletService.walletServiceSharded(cluster, sharding, ws1)
  }
}
