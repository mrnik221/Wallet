package wallet.simple

import java.util.concurrent.atomic.AtomicInteger

trait Wallet {
  def change(userId: String, amount: Int): Unit
  def get(userId: String): Int
}

object Wallet {
  def apply(amount: Int): Wallet = {
    val balance = new AtomicInteger(amount)
    new Wallet {
      override def change(userId: String, amount: Int): Unit =
        if (balance.get() + amount >= 0) {
          balance.addAndGet(amount)
          ()
        } else ()//throw new IllegalThreadStateException("Not enough money")

      override def get(userId: String): Int = balance.get()
    }
  }
}
