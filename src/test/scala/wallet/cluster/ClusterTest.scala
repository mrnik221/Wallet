package wallet.cluster

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import wallet.cluster.node.WalletServiceRequest
import wallet.dm.UserId
import wallet.es.app.WalletApp.{journal, stateRepository}
import wallet.es.service.WalletService
import wallet.es.service.WalletService.{BalanceResponse, WalletServiceResponse}
import wallet.fixtures.WalletFixtures

class ClusterTest extends AnyFlatSpec with Matchers with WalletFixtures {

  behavior of "Cluster"

  it should "apply" in {
    val shardingCoordinator = ShardingSingletonImpl.ofUserIdAndWalletService()

    def requestHandlerBuilder: WalletService => WalletServiceRequest => WalletServiceResponse =
      (ws: WalletService) => (re: WalletServiceRequest) => WalletServiceRequest.requestHandler(re, ws)

    def walletServiceBuilder(): WalletService = WalletService.apply(stateRepository, journal)

    val cluster = Cluster(3, walletServiceBuilder, requestHandlerBuilder, shardingCoordinator)

    println(s"$userId1 shard: ${Math.abs(userId1.hashCode() % 3)}")
    println(s"$userId2 shard: ${userId2.hashCode() % 3}")
    println(s"$userId2 shard: ${UserId("3").hashCode() % 3}")

    val requests: Seq[WalletServiceRequest] = Seq(
      WalletServiceRequest.ChangeRequest(userId1, 10),
      WalletServiceRequest.ChangeRequest(userId2, 10),
      WalletServiceRequest.ChangeRequest(userId1, -5),
      WalletServiceRequest.ChangeRequest(userId2, -5),
      WalletServiceRequest.CalculateBalance(userId2),
      WalletServiceRequest.CalculateBalance(userId1)
    )

    val resp = requests.map(cluster.runRequest)

    resp should have size 6

    cluster.runRequest(WalletServiceRequest.CalculateBalance(userId1)) should be(BalanceResponse(Some(5)))
    cluster.runRequest(WalletServiceRequest.CalculateBalance(userId2)) should be(BalanceResponse(Some(5)))
  }

}
