package io.backchat.websocket

import reflect.BeanProperty
import collection.JavaConverters._

sealed trait OperationResult {
  @BeanProperty
  def isSuccess: Boolean = false
  @BeanProperty
  def isCancelled: Boolean = false

  @BeanProperty
  def children: java.util.List[OperationResult] = List[OperationResult]().asJava
}
case object Success extends OperationResult {
  @BeanProperty
  override val isSuccess = true

  @BeanProperty
  override val isCancelled = false
}
case object Cancelled extends OperationResult {
  @BeanProperty
  override val isSuccess = false

  @BeanProperty
  override val isCancelled = true
}
case class ResultList(results: List[OperationResult]) extends OperationResult {

  @BeanProperty
  override def isSuccess = results.forall(_.isSuccess)

  @BeanProperty
  override def isCancelled = results.nonEmpty && results.exists(_.isCancelled)

  @BeanProperty
  override def children = results.asJava
}
