package co.blocke.scalabars
package model

import org.graalvm.polyglot.Value
import org.json4s.native.JsonMethods

object JSHelper {

  // Simple eval with no parameters
  private[scalabars] def jsEval(context: Context, js: String)(
      implicit
      sb: Scalabars): EvalResult[_] =
    interpretJSreturn(context, sb.js.eval("js", js))

  // More complext running of a function with parameters and 'this' context
  private[scalabars] def jsRunFn(fnName: String, context: Context, args: List[Object])(
      implicit
      sb: Scalabars): EvalResult[_] = {
    val thisContext = JsonMethods.compact(JsonMethods.render(context.value))
    val fn = sb.js.eval("js", s"""($fnName.bind($thisContext))""")
    interpretJSreturn(context, fn.execute(args: _*))
  }

  // This is where EvalResults from Javascript are born!
  def interpretJSreturn(context: Context, retFromJS: Value): EvalResult[_] = {
    implicit val ctx = context // trigger implicit conversion Value->EvalResult
    if (retFromJS.isHostObject)
      retFromJS.asHostObject[EvalResult[_]]
    else {
      val er: EvalResult[_] = retFromJS
      er
    }
  }
}
import JSHelper._

case class JSHelper(name: String, js: String) extends Helper() {

  private var hasCompiledJS = false

  def run()(implicit options: Options, partials: Map[String, Template]): EvalResult[_] = {

    // First time thru we need to eval this helper's function code into the JavaScript engine
    if (!hasCompiledJS) {
      jsEval(options.context, s"var $name = " + js)(options.handlebars)
      hasCompiledJS = true
    }

    // Now we need to transform helper arguments (options.params) from EvalResult into graal Value objects
    val params = options.params.map(p => er2graalvalue(p)(options.handlebars)) :+ options.asJava

    jsRunFn(name, options.context, params.asInstanceOf[List[Object]])(options.handlebars)
  }
}
