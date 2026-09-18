package impulse

class GreetingSuite extends munit.FunSuite:

  test("hello greets by name"):
    assertEquals(Greeting.hello("world"), "Hello, world!")
