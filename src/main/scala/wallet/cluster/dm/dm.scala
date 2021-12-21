package wallet.cluster

package object dm {
  final case class ShardId(value: Int)

  final case class NrOfShards(value: Int)
}
