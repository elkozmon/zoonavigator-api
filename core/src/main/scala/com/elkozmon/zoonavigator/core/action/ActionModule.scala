package com.elkozmon.zoonavigator.core.action

import com.elkozmon.zoonavigator.core.action.actions._
import org.apache.curator.framework.CuratorFramework
import shapeless.HNil

trait ActionModule {

  val createZNodeActionHandler: ActionHandler[CreateZNodeAction]

  val deleteZNodeRecursiveActionHandler: ActionHandler[DeleteZNodeRecursiveAction]

  val forceDeleteZNodeRecursiveActionHandler: ActionHandler[ForceDeleteZNodeRecursiveAction]

  val duplicateZNodeRecursiveActionHandler: ActionHandler[DuplicateZNodeRecursiveAction]

  val moveZNodeRecursiveActionHandler: ActionHandler[MoveZNodeRecursiveAction]

  val getZNodeWithChildrenActionHandler: ActionHandler[GetZNodeWithChildrenAction]

  val getZNodeAclActionHandler: ActionHandler[GetZNodeAclAction]

  val getZNodeDataActionHandler: ActionHandler[GetZNodeDataAction]

  val getZNodeMetaActionHandler: ActionHandler[GetZNodeMetaAction]

  val getZNodeChildrenActionHandler: ActionHandler[GetZNodeChildrenAction]

  val updateZNodeAclListActionHandler: ActionHandler[UpdateZNodeAclListAction]

  val updateZNodeAclListRecursiveActionHandler: ActionHandler[UpdateZNodeAclListRecursiveAction]

  val updateZNodeDataActionHandler: ActionHandler[UpdateZNodeDataAction]

  val exportZNodesActionHandler: ActionHandler[ExportZNodesAction]

  val importZNodesActionHandler: ActionHandler[ImportZNodesAction]

  val actionDispatcher = new ActionDispatcher(
    createZNodeActionHandler ::
      deleteZNodeRecursiveActionHandler ::
      forceDeleteZNodeRecursiveActionHandler ::
      duplicateZNodeRecursiveActionHandler ::
      moveZNodeRecursiveActionHandler ::
      getZNodeWithChildrenActionHandler ::
      getZNodeAclActionHandler ::
      getZNodeDataActionHandler ::
      getZNodeMetaActionHandler ::
      getZNodeChildrenActionHandler ::
      updateZNodeAclListActionHandler ::
      updateZNodeAclListRecursiveActionHandler ::
      updateZNodeDataActionHandler ::
      exportZNodesActionHandler ::
      importZNodesActionHandler ::
      HNil
  )
}
