package beyond.engine.javascript

import org.mozilla.javascript.Context
import scala.concurrent.ExecutionContext

class BeyondContext(factory: BeyondContextFactory)(implicit val executionContext: ExecutionContext) extends Context(factory)

