package com.wangyuxuan.util;

import org.activiti.engine.*;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.Task;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Auther: wangyuxuan
 * @Date: 2018/12/6 13:58
 * @Description:
 */

@Component("activitiUtils")
public class ActivitiUtils {

    /**
     * 运行时Service
     */
    @Autowired
    private RuntimeService runtimeService;
    /**
     * 任务service
     */
    @Autowired
    private TaskService taskService;
    @Autowired
    protected ProcessEngine processEngine;
    @Autowired
    protected RepositoryService repositoryService;
    @Autowired
    protected HistoryService historyService;
    @Autowired
    protected ManagementService managementService;
    @Autowired
    protected IdentityService identityService;

    /**
     * 日志
     */
    protected Logger logger = LoggerFactory.getLogger(ActivitiUtils.class);

    /**
     * 获取当前部署了的流程
     *
     * @param category 类型
     * @return
     */
    public List<ProcessDefinition> getProcessDefinitions(String category) {
        logger.info("【获取当前部署了的流程】category={}", category);
        List<ProcessDefinition> processDefinitions = new ArrayList<ProcessDefinition>();
        if (StringUtils.isBlank(category)) {
            processDefinitions = repositoryService.createProcessDefinitionQuery().list();
        } else {
            processDefinitions = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionCategory(category).list();
        }
        return processDefinitions;
    }


    /**
     * 获取流程
     *
     * @return
     */
    public List<ProcessDefinition> getProcessDefinition(String id) {
        return repositoryService.createProcessDefinitionQuery().processDefinitionId(id).list();
    }


    /**
     * 部署流程图
     *
     * @param activitiName 工作流地址
     */
    public void processActivit(String activitiName) {
        logger.info("【部署流程图】activitiName={}", activitiName);
        processEngine.getRepositoryService().createDeployment()
                .addClasspathResource("process/" + activitiName).deploy();
    }


    /**
     * 启动工作流
     *
     * @param processDefinitionId 部署了的流程id
     * @param querys              代办组或者代办人 格式 users:******* (条件)
     * @return 启动的工作流id
     */
    public String startActivit(String processDefinitionId, Map<String, Object> querys) {
        logger.info("【启动工作流】processDefinitionId={},users={}", processDefinitionId, querys);
        // 5.获取流程实例
        ProcessInstance pi = runtimeService.startProcessInstanceById(processDefinitionId, querys);
        return pi.getId();
    }


    /**
     * 获取工作流所处的位置
     *
     * @param instanceId 工作流id
     * @return 位置
     */
    public String getActivitiNow(String instanceId) {
        logger.info("【获取工作流所处的位置】instanceId={}", instanceId);
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(instanceId);
        return instance.getActivityId();
    }


    /**
     * 验证工作流是不是已经停止
     *
     * @param instanceId 工作流id
     * @return true:已经停止,false:没有停止
     */
    public Boolean validateActiviti(String instanceId) {
        logger.info("【验证工作流是不是已经停止】instanceId={}", instanceId);
        ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                .processInstanceId(instanceId).singleResult();
        if (pi != null) {
            return false;
        }
        return true;
    }


    /**
     * 验证用户是否处于该工作流的当前任务组 group user assignee 都查
     *
     * @param userId 用户id
     * @return true:处于,false:不处于
     */
    public Boolean validateUserIn(String userId) {
        logger.info("【验证用户是否处于工作流的当前任务组】userId={}", userId);
        List<Task> list = getUserHaveTasks(userId);
        if (list != null && list.size() > 0) {
            return true;
        }
        return false;
    }


