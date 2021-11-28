package wallet.es.service

import wallet.dm.UserId
import wallet.es.command.{Add, CalculateBalance, Command, Show, Subtract}
import wallet.es.repository.{EventRepository, Failure, Response}

trait WalletService {
  def change(userId: UserId, amount: Int): Response
  def get(userId: UserId): Int
}

class WalletServiceImpl(eventRepository: EventRepository) extends WalletService {
  def show(userId: UserId): Response = eventRepository.add(Show(userId))

  override def change(userId: UserId, amount: Int): Response =
    if (amount < 0) {
      val currentBalance: Int = eventRepository.add(CalculateBalance(userId)).state.getOrElse(0)

      if (currentBalance + amount > 0) {
        runCommand(Subtract(userId, Math.abs(amount)))
      } else Failure
    } else runCommand(Add(userId, amount))

  private def runCommand(command: Command) = eventRepository.add(command)

  override def get(userId: UserId): Int = eventRepository.add(CalculateBalance(userId)).state.getOrElse(0)
}
