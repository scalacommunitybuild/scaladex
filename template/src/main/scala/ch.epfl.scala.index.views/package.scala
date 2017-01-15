package ch.epfl.scala.index
package views

import model._
import misc.Pagination

import com.typesafe.config.ConfigFactory

import akka.http.scaladsl.model.Uri
import Uri.Query

package object html {

  implicit class QueryAppend(uri: Uri) {

    def appendQuery(kv: (String, String)): Uri = uri.withQuery(
      Query((uri.query(java.nio.charset.Charset.forName("UTF-8")) ++ Vector(kv)):_*)
    )

    def appendQuery(k: String, on: Boolean): Uri = {
      if (on) uri.appendQuery(k -> "✓")
      else uri
    }
    def appendQuery(k: String, vs: List[String]): Uri =
      vs.foldLeft(uri){ case (acc, v) =>
        acc.appendQuery(k -> v)
      }
    def appendQuery(k: String, ov: Option[String]): Uri = {
      ov match {
        case Some(v) => appendQuery(k -> v) 
        case None    => uri
      }
    }
  }

  def paginationUri(
    uri: Uri,
    query: String,
    pagination: Pagination,
    sorting: Option[String],
    you: Boolean,
    filterKeywords: Set[String],
    filterTargets: Set[String]): Int => Uri = page => {

    uri
      .appendQuery("sort", sorting)
      .appendQuery("keywords", filterKeywords.toList)
      .appendQuery("targets" , filterTargets.toList)
      .appendQuery("you", you)
      .appendQuery("q" -> query)
      .appendQuery("page" -> page.toString)
  }

  // https://www.reddit.com/r/scala/comments/4n73zz/scala_puzzle_gooooooogle_pagination/d41jor5
  def paginationRender(selected: Int,
                       max: Int,
                       toShow: Int = 10): (Option[Int], List[Int], Option[Int]) = {
    if (selected == max && max == 1) (None, List(1), None)
    else {
      val min = 1
      require(min to max contains selected, "cannot select something outside the range")
      require(min <= max, "min must not be greater than max")
      require(max > 0 && selected > 0 && toShow > 0, "all arguments must be positive")

      val window = (max min toShow) / 2
      val left = selected - window
      val right = selected + window

      val (minToShow, maxToShow) =
        if (max < toShow) (min, max)
        else {
          (left, right) match {
            case (l, r) if l < min => (min, min + toShow - 1)
            case (l, r) if r > max => (max - toShow + 1, max)
            case (l, r) => (l, r - 1 + toShow % 2)
          }
        }

      val prev =
        if (selected == 1) None
        else Some(selected - 1)

      val next =
        if (selected == max) None
        else Some(selected + 1)

      (prev, (minToShow to maxToShow).toList, next)
    }
  }

  val config = ConfigFactory.load().getConfig("org.scala_lang.index.server")
  val production = config.getBoolean("production")

  def unescapeBackground(in: String) = {
    play.twirl.api.HtmlFormat
      .escape(in)
      .toString
      .replaceAllLiterally("url(&#x27;", "url('")
      .replaceAllLiterally("&#x27;)", "')")
  }

  def formatDate(date: String): String = {
    import org.joda.time.format.{DateTimeFormat, ISODateTimeFormat}
    val in = ISODateTimeFormat.dateTime.withOffsetParsed
    val out = DateTimeFormat.forPattern("dd/MM/yyyy")

    out.print(in.parseDateTime(date))
  }

  def renderTargets(project: Project): String = {
    val prefix = "scala_"
    project.targets
      .collect { case t if t.startsWith(prefix) => t.drop(prefix.length) }
      .to[List].sorted(Ordering.String.reverse)
      .mkString(", ")
  }
}
