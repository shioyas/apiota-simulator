class TransactionNode (
  private val txId: Int,
  private val apId: Int,
  private val type: TxType,
  private val time: Double
) {
  
  private val cachedApprovedFromTxId: Set<Int>
  private val cachedApprovingToTxId: Set<Int>
  private var cumWeight: Double
  
  init {
    this.cachedApprovedFromTxId = mutableSetOf()
    this.cachedApprovingToTxId  = mutableSetOf()
    this.cumWeight = 0.0
  }
  
  fun getTxId (): Int {
    return this.txId
  }
  
  fun getTime (): Double {
    return this.time
  }
  
  fun getCumWeight (): Double {
    return this.cumWeight
  }
  
  fun setCumWeight (v: Double) {
    this.cumWeight = v
  }
  
  fun println () {
    println("{id: $txId, time: $time}")
  }
}

enum class TxType{
  GENESIS,
  NORMAL,
  MILESTONE,
}