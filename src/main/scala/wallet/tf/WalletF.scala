package wallet.tf

import cats.Functor
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.all._
import wallet.dm._


trait WalletF[F[_]] {
  def change(userId: String, amount: Int): F[Unit]
  def get(userId: String): F[Int]
}

object WalletF {
  def of[F[_] : Sync]: F[WalletF[F]] = Ref.of(Map.empty[UserId, Balance]).map(apply(_))

  def apply[F[_] : Functor](state: Ref[F, Map[UserId, Balance]]): WalletF[F] = new WalletF[F] {
    override def change(userId: String, amount: Int): F[Unit] = state.modify { prevState: Map[UserId, Balance] =>
      val id               = UserId(userId)
      val balance: Balance = prevState.getOrElse(id, Balance.empty)
      val newBalance       = balance.amount + amount

      if (newBalance >= 0) {
        (prevState + (id -> Balance(newBalance)), ())
      } else {
        (prevState, ())
      }
    }

    override def get(userId: String): F[Int] =
      state.get.map(_.getOrElse(UserId(userId), Balance.empty).amount)
  }
}
