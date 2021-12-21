package wallet.cluster

import wallet.cluster.dm.Node
import wallet.dm.UserId
import wallet.es.repository.state.StateRepository.BalanceResponse
import wallet.es.service.WalletService
import wallet.es.service.WalletService.{ChangeResponse, ShowResponse}

sealed trait ServiceRequest

trait Cluster[S] {
  def addNode(node: Node[S]): Unit

  def removeNode(node: Node[S]): Unit

  //Guess here's a good spot to use some abstraction over requests
  def callChangeOnNode(node: Node[S], userId: UserId, amount: Int): ChangeResponse
  def callShowOnNode(node: Node[S], userId: UserId): ShowResponse
  def callCalculateBalanceOnNode(node: Node[S], userId: UserId): BalanceResponse
}

object Cluster {
  private var clusterNodes: List[Node[WalletService]] = List.empty

  def apply(nodes: Node[WalletService]*): Cluster[WalletService] = {
    clusterNodes = clusterNodes.concat(nodes)

    new Cluster[WalletService] {
      override def addNode(node: Node[WalletService]): Unit = clusterNodes = clusterNodes :+ node

      override def removeNode(node: Node[WalletService]): Unit = clusterNodes = clusterNodes.filterNot(_ == node)

      override def callChangeOnNode(node: Node[WalletService], userId: UserId, amount: Int): ChangeResponse =
        clusterNodes.filter(_ == node).head.service.change(userId, amount)

      override def callShowOnNode(node: Node[WalletService], userId: UserId): ShowResponse =
        clusterNodes.filter(_ == node).head.service.show(userId)

      override def callCalculateBalanceOnNode(node: Node[WalletService], userId: UserId): BalanceResponse =
        clusterNodes.filter(_ == node).head.service.calculateBalance(userId)
    }
  }
}
