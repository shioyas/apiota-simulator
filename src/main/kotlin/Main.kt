fun main() {
  // シミュレーションパラメータ
  val simNumber = 50
  
  // システムパラメータ
  val lambda = 1.5
  val alpha = 1.5
  val d = 1.0
  val h = 5
  val timeLimit = 400.0
  
  repeat((0..simNumber).count()) {
    val tangle = Tangle(lambda, h, alpha, d, timeLimit)
    tangle.generate()
    tangle.printNodes()
  }
}