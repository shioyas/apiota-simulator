import kotlin.math.exp
import kotlin.math.ln
import kotlin.random.Random

class Tangle (
  private val title: String,
  private val lambda: Double,
  private var alpha: Double,
  private var d: Double,
  private var timeLimit: Double
) {
  private val logInterval: Double = 1.0
  private var lastLogOutputTime: Double = 0.0
  private var nodes: Array<TransactionNode> = arrayOf(TransactionNode(0, 0, TxType.GENESIS, 0.0))
  private var links: Array<Link> = emptyArray()
  
  // システムパラメータ
  private var limitNodeNum: Int = 10000
  
  fun printResult () {
    println(title)
    println("\tnodes len ${nodes.size}")
    println("\tlinks len ${links.size}")
  }
  
  fun printAllLinks () {
    println("== links (size: ${links.size}) ==")
    links.forEach { link -> link.println() }
  }
  
  fun printAllNodes () {
    println("== nodes (size: ${nodes.size}) ==")
    nodes.forEach { node -> node.println() }
  }
  
  fun generateNodes() {
    var time: Double = this.d
    
    while (this.nodes.size < limitNodeNum && time <= timeLimit) {
      time += exponentialSample(lambda)
      this.nodes += TransactionNode(nodes.size, 1, TxType.NORMAL, time)
    }
  }
  
  fun generateLinks() {
    for (node in nodes) {
      val candidates: List<TransactionNode> = nodes
        .filter{ candidate -> node.getTime() - candidate.getTime() >= d  }
      
      val candidateLinks = links
        .filter{ link -> node.getTime() - link.getSourceNode().getTime() >= d }
      
      val tips = weightedMCMC(
        candidates.toTypedArray(),
        candidateLinks.toTypedArray(),
        alpha,
      )
      
      if (lastLogOutputTime == 0.0 || node.getTime() - lastLogOutputTime > logInterval) {
        lastLogOutputTime = node.getTime()
        println("Time: ${node.getTime()}, L(t): ${candidates.filter{ isTip(candidateLinks.toTypedArray(), it)}.size}")
      }
      
      if (tips.isEmpty()) continue
      
      this.links += Link(node, tips[0])
      if (tips.size > 1 && tips[0] != tips[1]) {
        this.links += Link(node, tips[1])
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
  val ancestorSets: MutableMap<Int, MutableSet<TransactionNode>> = mutableMapOf();
  nodes.forEach{ node -> ancestorSets[node.getTxId()] = mutableSetOf() }
  
  val childrenLists = getChildrenLists(nodes, links);
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
  var childrenList = getChildrenLists(nodes, links)
  var unvisited = nodes.toMutableSet()
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
