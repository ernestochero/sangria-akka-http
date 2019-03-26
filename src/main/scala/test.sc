import com.redis._
import models.Product
import com.redis.serialization._

val r = new RedisClient("172.17.0.3", 6379)

private val Products = List(
  Product("5c798c2137024ab47a2b9617", "Cheesecake", "Tasty"),
  Product("5c798c2137024ab47a2b9618", "Health Potion", "+50 HP")
)

object Implicits {
  implicit val parseProduct: Parse[Product] = Parse[Product](new String(_,"UTF-8").asInstanceOf[Product])
}


val productsSet = Products.map{ p => (p.id,p)}.toMap

/*r.hmset("products", productsSet)*/

r.keys("*")
// r.hmset("hash", Map("field1" -> "1", "field2" -> 2))

val result = r.hmget("products", "5c798c2137024ab47a2b9617")
result.map(_.values)

/*val x = "debasish".getBytes("UTF-8")
x
r.set("key", x)
val s = r.get[Array[Byte]]("key")
s
new String(s.get)*/
