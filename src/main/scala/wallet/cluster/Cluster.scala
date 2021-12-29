package wallet.cluster

import wallet.cluster.dm.ShardId
import wallet.cluster.node.Node

sealed trait ServiceRequest

trait Cluster[S, Req, Resp] {
  def addNode(node: Node[S, Req, Resp]): Unit

  def removeNode(node: Node[S, Req, Resp]): Unit

  def runRequest(req: Req): Resp

  def getNodeInfo: Map[ShardId, Node[S, Req, Resp]]
}

object Cluster {
  private def createNodes[S, Req, Resp](
    nrOfNodes: Int,
    serviceBuilder: () => S,
    requestHandlerBuilder: S => Req => Resp,
    res: Set[Node[S, Req, Resp]] = Set.empty[Node[S, Req, Resp]]
  ): Set[Node[S, Req, Resp]] =
    if (nrOfNodes == 0) res
    else {
      val newNode        = serviceBuilder()
      val requestHandler = requestHandlerBuilder(newNode)
      createNodes(
        nrOfNodes - 1,
        serviceBuilder,
        requestHandlerBuilder,
        res + Node(s"node-$nrOfNodes", newNode, requestHandler)
      )
    }

  def apply[K, S, Req, Resp](
    nrOfNodes: Int,
    serviceBuilder: () => S,
    requestHandlerBuilder: S => Req => Resp,
    shardingSingleton: ShardingSingleton[K, S, Req, Resp]
  ): Cluster[S, Req, Resp] = {
    val shardsCount                                            = 1000
    var clusterNodes: Set[Node[S, Req, Resp]]                  = Set.empty
    var shardingCoordinator: Option[Sharding[K, S, Req, Resp]] = None
    var lastNodeUsed: Int                                      = 0

    clusterNodes = clusterNodes.concat(createNodes[S, Req, Resp](nrOfNodes, serviceBuilder, requestHandlerBuilder))

    shardingCoordinator = shardingCoordinator match {
      case Some(value) => Some(value)
      case None        =>
        Some(
          shardingSingleton.create(
            shardsCount,
            (id: K) => ShardId(Math.abs(id.hashCode() % shardsCount)),
            clusterNodes
          )
        )
    }

    new Cluster[S, Req, Resp] {
      override def addNode(node: Node[S, Req, Resp]): Unit = {
        clusterNodes = clusterNodes + node
        shardingCoordinator.map(_.updateMapping(clusterNodes))
      }

      override def removeNode(node: Node[S, Req, Resp]): Unit = {
        clusterNodes = clusterNodes.filterNot(_ == node)
        shardingCoordinator.map(_.updateMapping(clusterNodes))
      }

      override def runRequest(req: Req): Resp = {
        val node = clusterNodes.drop(lastNodeUsed).head
        lastNodeUsed = (lastNodeUsed + 1) % clusterNodes.size
        println(s"Send request to node ${node.nodeId}")
        node.requestHandler(req)
      }

      override def getNodeInfo(): Map[ShardId, Node[S, Req, Resp]] =
        shardingCoordinator
          .map(_.getMapping())
          .map(Map.from)
          .getOrElse(Map.empty)
    }
  }
}
