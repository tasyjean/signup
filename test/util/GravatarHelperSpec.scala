package util

import org.specs2.mutable._

class GravatarHelperSpec extends Specification {

  def CORRECT_URL = "https://secure.gravatar.com/avatar/0bc83cb571cd1c50ba6f3e8a78ef1346.jpg?default=mm"
  "Gravatar util" should {

    "handle simple case" in {
      GravatarHelper.gravatarUrl("myemailaddress@example.com") must beEqualTo(CORRECT_URL)
    }

    "handle CamelCase" in {
      GravatarHelper.gravatarUrl("MyEmailAddress@example.com") must beEqualTo(CORRECT_URL)
    }

    "handle white space" in {
      GravatarHelper.gravatarUrl("  MyEmailAddress@example.com ") must beEqualTo(CORRECT_URL)
    }

    "handle empty string" in {
      GravatarHelper.gravatarUrl("").size must beGreaterThan(0)
    }
  }
}
