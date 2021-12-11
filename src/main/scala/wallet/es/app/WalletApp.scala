package wallet.es.app

import wallet.es.repository.di.{StateRepositoryComponent, UserEventJournalComponent}
import wallet.es.service.WalletServiceBuilderComponent

object WalletApp extends WalletServiceBuilderComponent with StateRepositoryComponent with UserEventJournalComponent {
  def main(args: Array[String]): Unit = {
    val wallet1 = createWallet()
    val wallet2 = createWallet()
  }
}
