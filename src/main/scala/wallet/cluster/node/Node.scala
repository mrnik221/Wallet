package wallet.cluster.node

import wallet.cluster.dm.ShardId

final case class Node[S, Req, Resp](nodeId: String, service: S, requestHandler: Req => Resp) {
  private var shards: Set[ShardId]                             = Set.empty
  private var shardsPerNodes: Map[ShardId, Node[S, Req, Resp]] = Map.empty

  def addShard(shardId: ShardId): Unit    = shards += shardId
  def removeShard(shardId: ShardId): Unit = shards -= shardId

  def updateShardsMapping(newMapping: Map[ShardId, Node[S, Req, Resp]]): Unit = shardsPerNodes = newMapping

  def sendRequest(node: Node[S, Req, Resp], request: NodeRequest[Req]): Resp = node.handleRequest(request)

  def handleRequest(request: NodeRequest[Req]): Resp =
    if (shards.contains(request.shardId)) {
      requestHandler(request.requestBody)
    } else {
      shardsPerNodes.get(request.shardId) match {
        case Some(node) => requestHandler(request.requestBody)
        case None       => throw new IllegalArgumentException(s"Shard $request.shardId is not mapped to any node")
      }
    }

  override def toString: String = s"Node: $nodeId"
}

//  def walletServiceSharded(
//    cluster: Cluster[WalletService],
//    sharding: Sharding[UserId, WalletService],
//    walletService: WalletService
//  ): WalletService = {
//
//    val thisNode = Node[WalletService](walletService.hashCode().toString, walletService)
//
//    cluster.addNode(thisNode)
//
//    new WalletService {
//      override def change(userId: UserId, amount: Int): ChangeResponse =
//        sharding.nodeForShard(sharding.shardId(userId)) match {
//          case nodeForShard if nodeForShard == thisNode => walletService.change(userId, amount)
//          case otherNode                                => cluster.callChangeOnNode(otherNode, userId, amount)
//        }
//
//      override def show(userId: UserId): ShowResponse =
//        sharding.nodeForShard(sharding.shardId(userId)) match {
//          case nodeForShard if nodeForShard == thisNode => walletService.show(userId)
//          case otherNode                                => cluster.callShowOnNode(otherNode, userId)
//        }
//
//      override def calculateBalance(userId: UserId): StateRepository.BalanceResponse =
//        sharding.nodeForShard(sharding.shardId(userId)) match {
//          case nodeForShard if nodeForShard == thisNode => walletService.calculateBalance(userId)
//          case otherNode                                => cluster.callCalculateBalanceOnNode(otherNode, userId)
//        }
//    }
//  }
