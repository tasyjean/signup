package models

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._
import security.{Permission, NormalUser}

case class User(
                 id: Pk[Long] = NotAssigned,
                 firstName: String,
                 lastName: String,
                 email: String,
                 phone: String = "",
                 comment: String = "",
                 permission: Permission = NormalUser,
                 password: String = "*"
                 )  extends Ordered[User] {
  def compare(that: User) = {
    val c = this.firstName.compare(that.firstName)
    if (c != 0) {
      c
    } else {
      this.lastName.compare(that.lastName)
    }
  }
}

object User {
  val parser = {
    get[Pk[Long]]("id") ~
      get[String]("first_name") ~
      get[String]("last_name") ~
      get[String]("email") ~
      get[String]("phone") ~
      get[String]("comment") ~
      get[String]("permission") ~
      get[String]("pwd") map {
      case id ~ firstName ~ lastName ~ email ~ phone ~ comment ~ permission ~ pwd =>
        User(
          id = id,
          firstName = firstName,
          lastName = lastName,
          email = email.toLowerCase,
          phone = phone,
          comment = comment,
          permission = Permission.withName(permission),
          password = pwd
        )
    }
  }

  def find(id: Long): User = {
    DB.withConnection {
      implicit connection =>
        SQL("select * from users where id={id}").on('id -> id).as(User.parser single)
    }
  }


  def findByEmail(email: String): Option[User] = {
    DB.withConnection {
      implicit connection =>
        SQL("select * from users where lower(email)={email}").on('email -> email.toLowerCase).as(User.parser singleOpt)
    }
  }

  def findUnregisteredMembers(event: Event): Seq[User] = {
    DB.withConnection {
      implicit connection =>
        SQL(findUnregisteredMembersQueryString).on('event_id -> event.id, 'group_id -> event.group.id).as(parser *).sorted
    }
  }

  val findUnregisteredMembersQueryString =
    """
SELECT u.*
FROM users u, memberships m
WHERE m.userx = u.id
  AND m.groupx = {group_id}
  AND u.id NOT IN (SELECT p.userx FROM participations p WHERE p.event = {event_id})
ORDER BY u.first_name, u.last_name
    """

  def findNonMembers(groupId: Long): Seq[User] = {
    DB.withConnection {
      implicit connection =>
        SQL(findNonMembersQueryString).on('group_id -> groupId).as(parser *)
    }
  }

  val findNonMembersQueryString =
    """
SELECT u.*
FROM users u
WHERE u.id NOT IN (SELECT m.userx FROM memberships m WHERE m.groupx = {group_id})
    """

  def findNonGuests(eventId: Long): Seq[User] = {
    DB.withConnection {
      implicit connection =>
        SQL(findNonGuestsQueryString).on('eventId -> eventId).as(parser *)
    }
  }

  val findNonGuestsQueryString =
    """
SELECT u.*
FROM users u
WHERE u.id NOT IN ((SELECT m.userx FROM memberships m, events e WHERE m.groupx = e.groupx AND e.id = {eventId})
                   UNION (SELECT p.userx FROM participations p WHERE p.event = {eventId}))
    """

  def findAll(): Seq[User] = {
    DB.withConnection {
      implicit connection =>
        SQL("select * from users u ORDER BY u.first_name, u.last_name").as(User.parser *)
    }
  }

  def create(user: User): Long = {
    DB.withConnection {
      implicit connection =>
        SQL(insertQueryString).on(
          'firstName -> user.firstName,
          'lastName -> user.lastName,
          'email -> user.email.toLowerCase,
          'phone -> user.phone,
          'comment -> user.comment
        ).executeInsert()
    } match {
      case Some(primaryKey: Long) => primaryKey
      case _ => throw new RuntimeException("Could not insert into database, no PK returned")
    }
  }

  val insertQueryString =
    """
INSERT INTO users (
      first_name,
      last_name,
      email,
      phone,
      comment
    )
    values (
      {firstName},
      {lastName},
      {email},
      {phone},
      {comment}
    )
    """

  def update(id: Long, user: User) {
    DB.withConnection {
      implicit connection =>
        SQL(updateQueryString).on(
          'id -> id,
          'firstName -> user.firstName,
          'lastName -> user.lastName,
          'email -> user.email.toLowerCase,
          'phone -> user.phone,
          'comment -> user.comment
        ).executeUpdate()
    }
  }

  val updateQueryString =
    """
UPDATE users
SET first_name = {firstName},
    last_name = {lastName},
    email = {email},
    phone = {phone},
    comment = {comment}
WHERE id = {id}
    """

  def delete(id: Long) {
    DB.withConnection {
      implicit connection => {
        SQL("DELETE FROM participations p WHERE p.userx={id}").on('id -> id).executeUpdate()
        SQL("DELETE FROM users u WHERE u.id={id}").on('id -> id).executeUpdate()
      }
    }
  }

  def verifyUniqueEmail(userToVerify: User): Boolean = {
    val userInDb = User.findByEmail(userToVerify.email)
    userInDb.isEmpty || userToVerify.id.isDefined && (userInDb.get.id == userToVerify.id)
  }
}

