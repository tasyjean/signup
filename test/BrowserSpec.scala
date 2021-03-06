
import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

class BrowserSpec extends Specification {

  "Application" should {

    "work from within a browser" in {
      running(TestServer(3333), HTMLUNIT) { browser =>
        browser.goTo("http://localhost:3333/")
        browser.pageSource must contain("SignUp")
        browser.goTo("http://localhost:3333/users/-1")
        browser.pageSource must contain("Fredrik")
      }
    }
  }

}