package wallet.es.utils.atomic

import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec

object AtomicReferenceOps {
  implicit class AtomicReferenceOps[A](val self: AtomicReference[A]) extends AnyVal {
    def update(f: A => A): Unit =
      modify(a => (f(a), ()))

    def modify[B](f: A => (A, B)): B = {
      @tailrec
      def loop(): B = {
        val a       = self.get()
        val (a1, b) = f(a)
        if (self.compareAndSet(a, a1)) b else loop()
      }
      loop()
    }
  }
}
