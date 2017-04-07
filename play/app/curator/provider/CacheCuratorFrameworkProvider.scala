/*
 * Copyright (C) 2017  Ľuboš Kozmon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package curator.provider

import java.util.concurrent.{Executor, Executors, TimeUnit}

import com.elkozmon.zoonavigator.core.utils.CommonUtils._
import com.elkozmon.zoonavigator.core.zookeeper.acl.Scheme
import com.google.common.cache.{CacheBuilder, RemovalListener, RemovalNotification}
import com.google.common.util.concurrent.ThreadFactoryBuilder
import logging.AppLogger
import org.apache.curator.framework
import org.apache.curator.framework.api.UnhandledErrorListener
import org.apache.curator.framework.state.{ConnectionState, ConnectionStateListener}
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.ExponentialBackoffRetry
import zookeeper.{AuthInfo, ConnectionString}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContextExecutor, Future, Promise}
import scala.util.{Failure, Try}

class CacheCuratorFrameworkProvider(
  curatorCacheMaxAge: CuratorCacheMaxAge,
  curatorConnectTimeout: CuratorConnectTimeout,
  implicit val executionContextExecutor: ExecutionContextExecutor
)
  extends CuratorFrameworkProvider
    with RemovalListener[CuratorKey, Future[CuratorFramework]]
    with AppLogger {

  private val sessionCache =
    CacheBuilder
      .newBuilder()
      .expireAfterAccess(
        curatorCacheMaxAge.duration.toMillis,
        TimeUnit.MILLISECONDS
      )
      .removalListener(CacheCuratorFrameworkProvider.this)
      .build[CuratorKey, Future[CuratorFramework]]()

  // start cache clean up
  Executors
    .newSingleThreadScheduledExecutor(
      new ThreadFactoryBuilder()
        .setNameFormat(getClass.getSimpleName + "-cleanUp-%d")
        .build()
    )
    .scheduleWithFixedDelay(
      new Runnable {
        override def run(): Unit = sessionCache.cleanUp()
      },
      1000,
      1000,
      TimeUnit.MILLISECONDS
    )
    .asUnit()

  private val sessionCacheMap =
    sessionCache
      .asMap()
      .asScala

  override def onRemoval(
    notification: RemovalNotification[CuratorKey, Future[CuratorFramework]]
  ): Unit = {
    val curatorKey = notification.getKey
    val removalCause = notification.getCause

    logger.debug(
      s"Closing connection to ${curatorKey.connectionString.string}. " +
        s"Cause: $removalCause"
    )

    notification.getValue.foreach(_.close())
  }

  override def getCuratorInstance(
    connectionString: ConnectionString,
    authInfoList: List[AuthInfo]
  ): Future[CuratorFramework] =
    sessionCacheMap.synchronized(
      sessionCacheMap.getOrElseUpdate(
        CuratorKey(connectionString, authInfoList),
        newCuratorInstance(connectionString, authInfoList)
      )
    )

  private def newCuratorInstance(
    connectionString: ConnectionString,
    authInfoList: List[AuthInfo]
  ): Future[CuratorFramework] = {
    val promiseCurator = Promise[CuratorFramework]()

    val tryStartCurator = Try {
      // Create Curator instance
      val frameworkAuthInfoList = authInfoList.map {
        authInfo =>
          new framework.AuthInfo(
            Scheme.toZookeeperScheme(authInfo.scheme),
            authInfo.auth
          )
      }

      val curatorFramework = CuratorFrameworkFactory.builder()
        .authorization(frameworkAuthInfoList.asJava)
        .connectString(connectionString.string)
        .retryPolicy(new ExponentialBackoffRetry(100, 3))
        .build()

      // Listen for successful connection
      val connectionListener = new ConnectionStateListener {
        override def stateChanged(
          client: CuratorFramework,
          newState: ConnectionState
        ): Unit = {
          if (newState == ConnectionState.CONNECTED) {
            promiseCurator
              .trySuccess(client)
              .asUnit()

            client
              .getConnectionStateListenable
              .removeListener(this)
          }
        }
      }

      curatorFramework
        .getConnectionStateListenable
        .addListener(
          connectionListener,
          executionContextExecutor: Executor
        )

      // Log unhandled errors
      val unhandledErrorListener = new UnhandledErrorListener {
        override def unhandledError(message: String, e: Throwable): Unit =
          logger.warn(message, e)
      }

      curatorFramework
        .getUnhandledErrorListenable
        .addListener(
          unhandledErrorListener,
          executionContextExecutor: Executor
        )

      // Timeout the connection
      Executors
        .newSingleThreadScheduledExecutor(
          new ThreadFactoryBuilder()
            .setNameFormat(getClass.getSimpleName + "-timeoutWatcher-%d")
            .build()
        )
        .schedule(
          new Runnable {
            override def run(): Unit = {
              logger.debug(
                "Unable to establish connection with ZooKeeper. " +
                  s"(connection string: ${connectionString.string})"
              )

              val throwable = new Exception(
                "Unable to establish connection with ZooKeeper."
              )

              if (promiseCurator.tryFailure(throwable)) {
                // Curator didn't make it to the cache,
                // stop it from retrying indefinitely
                curatorFramework.close()
              }
            }
          },
          curatorConnectTimeout.duration.toMillis,
          TimeUnit.MILLISECONDS
        )
        .asUnit()

      curatorFramework.start()
    }

    tryStartCurator match {
      case Failure(throwable) =>
        logger.error("Failed to start Curator Framework", throwable)

        promiseCurator
          .tryFailure(throwable)
          .asUnit()
      case _ =>
    }

    promiseCurator.future
  }
}