    /**
     * 获取用户当前处于的任务集合 group user assignee 都查
     *
     * @param userId 用户
     * @return 任务集合
     */
    public List<Task> getUserHaveTasks(String userId) {
        logger.info("【获取用户当前处于的任务集合】userId={}", userId);
        List<Task> list = taskService.createTaskQuery().taskCandidateGroup(userId).list();
        List<Task> listTwo = taskService.createTaskQuery().taskAssignee(userId).list();
        List<Task> listThree = taskService.createTaskQuery().taskCandidateUser(userId).list();
        //排除重复的
        for (int i = 0; i < listTwo.size(); i++) {
            if (!list.contains(listTwo.get(i))) {
                list.add(listTwo.get(i));
            }
        }
        for (Task task : listThree) {
            if (!list.contains(task)) {
                list.add(task);
            }
        }
        return list;
    }


    /**
     * 获取用户当前处于的任务id集合  group user assignee 都查
     *
     * @param userId 用户
     * @return 任务id集合
     */
    public List<String> getUserHaveTaskIds(String userId) {
        logger.info("【获取用户当前处于的任务id集合】userId={}", userId);
        List<Task> list = getUserHaveTasks(userId);
        List<String> ids = new ArrayList<String>();
        for (Task task : list) {
            ids.add(task.getId());
        }
        return ids;
    }


    /**
     * 获取用户当前处于签收人的任务集合 只查Assignee
     *
     * @param userId 用户
     * @return 任务集合
     */
    public List<Task> getUserHaveTasksAssignee(String userId) {
        logger.info("【获取用户当前处于的任务集合】userId={}", userId);
        List<Task> listTwo = taskService.createTaskQuery().taskAssignee(userId).list();
        return listTwo;
    }


    /**
     * 获取当前用户处于签收人的工作流id集合 只查assignee
     *
     * @param userId 用户
     * @return
     */
    public List<String> getUserHaveActivitiIdsAssignee(String userId) {
        logger.info("【获取当前用户处于的工作流id集合】userId={}", userId);
        List<Task> tasks = getUserHaveTasksAssignee(userId);
        List<String> ids = new ArrayList<String>();
        for (Task task : tasks) {
            ids.add(task.getProcessInstanceId());
        }
        return ids;
    }


    /**
     * 获取用户当前处于签收人的任务id集合 只查Assignee
     *
     * @param userId 用户
     * @return 任务id集合
     */
    public List<String> getUserHaveTasksIdsAssignee(String userId) {
        logger.info("【获取用户当前处于的任务id集合】userId={}", userId);
        List<Task> list = getUserHaveTasksAssignee(userId);
        List<String> ids = new ArrayList<String>();
        for (Task task : list) {
            ids.add(task.getId());
        }
        return ids;
    }


    /**
     * 获取用户当前处于代办人的任务集合 只查Group
     *
     * @param userId 用户
     * @return 任务集合
     */
    public List<Task> getUserHaveTasksGroup(String userId) {
        logger.info("【获取用户当前处于的任务集合】userId={}", userId);
        List<Task> list = taskService.createTaskQuery().taskCandidateGroup(userId).list();
        return list;
    }


    /**
     * 获取用户当前处于代办人的任务id集合 只查Group
     *
     * @param userId 用户
     * @return 任务id集合
     */
    public List<String> getUserHaveTasksIdsGroup(String userId) {
        logger.info("【获取用户当前处于的任务id集合】userId={}", userId);
        List<Task> list = getUserHaveTasksGroup(userId);
        List<String> ids = new ArrayList<String>();
        for (Task task : list) {
            ids.add(task.getId());
        }
        return ids;
    }


    /**
     * 获取当前用户处于代办人的工作流id集合 只查group
     *
     * @param userId 用户
     * @return
     */
    public List<String> getUserHaveActivitiIdsGroup(String userId) {
        logger.info("【获取当前用户处于的工作流id集合】userId={}", userId);
        List<Task> tasks = getUserHaveTasksGroup(userId);
        List<String> ids = new ArrayList<String>();
        for (Task task : tasks) {
            ids.add(task.getProcessInstanceId());
        }
        return ids;
    }


