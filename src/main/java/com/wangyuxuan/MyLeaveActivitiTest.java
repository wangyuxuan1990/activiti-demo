package com.wangyuxuan;

import org.activiti.engine.*;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.zip.ZipInputStream;

/**
 * @Auther: wangyuxuan
 * @Date: 2018/12/4 19:30
 * @Description:
 */
public class MyLeaveActivitiTest {

    /**
     * 会默认按照Resources目录下的activiti.cfg.xml创建流程引擎
     */
    ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
    //学生任务ID
    private String taskIdByStudent;
    //班主任任务ID
    private String taskIdByTeacher;

    @Test
    public void test() {
        creatTable();
        deployFlow();
        queryProcdef();
        startFlow();
        queryTaskByStudent();
        startTaskByStudent();
        queryTaskByTeacher();
        startTaskByTeacher();
    }

    /**
     * 通过activiti.cfg.xml获取流程引擎
     */
    @Test
    public void creatTable() {
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
    }

    /**
     * 发布流程
     * 发布流程后，流程文件会保存到数据库中
     */
    @Test
    public void deployFlow() {
        RepositoryService repositoryService = processEngine.getRepositoryService();

        //获取在classpath下的流程文件
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("test.zip");
        ZipInputStream zipInputStream = new ZipInputStream(in, Charset.forName("UTF-8"));
        //使用deploy方法发布流程
        repositoryService.createDeployment()
                .addZipInputStream(zipInputStream)
                .name("Myleave")
                .deploy();
    }

    @Test
    public void queryProcdef() {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        //创建查询对象
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();
        //添加查询条件
        query.processDefinitionKey("leave");//通过key获取
        // .processDefinitionName("leave")//通过name获取
        // .orderByProcessDefinitionId()//根据ID排序
        //执行查询获取流程定义明细
        List<ProcessDefinition> pds = query.list();
        for (ProcessDefinition pd : pds) {
            System.out.println("ID:" + pd.getId() + ",NAME:" + pd.getName() + ",KEY:" + pd.getKey() + ",VERSION:" + pd.getVersion() + ",RESOURCE_NAME:" + pd.getResourceName() + ",DGRM_RESOURCE_NAME:" + pd.getDiagramResourceName());
        }
    }

    /**
     * 启动流程
     */
    @Test
    public void startFlow() {

        RuntimeService runtimeService = processEngine.getRuntimeService();
        /**
         * 启动请假单流程  并获取流程实例
         * 因为该请假单流程可以会启动多个所以每启动一个请假单流程都会在数据库中插入一条新版本的流程数据
         * 通过key启动的流程就是当前key下最新版本的流程
         *
         */
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("leave");
        System.out.println("id:" + processInstance.getId() + ",activitiId:" + processInstance.getActivityId());
    }

    /**
     * 学生查看任务
     */
    @Test
    public void queryTaskByStudent() {
        //获取任务服务对象
        TaskService taskService = processEngine.getTaskService();
        //根据接受人获取该用户的任务
        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee("student")
                .list();
        for (Task task : tasks) {
            taskIdByStudent = task.getId();
            System.out.println("ID:" + task.getId() + ",姓名:" + task.getName() + ",接收人:" + task.getAssignee() + ",开始时间:" + task.getCreateTime());
        }
    }

    /**
     * 学生提出请假申请，完成任务
     */
    @Test
    public void startTaskByStudent() {
        TaskService taskService = processEngine.getTaskService();
        //taskId 就是查询任务中的 ID
        String taskId = taskIdByStudent;
        //完成请假申请任务
        taskService.complete(taskId);
    }

    /**
     * 班主任查看任务
     */
    @Test
    public void queryTaskByTeacher() {
        //获取任务服务对象
        TaskService taskService = processEngine.getTaskService();
        //根据接受人获取该用户的任务
        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee("teacher")
                .list();
        for (Task task : tasks) {
            taskIdByTeacher = task.getId();
            System.out.println("ID:" + task.getId() + ",姓名:" + task.getName() + ",接收人:" + task.getAssignee() + ",开始时间:" + task.getCreateTime());
        }
    }

    /**
     * 班主任审批请假，完成任务
     */
    @Test
    public void startTaskByTeacher() {
        TaskService taskService = processEngine.getTaskService();
        //taskId 就是查询任务中的 ID
        String taskId = taskIdByTeacher;
        //完成请假申请任务
        taskService.complete(taskId);
    }

}
