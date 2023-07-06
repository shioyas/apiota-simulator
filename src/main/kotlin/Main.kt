fun main() {
  // シミュレーションパラメータ
  val simNumber = 1
  
  // システムパラメータ
  val lambda = 30.0
  val alpha = 0.001
  val d = 5.0
  val timeLimit = 400.0
  
  repeat((1..simNumber).count()) {
    val tangle = Tangle(lambda, alpha, d, timeLimit)
    tangle.generateNodes()
    tangle.generateLinks()
  }
}