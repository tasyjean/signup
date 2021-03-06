package services

import models._
import play.api.templates.Html
import play.api.Logger
import models.Status._
import com.typesafe.plugin._


object EventNotifier {

  private def findReceivers(event: Event): Seq[User] = {
    val unregisteredMembers = User.findUnregisteredMembers(event)
    val unregisteredGuests = Participation.findGuestsByStatus(Unregistered, event) map {_.user}
    unregisteredMembers union unregisteredGuests
  }

  private def createEmailMessage(event: Event, user: User) : Html = {
    import play.api.Play.current
    val baseUrl = play.api.Play.configuration.getString("email.notification.base.url").getOrElse("")
    views.html.events.email(event, user, baseUrl)
  }


  def notifyParticipants(event: Event) {
    val receivers = findReceivers(event)
    Logger.debug("Found receivers: " + receivers)
    receivers map { receiver =>
      import play.api.Play.current
      val mailer = use[MailerPlugin].email
      mailer.addRecipient(receiver.email)
      mailer.setSubject(event.group.name + ": " + event.name)
      mailer.setReplyTo(event.group.mail_from)
      mailer.addFrom(event.group.mail_from)

      val emailMessage = createEmailMessage(event, receiver)
      try {
        Logger.debug("Sending notification email for " + event.name + " to " + receiver)
        mailer.sendHtml(emailMessage.toString())
        Logger.info("DONE sending notification email for " + event.name + " to " + receiver)
      } catch {
        case ex: Exception => {
          Logger.error("FAILED sending notification email for " + event.name + " to " + receiver, ex)
          LogEntry.create(event, "Misslyckades att skicka påminnelse till " + receiver.email + ". " + ex.getClass.getSimpleName + ": " + ex.getMessage)
        }
      }
    }
    LogEntry.create(event, "Skickat påminnelse till " + receivers.size + " medlem(mar)")
  }
}
