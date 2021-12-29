package wallet.fixtures

import wallet.cluster.dm.ShardId
import wallet.cluster.node.Node
import wallet.dm.UserId

import scala.collection.mutable

trait WalletFixtures {
  val userId1: UserId = UserId("user1")
  val userId2: UserId = UserId("user2")

  def shardsPerNodeMapping[S, Request, Response](
    sharding: mutable.Map[ShardId, Node[S, Request, Response]]
  ): Map[Node[S, Request, Response], collection.Set[ShardId]] =
    sharding
      .groupBy(_._2)
      .map(entity => entity._1 -> entity._2.keySet)
}
