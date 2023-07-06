class TransactionNode (
  private val txId: Int,
  private val apId: Int,
  private val type: TxType,
  private val time: Double,
) {
  private var cumWeight: Double = 0.0
  private val knownMilestones: MutableMap<Int, TransactionNode> = mutableMapOf()
  
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
  
  fun getKnownMilestones (): MutableMap<Int, TransactionNode> {
    return this.knownMilestones
  }
}

enum class TxType{
  GENESIS,
  NORMAL,
  MILESTONE,
}