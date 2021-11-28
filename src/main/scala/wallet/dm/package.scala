package wallet

package object dm {
  case class UserId(id: String){
    override def toString: String = s"userId: $id"
  }

  case class Balance(amount: Int)
  object Balance {
    def empty: Balance = Balance(0)
  }

}
