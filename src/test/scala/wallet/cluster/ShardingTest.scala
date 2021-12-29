package wallet.cluster

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import wallet.cluster.dm.ShardId
import wallet.cluster.node.Node
import wallet.fixtures.WalletFixtures

import scala.collection.mutable

class ShardingTest extends AnyFlatSpec with Matchers with WalletFixtures {
  behavior of "Sharding"

  private def idGenerator(key: Int): ShardId = ShardId(key % 1000)

  trait TestReq
  case class EmptyReq()  extends TestReq
  trait TestResp
  case class EmptyResp() extends TestResp

  private def reqHandler(erq: TestReq): TestResp = EmptyResp()

  private val setOfNodes: Set[Node[None.type, TestReq, TestResp]] =
    Set(Node("node1", None, reqHandler), Node("node2", None, reqHandler), Node("node3", None, reqHandler))

  private val sharding: Sharding[Int, None.type, TestReq, TestResp] =
    Sharding.apply(3, idGenerator, setOfNodes)

  it should "equally distribute shards" in {
    for (i <- 0 to 8)
      sharding.assignShard(idGenerator(i))

    val afterSharding: mutable.Map[ShardId, Node[None.type, TestReq, TestResp]] = sharding.getMapping()

    afterSharding should have size 9
    shardsPerNodeMapping(afterSharding).values.foreach(_ should have size 3)
  }

  it should "redistribute shards when node removed" in {
    for (i <- 0 to 8)
      sharding.assignShard(idGenerator(i))

    sharding.updateMapping(setOfNodes.drop(1))
    val mapping = shardsPerNodeMapping(sharding.getMapping())

    mapping should have size 2
    mapping.foreach(entity => entity._2.size <= 5 should be(true))
  }

  it should "redistribute shards when node added" in {
    for (i <- 0 to 8)
      sharding.assignShard(idGenerator(i))

    sharding.updateMapping(setOfNodes + Node("node4", None, reqHandler))

    val mapping = shardsPerNodeMapping(sharding.getMapping())

    mapping should have size 4
    mapping.foreach(entity => entity._2.size <= 5 should be(true))
  }
}
