package beyond.plugin

import org.mozilla.javascript.Context

class BeyondContext(factory: BeyondContextFactory, val timer: JavaScriptTimerProvider) extends Context(factory)

