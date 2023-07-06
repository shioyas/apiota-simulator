import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.random.Random

class Tangle (
  private val lambda: Double,
  private val alpha: Double,
  private val d: Double,
  private val timeLimit: Double
) {
  private var nodes: Array<TransactionNode> = arrayOf(TransactionNode(0, 0, TxType.GENESIS, 0.0))
  private var links: Array<Link> = emptyArray()
  private var lastPrintLogTime: Double = 0.0
  private val maxApNum = 3
  
  // システムパラメータ
  private val limitNodeNum: Int = 1000000
  private val milestoneInterval: Int = 5
  
  // キャッシュ
  private var apTransactionCount: MutableMap<Int, Int> = mutableMapOf()
  
  init {
    for (apId in 1..maxApNum) {
      apTransactionCount[apId] = 0
    }
  }
  
  
  fun generateNodes() {
    var time: Double = this.d
    
    while (this.nodes.size < limitNodeNum && time <= timeLimit) {
      time += exponentialSample(lambda)
      val apId = getRouletteApId()
      this.nodes += TransactionNode(
        nodes.size,
        apId,
        getTransactionType(apId),
        time,
      )
    }
  }
  
  private fun getRouletteApId (): Int {
    return 1 + floor(Math.random() * (maxApNum - 1).toDouble()).toInt()
  }
  
  private fun getTransactionType (apId: Int): TxType {
    val nextTxType = if (apTransactionCount[apId]!! % milestoneInterval == 0) TxType.MILESTONE else TxType.NORMAL
    apTransactionCount[apId] = apTransactionCount[apId]!!.inc()
    return nextTxType
  }
  
  fun generateLinks() {
    for ((count, node) in nodes.withIndex()) {
      val candidates: List<TransactionNode> = nodes
        .filter{ candidate -> node.getTime() - candidate.getTime() >= d  }
      
      val candidateLinks = links
        .filter{ link -> node.getTime() - link.getSourceNode().getTime() >= d }
      
      val tips = weightedMCMC(
        candidates.toTypedArray(),
        candidateLinks.toTypedArray(),
        alpha,
      )
      
      //val tips = hybridSelection(
      //  candidates.toTypedArray(),
      //  candidateLinks.toTypedArray(),
      //  alpha,
      //  nodes[0],
      //)
      
      // ログの出力
      if (node.getTime() >= lastPrintLogTime) {
        println(
          "%.1f%%".format((count.toDouble() + 0.001) * 100.0 / nodes.size.toDouble())
            + " Time: $lastPrintLogTime"
            + " L(t): ${candidates.filter { isTip(candidateLinks.toTypedArray(), it) && it.getTime() <= lastPrintLogTime}.size}"
        )
        lastPrintLogTime += 1.0
      }
      
      if (tips.isEmpty()) continue
      
      this.links += Link(node, tips[0])
      //connect(node, tips[0])
      if (tips.size > 1 && tips[0] != tips[1]) {
        //connect(node, tips[1])
        this.links += Link(node, tips[1])
      }
    }
  }
  
  private fun connect(source: TransactionNode, target: TransactionNode) {
    this.links += Link(source, target)
    // known milestone の更新
    val knownMilestone: MutableMap<Int, TransactionNode> = source.getKnownMilestones()
    target.getKnownMilestones().forEach { (apId, targetKnown) ->
      val sourceKnown: TransactionNode? = knownMilestone[apId]
      if (sourceKnown == null) {
        knownMilestone[apId] = targetKnown
      } else if (sourceKnown.getTime() <= targetKnown.getTime() ) { // target が知っているマイルストーンの方が新しい場合
        knownMilestone[apId] = targetKnown
      }
    }
  }
  
  private fun exponentialSample(lambda: Double): Double {
    return -ln(Random.nextDouble()) / lambda
  }
  
  private fun weightedMCMC (nodes: Array<TransactionNode>, links: Array<Link>, alpha: Double): Array<TransactionNode> {
    if (nodes.isEmpty()) return emptyArray()
    
    val start = nodes[0]
    
    calculateWeights(nodes, links)
    
    return arrayOf(
      weightedRandomWalk(links, start, alpha),
      weightedRandomWalk(links, start, alpha),
    )
  }
  
  private fun hybridSelection (nodes: Array<TransactionNode>, links: Array<Link>, alpha: Double, start: TransactionNode): Array<TransactionNode> {
    if (nodes.isEmpty()) return emptyArray()
    
    calculateWeights(nodes, links)
    
    val candidates: List<TransactionNode> = nodes.filter { node -> isTip(links, node) }
    
    return arrayOf(
      weightedRandomWalk(links, start, alpha),
      choose(candidates)
    )
  }
}

