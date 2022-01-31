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

  def urlGet() = Action { implicit request: MessagesRequest[AnyContent] =>
    Ok(views.html.url.form(urlForm))
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
              Success(Redirect(routes.UrlController.urlGet()).flashing("message" -> url))
            case Failure(ex) =>
              Success(Redirect(routes.UrlController.urlGet()).flashing("message" -> s"Error: ${ex.getMessage}"))
          }
        } else {
          Future {
            Redirect(routes.UrlController.urlGet()).flashing("message" -> "Invalid URL")
          }
        }
      }
    )
  }

  def isValid(url: String) = Try(new URL(url).toURI).isSuccess
}
