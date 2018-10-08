object Main extends App {

  val ops: LogOps = new LogOps {}
  private val app = LogApp(ops, args)
  app.run()
}
