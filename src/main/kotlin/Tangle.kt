import org.apache.commons.math3.distribution.ExponentialDistribution

class Tangle (lambda: Double, h: Int, alpha: Double, d: Double, timeLimit: Double) {
  private var timeLimit: Double
  private var nodes: Array<TransactionNode>
  private var links: Array<Link>
  
  // システムパラメータ
  private var lambda: Double
  private var h: Int
  private var alpha: Double
  private var d: Double
  private var limitNodeNum: Int = 1000000
  
  init {
    this.lambda = lambda
    this.h = h
    this.alpha = alpha
    this.d = d
    this.timeLimit = timeLimit
    
    this.nodes = arrayOf(TransactionNode(0, 0, TxType.GENESIS, 0.0))
    this.links = emptyArray()
  }
  
  fun generate () {
    // nodesの作成
    generateNodes()

  }
  
  fun printNodes () {
    println(this.nodes.size)
    println(this.nodes.contentToString())
  }
  
  private fun generateNodes () {
    val exponentialDistribution = ExponentialDistribution(lambda)
    var time: Double = d
    
    while (this.nodes.size < limitNodeNum && time <= timeLimit) {
      time += exponentialDistribution.sample()
      this.nodes += TransactionNode(nodes.size, 1, TxType.NORMAL, time)
    }
  }
}