fun choose (arr: List<TransactionNode>): TransactionNode {
  return arr[floor(Math.random() * arr.size).toInt()]
}

fun isTip (links: Array<Link>, node: TransactionNode): Boolean {
  return !links.any{ link -> node == link.getTargetNode() }
}

fun weightedRandomWalk (links: Array<Link>, start: TransactionNode, alpha: Double): TransactionNode {
  var particle = start
  
  while (!isTip(links, particle)) {
    val approvers = getApprovers(links, particle)
    
    val cumWeights: List<Double> = approvers.map{ node -> node.getCumWeight() }
    
    val maxWeight = cumWeights.max()
    val normalizedWeights = cumWeights.map{ w -> w - maxWeight }
    
    val weights = normalizedWeights.map{ w -> exp(alpha * w) }
    
    particle = weightedChoose(approvers, weights)
  }
  
  return particle
}

fun getApprovers(links: Array<Link>, node: TransactionNode): List<TransactionNode> {
  return links
    .filter{ link -> link.getTargetNode() == node }
    .map{ link -> link.getSourceNode() }
}

fun getChildrenLists (nodes: Array<TransactionNode>, links: Array<Link>)
  : MutableMap<TransactionNode, MutableList<TransactionNode>>
{
  val childrenLists: MutableMap<TransactionNode, MutableList<TransactionNode>> = mutableMapOf()
  nodes.forEach{ node -> childrenLists[node] = mutableListOf() }
  
  links.forEach{ link -> childrenLists[link.getSourceNode()]?.add(link.getTargetNode()) }
  
  return childrenLists
}

fun weightedChoose (arr: List<TransactionNode>, weights: List<Double>): TransactionNode {
  val rand = Math.random() * weights.sum()
  var cumSum = weights[0]
  
  for (i in 1..arr.size) {
    if (rand < cumSum) {
      return arr[i-1]
    }
    cumSum += weights[i]
  }
  
  return arr[arr.size - 1]
}


private fun calculateWeights (nodes: Array<TransactionNode>, links: Array<Link>) {
  val sorted = topologicalSort(nodes, links)
  
  // 全てのノードを空のSetで初期化
  val ancestorSets: MutableMap<Int, MutableSet<TransactionNode>> = mutableMapOf()
  nodes.forEach{ node -> ancestorSets[node.getTxId()] = mutableSetOf() }
  
  val childrenLists = getChildrenLists(nodes, links)
  for (node in sorted) {
    // ノードが承認するものに対して、承認関係の引き継ぎと自身の追加
    for (child in childrenLists[node]!!) {
      val ancestor = ancestorSets[child.getTxId()]!!
        .plus(ancestorSets[node.getTxId()]!!)
        .plus(node)
      
      ancestorSets[child.getTxId()]!!.addAll(ancestor)
    }
    
    // ノードの重みを更新
    node.setCumWeight(ancestorSets[node.getTxId()]!!.size.toDouble() + 1.0)
  }
}

fun topologicalSort (nodes: Array<TransactionNode>, links: Array<Link>): Array<TransactionNode> {
  val childrenList = getChildrenLists(nodes, links)
  val unvisited = nodes.toMutableSet()
  var result: Array<TransactionNode> = emptyArray()
  
  fun visit (n: TransactionNode) {
    if (!unvisited.contains(n)) {
      return
    }
    
    childrenList[n]?.forEach { child ->
      visit(child)
    }
    
    unvisited.remove(n)
    result += n
  }
  
  nodes.forEach{ node ->
    visit(node)
  }
  
  result.reverse()
  return result
}