    /**
     * 获取用户当前处于代办人的任务集合 只查user
     *
     * @param userId 用户
     * @return 任务集合
     */
    public List<Task> getUserHaveTasksUser(String userId) {
        logger.info("【获取用户当前处于的任务集合】userId={}", userId);
        List<Task> list = taskService.createTaskQuery().taskCandidateUser(userId).list();
        return list;
    }


    /**
     * 获取当前用户处于代办人的工作流id集合 只查user
     *
     * @param userId 用户
     * @return
     */
    public List<String> getUserHaveActivitiIdsUser(String userId) {
        logger.info("【获取当前用户处于的工作流id集合】userId={}", userId);
        List<Task> tasks = getUserHaveTasksUser(userId);
        List<String> ids = new ArrayList<String>();
        for (Task task : tasks) {
            ids.add(task.getProcessInstanceId());
        }
        return ids;
    }


    /**
     * 获取用户当前处于代办人的任务id集合 只查Group
     *
     * @param userId 用户
     * @return 任务id集合
     */
    public List<String> getUserHaveTasksIdsUser(String userId) {
        logger.info("【获取用户当前处于的任务id集合】userId={}", userId);
        List<Task> list = getUserHaveTasksUser(userId);
        List<String> ids = new ArrayList<String>();
        for (Task task : list) {
            ids.add(task.getId());
        }
        return ids;
    }


    /**
     * 获取当前工作流的当前任务
     *
     * @param instanceId 工作流id
     * @return {@link List<Task>}
     */
    public List<Task> getInstanceTasks(String instanceId) {
        logger.info("【获取工作流的当前任务的当前任务】instanceId={}", instanceId);
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(instanceId).list();
        return tasks;
    }


    /**
     * 获取当前工作流的当前任务id集合
     *
     * @param instanceId 工作流id
     * @return {@link List<String>}
     */
    public List<String> getInstanceTaskIds(String instanceId) {
        logger.info("【获取当前工作流的当前任务id集合】instanceId={}", instanceId);
        List<Task> tasks = getInstanceTasks(instanceId);
        List<String> taskIds = new ArrayList<String>();
        for (Task task : tasks) {
            taskIds.add(task.getId());
        }
        return taskIds;
    }


    /**
     * 获取当前工作流的该用户的当前任务 只查了 group 和 assignee
     *
     * @param instanceId 工作流id
     * @param userId     用户id
     * @return {@link List<Task>}
     */
    public List<Task> getInstanceUserTasks(String instanceId, String userId) {
        logger.info("【获取当前工作流的该用户的当前任务】instanceId={}", instanceId);
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(instanceId)
                .taskCandidateGroup(userId).list();
        List<Task> listTwo = taskService.createTaskQuery().processInstanceId(instanceId)
                .taskAssignee(userId).list();
        //排除重复的
        for (int i = 0; i < listTwo.size(); i++) {
            if (!tasks.contains(listTwo.get(i))) {
                tasks.add(listTwo.get(i));
            }
        }
        return tasks;
    }


    /**
     * 获取当前工作流的该用户的当前任务id集合 只查了 group 和 assignee
     *
     * @param instanceId 工作流id
     * @param userId     用户id
     * @return {@link List<String>}
     */
    public List<String> getInstanceUserTaskIds(String instanceId, String userId) {
        logger.info("【获取工作流的当前任务的当前任务id集合】instanceId={}", instanceId);
        List<Task> tasks = getInstanceUserTasks(instanceId, userId);
        List<String> taskIds = new ArrayList<String>();
        for (Task task : tasks) {
            taskIds.add(task.getId());
        }
        return taskIds;
    }


    /**
     * 获取当前工作流的该用户的当前任务id集合 查了 group 和 assignee user
     *
     * @param instanceId 工作流id
     * @param userId     用户id
     * @param userId     角色或者 继续放入 userId
     * @return {@link List<String>}
     */
    public List<String> getInstanceUserTaskIds(String instanceId, String userId, String role) {
        logger.info("【获取工作流的当前任务的当前任务id集合】instanceId={}", instanceId);
        List<Task> tasks = getInstanceUserTasks(instanceId, userId, role);
        List<String> taskIds = new ArrayList<String>();
        for (Task task : tasks) {
            taskIds.add(task.getId());
        }
        return taskIds;
    }


