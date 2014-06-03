package beyond

import java.io.File
import java.net.URLClassLoader
import play.api.Mode
import scalax.file.Path

object BeyondRuntime {
  lazy val classPath: String = {
    import play.api.Play.current
    val currentClassLoaderURLs = current.classloader.asInstanceOf[URLClassLoader].getURLs
    val urls = current.mode match {
      case Mode.Dev =>
        val parentURLs = current.classloader.getParent.asInstanceOf[URLClassLoader].getURLs
        currentClassLoaderURLs ++ parentURLs
      case _ =>
        currentClassLoaderURLs
    }
    urls.mkString(File.pathSeparator)
  }

  val javaPath: String =
    (Path.fromString(System.getProperty("java.home")) / "bin" / "java").path
}