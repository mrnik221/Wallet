package wallet.cluster
import wallet.cluster.dm.ShardId
import wallet.cluster.dm.Node
import wallet.dm.UserId
import wallet.es.service.WalletService

import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.Map

trait Sharding[K, S] {
  def shardId(key: K): ShardId

  def assignShard(node: Node[S], shardId: ShardId): Unit

  def revokeShard(node: Node[S], shardId: ShardId): Unit

  def nodeForShard(shardId: ShardId): Node[S]
}

object ShardCoordinator {
  private val nodeShardMapping: mutable.Map[(Int, Int), Node[WalletService]] = mutable.Map.empty

  def apply(nodes: Node[WalletService]*): Sharding[UserId, WalletService] = {
    val parts     = nodes.size
    val partition = 1000 / parts

    @tailrec
    def getDistribution(nrOfNodes: Int, step: Int, boundaries: Seq[Int] = Seq.empty): Seq[Int] =
      if (nrOfNodes >= 0) {
        getDistribution(nrOfNodes - 1, step, step * nrOfNodes +: boundaries)
      } else boundaries

    val distribution = getDistribution(parts, partition)
    distribution
      .zip(distribution.tail.map(_ - 1))
      .zip(nodes)
      .foreach { nodeWithBoundaries =>
        nodeShardMapping += (nodeWithBoundaries._1 -> nodeWithBoundaries._2)
      }

    new Sharding[UserId, WalletService] {
      override def shardId(key: UserId): ShardId = ShardId(key.hashCode() % 1000)

      override def assignShard(node: Node[WalletService], shardId: ShardId): Unit = node.addShard(shardId)

      override def revokeShard(node: Node[WalletService], shardId: ShardId): Unit = node.removeShard(shardId)

      override def nodeForShard(shardId: ShardId): Node[WalletService] = {
        val shard =
          nodeShardMapping.keySet
            .filter(boundaries => boundaries._1 <= shardId.value && boundaries._2 >= shardId.value)

        nodeShardMapping(shard.head)
      }
    }
  }

//  def reallocate(nodes: Node*): Sharding[UserId] = {
//    val data = nodeShardMapping.values.fold()
//  }
}
