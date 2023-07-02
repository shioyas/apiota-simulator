class Link (
  private var source: TransactionNode,
  private var target: TransactionNode,
) {
  
  fun getSourceNode (): TransactionNode {
    return this.source
  }
  
  fun getTargetNode (): TransactionNode {
    return this.target
  }
  
  fun println () {
    println("{source: ${source.getTxId()}, target: ${target.getTxId()}}")
  }
}