package controllers

import com.seb.service.UrlService

import javax.inject._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case class UrlFormData(url: String)

class UrlController @Inject()(
  mcc: MessagesControllerComponents,
  urlService: UrlService,
) extends MessagesAbstractController(mcc) {

  implicit val ec = mcc.executionContext

  val urlForm = Form(
    mapping(
      "url" -> text
    )(UrlFormData.apply)(UrlFormData.unapply)
  )

  def index() = Action { implicit request: MessagesRequest[AnyContent] =>
    Ok(views.html.url.form(urlForm))
  }

  def urlGet(key: String) = Action.async { implicit request: MessagesRequest[AnyContent] =>
    urlService.get(key) transform {
      case Success(Some(url)) =>
        Success(Redirect(url))
      case _ =>
        Success(Redirect("/"))
    }
  }

  def urlPost() = Action.async { implicit request: MessagesRequest[AnyContent] =>
    urlForm.bindFromRequest().fold(
      formWithErrors => {
        // binding failure, you retrieve the form containing errors:
        Future { BadRequest(views.html.url.form(formWithErrors)) }
      },
      urlData => {
        if (isValid(urlData.url)) {
          urlService.shorten(urlData.url) transform {
            case Success(url) =>
              Success(Redirect(routes.UrlController.index()).flashing("message" -> url))
            case Failure(ex) =>
              Success(Redirect(routes.UrlController.index()).flashing("message" -> s"Error: ${ex.getMessage}"))
          }
        } else {
          Future {
            Redirect(routes.UrlController.index()).flashing("message" -> "Invalid URL")
          }
        }
      }
    )
  }

  def isValid(url: String) = Try(new URL(url).toURI).isSuccess
}
