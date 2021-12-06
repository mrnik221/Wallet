package wallet.simple

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class WalletTest extends AnyFlatSpec with Matchers {
  it should "add 100500 to empty wallet" in {
    val wallet = Wallet(0)

    wallet.change("", 100500)

    wallet.get("") should be(100500)
  }

  it should "correctly add 1000 in concurrent env" in {
    import scala.concurrent.ExecutionContext.global

    val wallet  = Wallet(0)
    val thread1 = Future {
      (1 to 500).foreach(_ => wallet.change("", 1))
    }(global)
    val thread2 = Future {
      (1 to 500).foreach(_ => wallet.change("", 1))
    }(global)
    Await.ready(thread1, 5.seconds)
    Await.ready(thread2, 5.seconds)

    print(s"current balance: ${wallet.get("")}")
    wallet.get("") should be(1000)
  }

  it should "correctly add 1000 and subtract 1000 in concurrent env" in {
    import scala.concurrent.ExecutionContext.global

    val wallet  = Wallet(0)
    val thread1 = Future {
      (1 to 1000).foreach(_ => wallet.change("", 1))
    }(global)
    val thread2 = Future {
      (1 to 1000).foreach(_ => wallet.change("", -1))
    }(global)
    Await.ready(thread1, 5.seconds)
    Await.ready(thread2, 5.seconds)

    print(s"current balance: ${wallet.get("")}")
    wallet.get("") should be(0)
  }

  it should "correctly subtract 1000 in concurrent env" in {
    import scala.concurrent.ExecutionContext.global

    val wallet  = Wallet(500)
    val thread1 = Future {
      (1 to 500).foreach(_ => wallet.change("", -500))
    }(global)
    val thread2 = Future {
      (1 to 500).foreach(_ => wallet.change("", -500))
    }(global)
    Await.ready(thread1, 5.seconds)
    Await.ready(thread2, 5.seconds)

    print(s"current balance: ${wallet.get("")}")
    wallet.get("") should be(0)
  }
}