    /**
     * 获取当前工作流的该用户的当前任务 查了 group 和 assignee user
     *
     * @param instanceId 工作流id
     * @param userId     用户id
     * @param userId     角色或者 继续放入 userId
     * @return {@link List<Task>}
     */
    public List<Task> getInstanceUserTasks(String instanceId, String userId, String role) {
        logger.info("【获取当前工作流的该用户的当前任务】instanceId={}", instanceId);
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(instanceId)
                .taskCandidateGroup(userId).list();
        List<Task> listTwo = taskService.createTaskQuery().processInstanceId(instanceId)
                .taskAssignee(userId).list();
        List<Task> listThree = taskService.createTaskQuery().processInstanceId(instanceId)
                .taskCandidateUser(role).list();
        //排除重复的
        for (Task task : listThree) {
            if (!tasks.contains(task)) {
                tasks.add(task);
            }
        }
        for (Task task : listTwo) {
            if (!tasks.contains(task)) {
                tasks.add(task);
            }
        }
        return tasks;
    }


    /**
     * 获取工作流的当前任务的用户组 group user assignee
     *
     * @param instanceId 工作流id
     * @return Map<String   ,   List   <   String>> key:taskId,value:用户集合
     */
    public Map<String, List<String>> getUserIdsMap(String instanceId) {
        logger.info("【获取工作流的当前任务的用户组】instanceId={}", instanceId);
        List<Task> tasks = getInstanceTasks(instanceId);
        Map<String, List<String>> userIdsAll = new HashMap<String, List<String>>();
        for (Task task : tasks) {
            logger.info("【当前任务id】taskId={}", task.getName());
            List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(task.getId());
            List<String> userIds = new ArrayList<String>();
            String userId = "";
            if (StringUtils.isNotBlank(task.getAssignee())) {
                userIds.add(task.getAssignee());
            }
            for (IdentityLink identityLink : identityLinks) {
                //获取用户封装入list
                userId = identityLink.getGroupId();
                if (StringUtils.isBlank(userId)) {
                    //group中无用户
                    userId = identityLink.getUserId();
                    if (StringUtils.isBlank(userId)) {
                        //group中无用户和userId中无用户
                        continue;
                    }
                }
                if (userIds.contains(userId)) {
                    continue;
                }
                userIds.add(userId);
            }
            //加入返回 key:taskId,value:用户集合
            userIdsAll.put(task.getId(), userIds);
        }
        return userIdsAll;
    }


    /**
     * 获取用户所处的任务id集合 根据groupId查找
     *
     * @param instanceId
     * @param userId
     * @return
     */
    public List<String> getInstanceGroupTaskIds(String instanceId, String userId) {
        List<String> taskIds = new ArrayList<String>();
        List<Task> tasks = getInstanceTasks(instanceId);
        for (Task task : tasks) {
            if (StringUtils.isNotBlank(task.getAssignee())) {
                continue;
            }
            List<String> userIds = getGroupUserIds(task);
            if (userIds.contains(userId)) {
                taskIds.add(task.getId());
            }
        }
        return taskIds;
    }


    /**
     * 获取用户所处的任务id集合 根据userId查找
     *
     * @param instanceId
     * @param userId
     * @return
     */
    public List<String> getInstanceUsersTaskIds(String instanceId, String userId) {
        List<String> taskIds = new ArrayList<String>();
        List<Task> tasks = getInstanceTasks(instanceId);
        for (Task task : tasks) {
            if (StringUtils.isNotBlank(task.getAssignee())) {
                continue;
            }
            List<String> userIds = getUserUserIds(task);
            if (userIds.contains(userId)) {
                taskIds.add(task.getId());
            }
        }
        return taskIds;
    }


