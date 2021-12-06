package wallet.es.app

import wallet.dm.UserId
import wallet.es.repository.StateRepositoryImpl
import wallet.es.service.WalletServiceImpl

object WalletApp {
  def main(args: Array[String]): Unit = {
    lazy val eventRepository = new StateRepositoryImpl()
//    val walletService        = new WalletServiceImpl(eventRepository)

    val userId = UserId("user-1")

//    walletService.change(userId, 10)
//    walletService.change(userId, -1)
//    walletService.show(userId)
//    println(walletService.get(userId))
  }
}
