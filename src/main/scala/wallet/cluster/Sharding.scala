package wallet.cluster
import wallet.cluster.dm.ShardId
import wallet.cluster.node.{Node, WalletServiceRequest}
import wallet.dm.UserId
import wallet.es.service.WalletService
import wallet.es.service.WalletService.WalletServiceResponse

import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.Map

trait Sharding[K, S, Req, Resp] {
  def shardId(key: K): ShardId

  def assignShard(shardId: ShardId): Unit

  def revokeShard(shardId: ShardId): Unit

  def updateMapping(nodes: Set[Node[S, Req, Resp]]): Unit

  def getMapping(): Map[ShardId, Node[S, Req, Resp]]
}

private object Sharding {
  def apply[K, S, Req, Resp](
    shardCount: Int,
    shardIdGenerator: K => ShardId,
    nodes: Set[Node[S, Req, Resp]]
  ): Sharding[K, S, Req, Resp] =
    new Sharding[K, S, Req, Resp] {
      private var nrOfShardsPerNode: mutable.Map[Node[S, Req, Resp], Int] = mutable.Map.from(nodes.map(_ -> 0))

      private var shardsNodeMapping: mutable.Map[ShardId, Node[S, Req, Resp]] = {
        @tailrec
        def go(
          nrOfShards: Int,
          nodesList: List[Node[S, Req, Resp]],
          map: Map[ShardId, Node[S, Req, Resp]]
        ): Map[ShardId, Node[S, Req, Resp]] =
          if (nrOfShards == 0) map
          else {
            val currentShard = ShardId(nrOfShards)
            go(nrOfShards - 1, nodesList, map + (currentShard -> nodesList(nrOfShards % nodesList.size)))
          }

        val res = go(shardCount, nodes.toList, Map.empty)

        nrOfShardsPerNode = res.foldLeft(Map.empty[Node[S, Req, Resp], Int]) { (map, el) =>
          if (!map.contains(el._2)) {
            map + (el._2 -> 1)
          } else {
            map + (el._2 -> (map(el._2) + 1))
          }
        }

        res
      }

      override def shardId(key: K): ShardId = shardIdGenerator(key)

      override def assignShard(shardId: ShardId): Unit = {
        val min = nrOfShardsPerNode.toSeq.minBy(_._2)
        nrOfShardsPerNode += min._1   -> (min._2 + 1)
        shardsNodeMapping += (shardId -> min._1)
      }

      override def revokeShard(shardId: ShardId): Unit = {
        val nodeWithShardToRemove: Node[S, Req, Resp] = shardsNodeMapping(shardId)
        nrOfShardsPerNode += nodeWithShardToRemove -> (nrOfShardsPerNode(nodeWithShardToRemove) - 1)
      }

      override def updateMapping(updatedNodes: Set[Node[S, Req, Resp]]): Unit =
        if (updatedNodes.size < nodes.size) {
          val nodeToRemove: mutable.Map[ShardId, Node[S, Req, Resp]] =
            shardsNodeMapping.filter(entity => !updatedNodes.contains(entity._2))

          val keysFromRemovedNode = Set.from(nodeToRemove.keySet)
          keysFromRemovedNode.foreach(key => shardsNodeMapping -= key)

          nrOfShardsPerNode -= nodeToRemove.values.head

          redistributeShards(keysFromRemovedNode)
        } else if (updatedNodes.size > nodes.size) {
          updatedNodes.foreach(node => nrOfShardsPerNode += node -> 0)

          redistributeShards(Set.from(shardsNodeMapping.keySet))
        }

      private def redistributeShards(shards: Set[ShardId]): Unit = shards.foreach(assignShard)

      override def getMapping(): mutable.Map[ShardId, Node[S, Req, Resp]] = shardsNodeMapping
    }
}

trait ShardingSingleton[K, S, Req, Resp] {
  def create(shardCount: Int, shardIdGenerator: K => ShardId, nodes: Set[Node[S, Req, Resp]]): Sharding[K, S, Req, Resp]
}

object ShardingSingletonImpl {
  private var shardingCoordinator
    : Option[Sharding[UserId, WalletService, WalletServiceRequest, WalletServiceResponse]] = None

  def ofUserIdAndWalletService()
    : ShardingSingleton[UserId, WalletService, WalletServiceRequest, WalletServiceResponse] =
    (
      shardCount: Int,
      shardIdGenerator: UserId => ShardId,
      nodes: Set[Node[WalletService, WalletServiceRequest, WalletServiceResponse]]
    ) =>
      shardingCoordinator match {
        case Some(sc) => sc
        case None     =>
          shardingCoordinator = Some(Sharding.apply(shardCount, shardIdGenerator, nodes))
          shardingCoordinator.get
      }
}