    /**
     * 获取用户所处的任务id集合 根据Assignee查找
     *
     * @param instanceId
     * @param userId
     * @return
     */
    public List<String> getInstanceAssigneeTaskIds(String instanceId, String userId) {
        List<String> taskIds = new ArrayList<String>();
        List<Task> tasks = getInstanceTasks(instanceId);
        for (Task task : tasks) {
            String userIds = getAssigneeUserIds(task);
            if (userId.equals(userIds)) {
                taskIds.add(task.getId());
            }
        }
        return taskIds;
    }


    /**
     * 获取工作流的当前任务的所有用户组 groupId userId assignee
     *
     * @param instanceId 工作流id
     * @return List<List   <   String>> 表示当前任务可能不只有一个
     */
    public List<List<String>> getUserIds(String instanceId) {
        logger.info("【获取工作流的当前任务的用户组】instanceId={}", instanceId);
        List<Task> tasks = getInstanceTasks(instanceId);
        List<List<String>> userIdsAll = new ArrayList<List<String>>();
        for (Task task : tasks) {
            logger.info("【当前任务id】taskId={}", task.getName());
            List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(task.getId());
            List<String> userIds = new ArrayList<String>();
            String userId = "";
            String groupId = "";
            if (StringUtils.isNotBlank(task.getAssignee())) {
                userIds.add(task.getAssignee());
                logger.info("【有签收人不用再获取用户】");
                continue;
            }
            for (IdentityLink identityLink : identityLinks) {
                //获取用户封装入list
                groupId = identityLink.getGroupId();
                if (StringUtils.isNotBlank(groupId)) {
                    if (groupId.indexOf(",") > 0) {
                        String[] idStr = groupId.split(",");
                        for (String idOne : idStr) {
                            if (!userIds.contains(idOne)) {
                                userIds.add(idOne);
                            }
                        }
                    }
                }
                if (groupId.length() == 1) {
                    if (!userIds.contains(groupId)) {
                        userIds.add(groupId);
                    }
                }
                userId = identityLink.getUserId();
                if (StringUtils.isNotBlank(userId)) {
                    if (userId.indexOf(",") > 0) {
                        String[] idStr = userId.split(",");
                        for (String idOne : idStr) {
                            if (!userIds.contains(idOne)) {
                                userIds.add(idOne);
                            }
                        }
                    }
                }
                if (userId.length() == 1) {
                    if (!userIds.contains(userId)) {
                        userIds.add(userId);
                    }
                }
            }
            //加入返回List<List<String>>
            userIdsAll.add(userIds);
        }
        return userIdsAll;
    }


