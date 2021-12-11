package wallet.es.repository.di

import wallet.dm.UserId
import wallet.es.event.Event
import wallet.es.repository.journal.Journal

trait UserEventJournalComponent {
  lazy val journal: Journal[UserId, Event] = Journal.of()
}
