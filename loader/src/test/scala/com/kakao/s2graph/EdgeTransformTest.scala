package com.kakao.s2graph

import com.kakao.s2graph.core.mysqls.{Etl, Model}
import com.kakao.s2graph.core.rest.RestCaller
import com.kakao.s2graph.core.{Graph, GraphConfig, Management}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

/**
  * Created by hsleep(honeysleep@gmail.com) on 2015. 12. 22..
  */
class EdgeTransformTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  implicit val graphEx = ExecutionContext.Implicits.global

  lazy val config = ConfigFactory.load()
  val graphConfig = new GraphConfig(config)
  Model(config)
  lazy val _s2graph = new Graph(config)
  lazy val _rest = new RestCaller(_s2graph)
  lazy val _edgeTransform = new EdgeTransform(_rest)

  override def beforeAll: Unit = {
    if (Management.findService("test").isEmpty) {
      Management.createService("test", graphConfig.HBASE_ZOOKEEPER_QUORUM, "test", 1, None, "gz")
    }

    if (Management.findLabel("test1").isEmpty) {
      Management.createLabel("test1", "test", "c1", "string", "test", "c1", "string", true, "test", Nil, Nil, "weak", None, None, isAsync = false)
    }
    val l1Id = Management.findLabel("test1").get.id.get
    if (Management.findLabel("test2").isEmpty) {
      Management.createLabel("test2", "test", "c1", "string", "test", "c1", "string", true, "test", Nil, Nil, "weak", None, None, isAsync = false)
    }
    val l2Id = Management.findLabel("test2").get.id.get
    Etl.create(l1Id.toInt, l2Id.toInt)
  }

  override def afterAll: Unit = {
  }

  "EdgeTransform" should "transform edge" in {
    val e1 = Management.toEdge(1, "insert", "0", "1", "test1", "out", "{}")
    val future = _edgeTransform.changeEdge(e1)

    val result = Await.result(future, 10 seconds)
    result should not be empty

    val e2 = result.get
    e2.label.label should equal("test2")

    val rets = Await.result(_s2graph.mutateEdges(Seq(e2), withWait = true), 10 seconds)
    rets should not be empty

    rets.head should equal(true)
  }
}