    /**
     * 获取工作流的当前任务的所有用户组 groupId userId assignee
     *
     * @param instanceId 工作流id
     * @return
     */
    public List<String> getUserIdsOneList(String instanceId) {
        List<List<String>> lists = getUserIds(instanceId);
        List<String> ids = new ArrayList<String>();
        for (List<String> list : lists) {
            for (String id : list) {
                if (!ids.contains(id)) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }


    /**
     * 完成任务
     *
     * @param taskId 任务id
     */
    public void completeTask(String taskId) {
        logger.info("【完成任务】taskId={}", taskId);
        taskService.complete(taskId);
    }


    /**
     * 完成任务
     *
     * @param taskId 任务id
     * @param userId 用户id
     */
    public void completeTask(String taskId, String userId) {
        logger.info("【完成任务】taskId={},userId={}", taskId, userId);
        taskService.complete(taskId);
    }


    /**
     * 认领任务
     *
     * @param taskId 任务id
     * @param userId 用户id
     */
    public void claimTask(String taskId, String userId) {
        taskService.claim(taskId, userId);
    }


    /**
     * 查询是否结束
     *
     * @param instanceId 流程id
     * @return true:已经结束 false:没有结束
     */
    public Boolean validateEnd(String instanceId) {
        ProcessInstance rpi = processEngine.getRuntimeService()//
                .createProcessInstanceQuery()//创建流程实例查询对象
                .processInstanceId(instanceId).singleResult();
        //说明流程实例结束了
        if (rpi == null) {
            return true;
        }
        return false;
    }


    /**
     * 获取用户id集合
     *
     * @param nameIds
     * @return
     */
    public List<String> analysisUserId(String nameIds) {
        List<String> ids = new ArrayList<String>();
        while (nameIds.lastIndexOf("),") > 0) {
            int startIndex = nameIds.indexOf("(");
            int endIndex = nameIds.indexOf("),");
            String id = nameIds.substring(startIndex + 1, endIndex);
            ids.add(id);
            nameIds = nameIds.substring(endIndex + 2);
        }
        return ids;
    }


    /**
     * 修改变量
     *
     * @param activitiId 工作流id
     * @param variables  条件
     */
    public void setVar(String activitiId, Map<String, Object> variables) {
        logger.info("【修改变量】activitiId={},variables={}", activitiId, variables);
        List<Task> tasks = getInstanceTasks(activitiId);
        for (Task task : tasks) {
            taskService.setVariables(task.getId(), variables);
        }
    }


    /**
     * 获取工作流的当前任务的groupId用户组
     *
     * @param instanceId 工作流id
     * @return List<List   <   String>> 表示当前任务可能不只有一个
     */
    public List<List<String>> getGroupUserIds(String instanceId) {
        logger.info("【获取工作流的当前任务的用户组】instanceId={}", instanceId);
        List<Task> tasks = getInstanceTasks(instanceId);
        List<List<String>> userIdsAll = new ArrayList<List<String>>();
        for (Task task : tasks) {
            logger.info("【当前任务id】taskId={}", task.getName());
            List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(task.getId());
            List<String> userIds = new ArrayList<String>();
            String userId = "";
            if (StringUtils.isNotBlank(task.getAssignee())) {
                logger.info("【有签收人 不用获取用户】");
                continue;
            }
            for (IdentityLink identityLink : identityLinks) {
                //获取用户封装入list
                userId = identityLink.getGroupId();
                if (StringUtils.isBlank(userId)) {
                    //group中无用户
                    continue;
                }
                if (userId.indexOf(",") > 0) {
                    String[] idStr = userId.split(",");
                    for (String idOne : idStr) {
                        if (!userIds.contains(idOne)) {
                            userIds.add(idOne);
                        }
                    }
                }
                if (userId.length() == 1) {
                    if (!userIds.contains(userId)) {
                        userIds.add(userId);
                    }
                }
            }
            //加入返回List<List<String>>
            userIdsAll.add(userIds);
        }
        return userIdsAll;
    }


    /**
     * 获取工作流的当前任务的groupId用户组
     *
     * @param instanceId 工作流id
     * @return
     */
    public List<String> getGroupUserIdsOneList(String instanceId) {
        List<List<String>> lists = getGroupUserIds(instanceId);
        List<String> ids = new ArrayList<String>();
        for (List<String> list : lists) {
            for (String id : list) {
                if (!ids.contains(id)) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }


    /**
     * 获取工作流的当前任务的userId用户组
     *
     * @param instanceId 工作流id
     * @return List<List   <   String>> 表示当前任务可能不只有一个
     */
    public List<List<String>> getUserUserIds(String instanceId) {
        logger.info("【获取工作流的当前任务的用户组】instanceId={}", instanceId);
        List<Task> tasks = getInstanceTasks(instanceId);
        List<List<String>> userIdsAll = new ArrayList<List<String>>();
        for (Task task : tasks) {
            logger.info("【当前任务id】taskId={}", task.getName());
            List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(task.getId());
            List<String> userIds = new ArrayList<String>();
            String userId = "";
            if (StringUtils.isNotBlank(task.getAssignee())) {
                logger.info("【有签收人 不用获取用户】");
                continue;
            }
            for (IdentityLink identityLink : identityLinks) {
                userId = identityLink.getUserId();
                if (StringUtils.isBlank(userId)) {
                    continue;
                }
                if (userId.indexOf(",") > 0) {
                    String[] idStr = userId.split(",");
                    for (String idOne : idStr) {
                        if (!userIds.contains(idOne)) {
                            userIds.add(idOne);
                        }
                    }
                }
                if (userId.length() == 1) {
                    if (!userIds.contains(userId)) {
                        userIds.add(userId);
                    }
                }
            }
            //加入返回List<List<String>>
            userIdsAll.add(userIds);
        }
        return userIdsAll;
    }


    /**
     * 获取工作流的当前任务的userId用户组
     *
     * @param instanceId 工作流id
     * @return
     */
    public List<String> getUserUserIdsOneList(String instanceId) {
        List<List<String>> lists = getUserUserIds(instanceId);
        List<String> ids = new ArrayList<String>();
        for (List<String> list : lists) {
            for (String id : list) {
                if (!ids.contains(id)) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }


    /**
     * 获取工作流的当前任务的groupId用户组
     *
     * @param
     * @return List<List   <   String>> 表示当前任务可能不只有一个
     */
    public List<String> getGroupUserIds(Task task) {
        logger.info("【当前任务id】taskId={}", task.getName());
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(task.getId());
        List<String> userIds = new ArrayList<String>();
        String userId = "";
        if (StringUtils.isNotBlank(task.getAssignee())) {
            logger.info("【有签收人 不用获取用户】");
            return userIds;
        }
        for (IdentityLink identityLink : identityLinks) {
            //获取用户封装入list
            userId = identityLink.getGroupId();
            if (StringUtils.isBlank(userId)) {
                //group中无用户
                continue;
            }
            if (userId.indexOf(",") > 0) {
                String[] idStr = userId.split(",");
                for (String idOne : idStr) {
                    if (!userIds.contains(idOne)) {
                        userIds.add(idOne);
                    }
                }
            }
            if (userId.length() == 1) {
                if (!userIds.contains(userId)) {
                    userIds.add(userId);
                }
            }
        }
        //加入返回List<List<String>>
        return userIds;
    }


    /**
     * 获取工作流的当前任务的userId用户组
     *
     * @param
     * @return List<List   <   String>> 表示当前任务可能不只有一个
     */
    public List<String> getUserUserIds(Task task) {
        logger.info("【当前任务id】taskId={}", task.getName());
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(task.getId());
        List<String> userIds = new ArrayList<String>();
        String userId = "";
        if (StringUtils.isNotBlank(task.getAssignee())) {
            logger.info("【有签收人 不用获取用户】");
            return userIds;
        }
        for (IdentityLink identityLink : identityLinks) {
            userId = identityLink.getUserId();
            if (StringUtils.isBlank(userId)) {
                continue;
            }
            if (userId.indexOf(",") > 0) {
                String[] idStr = userId.split(",");
                for (String idOne : idStr) {
                    if (!userIds.contains(idOne)) {
                        userIds.add(idOne);
                    }
                }
            }
            if (userId.length() == 1) {
                if (!userIds.contains(userId)) {
                    userIds.add(userId);
                }
            }
        }
        return userIds;
    }


    /**
     * 获取工作流的当前任务的assignee用户组
     *
     * @param instanceId 工作流id
     * @return List<List   <   String>> 表示当前任务可能不只有一个
     */
    public List<String> getAssigneeUserIds(String instanceId) {
        logger.info("【获取工作流的当前任务的用户组】instanceId={}", instanceId);
        List<Task> tasks = getInstanceTasks(instanceId);
        List<String> userIds = new ArrayList<String>();
        for (Task task : tasks) {
            logger.info("【当前任务id】taskId={}", task.getName());
            String userId = "";
            if (StringUtils.isNotBlank(task.getAssignee())) {
                userId = task.getAssignee();
                if (StringUtils.isBlank(userId)) {
                    continue;
                }
                if (userIds.contains(userId)) {
                    continue;
                }
                userIds.add(userId);
            }
        }
        return userIds;
    }


    /**
     * 获取工作流的当前任务的assignee用户组
     *
     * @param
     * @return List<List   <   String>> 表示当前任务可能不只有一个
     */
    public String getAssigneeUserIds(Task task) {
        logger.info("【当前任务id】taskId={}", task.getName());
        String userId = "";
        if (StringUtils.isNotBlank(task.getAssignee())) {
            userId = task.getAssignee();
            if (StringUtils.isBlank(userId)) {
                return null;
            }
        }
        return userId;
    }


    /**
     * 设置组用户 放入groupId中
     *
     * @param ids  用户
     * @param task
     */
    public void addGroupIds(List<String> ids, Task task) {
        String idStr = "";
        for (String id : ids) {
            idStr += id + ",";
        }
        taskService.addCandidateGroup(task.getId(), idStr);
    }


    /**
     * 获取历史任务 查询用户处于签收人 assignee
     *
     */
    public List<HistoricTaskInstance> getHistoryAssigneeActivitiIds(String userId) {
        List<HistoricTaskInstance> list = processEngine.getHistoryService() // 历史任务Service
                .createHistoricTaskInstanceQuery() // 创建历史任务实例查询
                .taskAssignee(userId) // 指定办理人
                .finished() // 查询已经完成的任务
                .list();
        return list;
    }


    /**
     * 获取历史任务 查询用户处于签收人 assignee
     *
     */
    public List<String> getHistoryAssigneeActivitiIdsString(String userId) {
        List<HistoricTaskInstance> list = getHistoryAssigneeActivitiIds(userId);
        List<String> ids = new ArrayList<String>();
        for (HistoricTaskInstance historicTaskInstance : list) {
            if (!ids.contains(historicTaskInstance.getProcessInstanceId())) {
                ids.add(historicTaskInstance.getProcessInstanceId());
            }
        }
        return ids;
    }


    /**
     * 获取历史任务 查询用户处于代办组 group
     *
     */
    public List<HistoricTaskInstance> getHistoryGroupActivitiIds(String userId) {
        List<HistoricTaskInstance> list = processEngine.getHistoryService() // 历史任务Service
                .createHistoricTaskInstanceQuery() // 创建历史任务实例查询
                .taskCandidateGroup(userId).finished() // 查询已经完成的任务
                .list();
        return list;
    }


    /**
     * 获取历史任务 查询用户处于代办组 group
     *
     */
    public List<String> getHistoryGroupActivitiIdsString(String userId) {
        List<HistoricTaskInstance> list = getHistoryGroupActivitiIds(userId);
        List<String> ids = new ArrayList<String>();
        for (HistoricTaskInstance historicTaskInstance : list) {
            if (!ids.contains(historicTaskInstance.getProcessInstanceId())) {
                ids.add(historicTaskInstance.getProcessInstanceId());
            }
        }
        return ids;
    }


    /**
     * 获取历史任务 查询用户处于代办组 group
     *
     */
    public List<HistoricTaskInstance> getHistoryUserActivitiIds(String userId) {
        List<HistoricTaskInstance> list = processEngine.getHistoryService() // 历史任务Service
                .createHistoricTaskInstanceQuery() // 创建历史任务实例查询
                .taskCandidateUser(userId).finished() // 查询已经完成的任务
                .list();
        return list;
    }


    /**
     * 获取历史任务 查询用户处于代办组 group
     *
     */
    public List<String> getHistoryUserActivitiIdsString(String userId) {
        List<HistoricTaskInstance> list = getHistoryUserActivitiIds(userId);
        List<String> ids = new ArrayList<String>();
        for (HistoricTaskInstance historicTaskInstance : list) {
            if (!ids.contains(historicTaskInstance.getProcessInstanceId())) {
                ids.add(historicTaskInstance.getProcessInstanceId());
            }
        }
        return ids;
    }


}
