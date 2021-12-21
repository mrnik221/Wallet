package wallet.cluster.dm

final case class Node[A](value: String, service: A) {
  private var shards: Set[ShardId] = Set.empty

  def addShard(shardId: ShardId): Unit    = shards += shardId
  def removeShard(shardId: ShardId): Unit = shards -= shardId
//    def getAllShards
}
