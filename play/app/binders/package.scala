import com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodeAclVersion
import com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodeDataVersion
import com.elkozmon.zoonavigator.core.zookeeper.znode.ZNodePath
import play.api.mvc.QueryStringBindable

/**
  * TODO custom errors (https://github.com/playframework/playframework/issues/8459)
  */
package object binders {

  implicit def zNodePathBinder(
      implicit string: QueryStringBindable[String]
  ): QueryStringBindable[ZNodePath] = new QueryStringBindable[ZNodePath] {
    override def bind(
        key: String,
        params: Map[String, Seq[String]]
    ): Option[Either[String, ZNodePath]] =
      string
        .bind(key, params)
        .map(_.flatMap(ZNodePath.parse(_).toEither.left.map(_.getMessage)))

    override def unbind(key: String, value: ZNodePath): String = value.path
  }

  implicit def zNodeAclVersionBinder(
      implicit long: QueryStringBindable[Long]
  ): QueryStringBindable[ZNodeAclVersion] =
    new QueryStringBindable[ZNodeAclVersion] {
      override def bind(
          key: String,
          params: Map[String, Seq[String]]
      ): Option[Either[String, ZNodeAclVersion]] =
        long.bind(key, params).map(_.map(ZNodeAclVersion))

      override def unbind(key: String, value: ZNodeAclVersion): String =
        value.version.toString
    }

  implicit def zNodeDataVersionBinder(
      implicit long: QueryStringBindable[Long]
  ): QueryStringBindable[ZNodeDataVersion] =
    new QueryStringBindable[ZNodeDataVersion] {
      override def bind(
          key: String,
          params: Map[String, Seq[String]]
      ): Option[Either[String, ZNodeDataVersion]] =
        long.bind(key, params).map(_.map(ZNodeDataVersion))

      override def unbind(key: String, value: ZNodeDataVersion): String =
        value.version.toString
    }
}
