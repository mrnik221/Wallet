package wallet.es.service

import wallet.dm.UserId
import wallet.es.event.Event
import wallet.es.repository.journal.Journal
import wallet.es.repository.state.StateRepository

trait WalletServiceBuilderComponent {
  def createWallet() = new WalletServiceImpl(stateRepository, journal)

  def stateRepository: StateRepository
  def journal: Journal[UserId, Event]
}
