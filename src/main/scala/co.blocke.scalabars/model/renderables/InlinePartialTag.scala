package co.blocke.scalabars
package model
package renderables

import org.json4s._

case class InlinePartialTag(
    nameArg: Argument,
    body:    Block
) extends Renderable {

  /**
   * Render here does something different than normal.  It doesn't actually produce any output.  What it does produce
   * is a modified options; specifically the contents of this inline partial inserted into options.context so this partial
   * can be found by other Renderables in this scope.
   *
   * @param rc
   * @return
   */
  override def render(rc: RenderControl): RenderControl = {
    val name = (nameArg.eval(rc.opts) match {
      case s: StringEvalResult => Some(s.value)
      // $COVERAGE-OFF$No current use but some future inline helper may return non-String context result.  (Currently only helper like this is
      // lookup, which returns a StringEvalResult
      case c: ContextEvalResult =>
        c.value.value match {
          case s: JString => Some(s.values)
          case _          => None
        }
      // $COVERAGE-ON$
      case _ => None
    }).getOrElse(throw new BarsException("Inline partial's argument must evaluate to a string"))

    val t = SBTemplate(body.flatten.toList, rc.opts)
    val opt = rc.opts.copy(context = rc.opts.context.copy(partials = rc.opts.context.partials + (name -> t)))

    rc.copy(opts = opt)
  }

  override def toString: String =
    s"InlinePartialTag(${body.body.size})\n" + body.body
      .map(_.toString)
      .map(s => "    " + s)
      .mkString("\n") + "\n" +
      "--> (end Inline partial)"
}
