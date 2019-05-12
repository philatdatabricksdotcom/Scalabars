package co.blocke.scalabars

import co.blocke.scalajack.ScalaJack
import co.blocke.scalajack.json4s.Json4sFlavor
import javax.script._

import scala.reflect.runtime.universe.TypeTag
import builtins.collections._
import builtins.misc._
import builtins.stock._
import builtins.comparison._
import org.json4s.native.JsonMethods

object Scalabars {
  def apply(): Scalabars = Scalabars(
    Map(
      "if" -> IfHelper(),
      "each" -> EachHelper(),
      "eachIndex" -> EachIndexHelper(),
      "eachProperty" -> EachPropertyHelper(),
      "with" -> WithHelper(),
      "eq" -> EqHelper(),
      "ne" -> NeHelper(),
      "or" -> OrHelper(),
      "and" -> AndHelper(),
      "first" -> FirstHelper(),
      "last" -> LastHelper(),
      "empty" -> EmptyHelper(),
      "withBefore" -> WithBeforeHelper(),
      "withAfter" -> WithAfterHelper(),
      "withFirst" -> WithFirstHelper(),
      "withLast" -> WithLastHelper(),
      "contains" -> ContainsHelper(),
      "any" -> AnyHelper(),
      "join" -> JoinHelper(),
      "length" -> LengthHelper(),
      "lengthEquals" -> LengthEqualsHelper(),
      "sortEach" -> SortEachHelper(),
      "url" -> UrlHelper(),
      "markdown" -> MarkdownHelper(),
      "default" -> DefaultHelper(),
      "unless" -> UnlessHelper()
    ))
}

case class Scalabars(private val helpers: Map[String, Helper] = Map.empty[String, Helper]) {

  private lazy val parser = HandlebarsParser()
  private[scalabars] lazy val sjJson = ScalaJack(Json4sFlavor())
  private lazy val javascript = {
    val engine = new ScriptEngineManager().getEngineByName("nashorn")
    //    val engine = new ScriptEngineManager().getEngineByName("graal.js")
    val bindings = engine.createBindings()
    bindings.put("Handlebars", Handlebars)
    engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE)
    engine
  }

  private[scalabars] def run(js: String): Any = javascript.eval(js)
  private[scalabars] def runHelper(fnName: String, context: Context, args: List[Object]): StringWrapper = {
    val inv = javascript.asInstanceOf[Invocable]

    // Gymnastics to glom context into "this" context within js function.  The bind() Javascript function accomplishes this.
    // Totally NOT thread-safe, but Javascript isn't mutli-threaded anyway, so....
    val bindFn = "bind_" + fnName
    javascript.eval(bindFn + s" = $fnName.bind(" + JsonMethods.compact(JsonMethods.render(context.value)) + ")")

    inv.invokeFunction(bindFn, args: _*) match {
      case s: String        => s
      case w: StringWrapper => w
      case x                => x.toString
    }
  }

  private lazy val stockOptions: Map[String, Any] = Map(
    "noEscape" -> false,
    "strict" -> false
  )

  def registerHelper(name: String, helper: Helper): Scalabars = this.copy(helpers = helpers + (name -> helper))
  def registerHelper(name: String, helperJS: String): Scalabars = this.copy(helpers = helpers + (name -> JSHelper(name, helperJS)))

  def compile(rawTemplate: String, compleOptions: Map[String, Any] = Map.empty[String, Any]) = {
    val hashArgs = (stockOptions ++ compleOptions).map { case (k, v) => k -> StringArgument(v.toString) }
    SimpleTemplate(parser.compile(rawTemplate), Options(this, _hash = hashArgs))
  }

  def pathCompile(p: String): Path = parser.pathCompile(p)

  private[scalabars] def toJson4s[T](t: T)(implicit tt: TypeTag[T]) = sjJson.render(t)
  private[scalabars] def getHelper(name: String) = helpers.get(name)
  private[scalabars] def parsePath(p: String): Path = parser.pathCompile(p)

}

/*
data: Set to false to disable @data tracking.
compat: Set to true to enable recursive field lookup.
knownHelpers: Hash containing list of helpers that are known to exist (truthy) at template execution time. Passing this allows the compiler to optimize a number of cases. Builtin helpers are automatically included in this list and may be omitted by setting that value to false.
knownHelpersOnly: Set to true to allow further optimzations based on the known helpers list.
noEscape: Set to true to not HTML escape any content.
strict: Run in strict mode. In this mode, templates will throw rather than silently ignore missing fields. This has the side effect of disabling inverse operations such as {{^foo}}{{/foo}} unless fields are explicitly included in the source object.
assumeObjects: Removes object existence checks when traversing paths. This is a subset of strict mode that generates optimized templates when the data inputs are known to be safe.
preventIndent: By default, an indented partial-call causes the output of the whole partial being indented by the same amount. This can lead to unexpected behavior when the partial writes pre-tags. Setting this option to true will disable the auto-indent feature.
ignoreStandalone: Disables standalone tag removal when set to true. When set, blocks and partials that are on their own line will not remove the whitespace on that line.
explicitPartialContext: Disables implicit context for partials. When enabled, partials that are not passed a context value will execute against an empty object.
 */

// jdk.nashorn.api.scripting.ScriptObjectMirror.call(thiz, obj...)