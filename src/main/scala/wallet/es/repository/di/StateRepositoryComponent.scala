package wallet.es.repository.di

import wallet.es.repository.state.{StateRepository, StateRepositoryImpl}

trait StateRepositoryComponent {
  lazy val stateRepository: StateRepository = new StateRepositoryImpl()
}
