class TransactionNode (txID: Int, apID: Int, type: TxType, time: Double) {
  private val txID: Int;
  private val apID: Int;
  private val type: TxType;
  private val time: Double;
  
  init {
    this.txID = txID;
    this.apID = apID;
    this.type = type;
    this.time = time;
  }
  
}

enum class TxType{
  GENESIS,
  NORMAL,
  MILESTONE,
}