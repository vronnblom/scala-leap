package impulse.web

import org.scalajs.dom
import impulse.Greeting

@main def main(): Unit =
  val out = dom.document.getElementById("out")
  out.textContent = Greeting.hello("Impulse")
